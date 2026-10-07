package com.skd.wallpaper.activities

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import coil.load
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.skd.wallpaper.R
import com.skd.wallpaper.data.FavoritesStore
import com.skd.wallpaper.databinding.ActivityWallpaperPreviewBinding
import com.skd.wallpaper.model.Wallpaper
import com.skd.wallpaper.utils.DownloadStatus
import com.skd.wallpaper.utils.WallpaperDownloader
import com.skd.wallpaper.utils.WallpaperSetter
import com.skd.wallpaper.utils.formatCount
import com.skd.wallpaper.utils.formatFileSize
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class WallpaperPreviewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWallpaperPreviewBinding
    private lateinit var wallpaper: Wallpaper
    private var downloadJob: Job? = null
    private var controlsVisible = true

    private val storagePermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startDownload()
        else Toast.makeText(this, R.string.msg_permission_needed, Toast.LENGTH_LONG).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityWallpaperPreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        wallpaper = IntentCompat.getSerializableExtra(intent, EXTRA_WALLPAPER, Wallpaper::class.java) ?: run {
            finish()
            return
        }

        applyInsets()
        bindInfo()
        loadImage()
        setupActions()
        animatePanelIn()
    }

    private fun applyInsets() {
        val topPad = binding.topBar.paddingTop
        val panelMargin = (binding.bottomPanel.layoutParams as android.widget.FrameLayout.LayoutParams).bottomMargin
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.topBar.updatePadding(top = topPad + bars.top)
            (binding.bottomPanel.layoutParams as android.widget.FrameLayout.LayoutParams).bottomMargin =
                panelMargin + bars.bottom
            binding.bottomPanel.requestLayout()
            insets
        }
    }

    private fun bindInfo() = with(binding) {
        tvTitle.text = wallpaper.category?.replaceFirstChar { it.uppercase() } ?: getString(R.string.cd_wallpaper)
        tvSource.text = listOfNotNull(wallpaper.author, "via ${wallpaper.source}").joinToString(" · ")
        tvResolution.text = wallpaper.resolution
        tvSize.text = formatFileSize(wallpaper.fileSize)

        tvViews.visibility = if (wallpaper.views > 0) View.VISIBLE else View.GONE
        tvViews.text = formatCount(wallpaper.views)
        tvFavorites.visibility = if (wallpaper.favorites > 0) View.VISIBLE else View.GONE
        tvFavorites.text = formatCount(wallpaper.favorites)

        renderColors()
        renderLike(FavoritesStore.isLiked(this@WallpaperPreviewActivity, wallpaper))
    }

    /** Small palette swatches – Wallhaven returns the dominant colors of each image. */
    private fun renderColors() {
        val colors = wallpaper.colors.mapNotNull { runCatching { Color.parseColor(it) }.getOrNull() }
        binding.colorsRow.visibility = if (colors.isEmpty()) View.GONE else View.VISIBLE
        val size = (22 * resources.displayMetrics.density).toInt()
        val gap = (8 * resources.displayMetrics.density).toInt()
        colors.forEach { color ->
            val dot = View(this).apply {
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(color)
                    setStroke((2 * resources.displayMetrics.density).toInt(), 0x40FFFFFF)
                }
            }
            binding.colorsRow.addView(dot, LinearLayout.LayoutParams(size, size).apply { marginEnd = gap })
        }
    }

    private fun loadImage() {
        // Show the cached grid thumbnail instantly, then swap in the full-resolution image
        binding.ivFull.load(wallpaper.fullUrl) {
            placeholderMemoryCacheKey(wallpaper.thumbUrl)
            crossfade(400)
            listener(
                onSuccess = { _, _ -> binding.progressImage.visibility = View.GONE },
                onError = { _, _ ->
                    binding.progressImage.visibility = View.GONE
                    binding.ivFull.load(wallpaper.thumbUrl)
                }
            )
        }
        binding.ivFull.setOnClickListener { toggleControls() }
    }

    private fun setupActions() = with(binding) {
        btnBack.setOnClickListener { finish() }
        btnLike.setOnClickListener {
            val liked = FavoritesStore.toggle(this@WallpaperPreviewActivity, wallpaper)
            renderLike(liked)
            bounce(it)
            Toast.makeText(
                this@WallpaperPreviewActivity,
                if (liked) R.string.msg_liked else R.string.msg_unliked,
                Toast.LENGTH_SHORT
            ).show()
        }
        btnDownload.setOnClickListener { onDownloadClicked() }
        btnSet.setOnClickListener { chooseWallpaperTarget() }
        btnShare.setOnClickListener { share() }
        btnInfo.setOnClickListener { showInfo() }
    }

    private fun renderLike(liked: Boolean) {
        binding.btnLike.setImageResource(if (liked) R.drawable.ic_heart else R.drawable.ic_heart_outline)
        binding.btnLike.setColorFilter(
            ContextCompat.getColor(this, if (liked) R.color.like_red else R.color.text_primary)
        )
    }

    // region Download
    private fun onDownloadClicked() {
        if (downloadJob?.isActive == true) return
        val needsPermission = Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
            PackageManager.PERMISSION_GRANTED
        if (needsPermission) storagePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        else startDownload()
    }

    private fun startDownload() {
        downloadJob = lifecycleScope.launch {
            binding.downloadProgress.visibility = View.VISIBLE
            binding.downloadProgress.progress = 0
            try {
                WallpaperDownloader.download(applicationContext, wallpaper).collect { status ->
                    when (status) {
                        is DownloadStatus.Progress -> {
                            binding.downloadProgress.progress = status.percent
                            binding.tvDownload.text = "${getString(R.string.msg_download_started)} ${status.percent}%"
                        }
                        DownloadStatus.Success -> {
                            binding.downloadProgress.progress = 100
                            binding.tvDownload.text = "Saved ✓"
                            Toast.makeText(this@WallpaperPreviewActivity, R.string.msg_download_done, Toast.LENGTH_LONG).show()
                        }
                        DownloadStatus.Failed -> showDownloadFailed()
                    }
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                showDownloadFailed()
            }
            binding.downloadProgress.visibility = View.INVISIBLE
        }
    }

    private fun showDownloadFailed() {
        binding.tvDownload.setText(R.string.lbl_download)
        Toast.makeText(this, R.string.msg_download_failed, Toast.LENGTH_LONG).show()
    }
    // endregion

    // region Set wallpaper
    private fun chooseWallpaperTarget() {
        val labels = arrayOf(
            getString(R.string.lbl_home_screen),
            getString(R.string.lbl_lock_screen),
            getString(R.string.lbl_both_screens)
        )
        val targets = intArrayOf(WallpaperSetter.TARGET_HOME, WallpaperSetter.TARGET_LOCK, WallpaperSetter.TARGET_BOTH)
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.lbl_set_as)
            .setItems(labels) { _, which -> applyWallpaper(targets[which]) }
            .show()
    }

    private fun applyWallpaper(target: Int) {
        binding.btnSet.isEnabled = false
        Toast.makeText(this, R.string.msg_setting_wallpaper, Toast.LENGTH_SHORT).show()
        lifecycleScope.launch {
            val ok = runCatching { WallpaperSetter.apply(applicationContext, wallpaper, target) }.getOrDefault(false)
            binding.btnSet.isEnabled = true
            Toast.makeText(
                this@WallpaperPreviewActivity,
                if (ok) R.string.msg_wallpaper_set else R.string.msg_wallpaper_failed,
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    // endregion

    private fun share() {
        val link = wallpaper.pageUrl ?: wallpaper.fullUrl
        startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND)
                    .setType("text/plain")
                    .putExtra(Intent.EXTRA_TEXT, getString(R.string.lbl_share_text, link)),
                getString(R.string.lbl_share)
            )
        )
    }

    private fun showInfo() {
        val details = buildString {
            appendLine("Resolution: ${wallpaper.resolution}")
            formatFileSize(wallpaper.fileSize).takeIf { it.isNotEmpty() }?.let { appendLine("File size: $it") }
            wallpaper.fileType?.let { appendLine("Type: $it") }
            wallpaper.author?.let { appendLine("Author: $it") }
            if (wallpaper.views > 0) appendLine("Views: ${formatCount(wallpaper.views)}")
            if (wallpaper.favorites > 0) appendLine("Favorites: ${formatCount(wallpaper.favorites)}")
            append("Source: ${wallpaper.source}")
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.lbl_menu_about)
            .setMessage(details)
            .setPositiveButton(R.string.lbl_close, null)
            .show()
    }

    /** Tap the image to hide/show the overlays and enjoy the full wallpaper. */
    private fun toggleControls() {
        controlsVisible = !controlsVisible
        val alpha = if (controlsVisible) 1f else 0f
        listOf(binding.topBar, binding.bottomPanel).forEach { view ->
            if (controlsVisible) view.visibility = View.VISIBLE
            view.animate().alpha(alpha).setDuration(200).withEndAction {
                if (!controlsVisible) view.visibility = View.GONE
            }.start()
        }
    }

    private fun animatePanelIn() {
        binding.bottomPanel.translationY = 400f
        binding.bottomPanel.alpha = 0f
        binding.bottomPanel.animate().translationY(0f).alpha(1f).setStartDelay(150).setDuration(450).start()
    }

    private fun bounce(view: View) {
        view.animate().scaleX(1.2f).scaleY(1.2f).setDuration(120).withEndAction {
            view.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
        }.start()
    }

    companion object {
        private const val EXTRA_WALLPAPER = "wallpaper"

        fun start(context: Context, wallpaper: Wallpaper) {
            context.startActivity(
                Intent(context, WallpaperPreviewActivity::class.java).putExtra(EXTRA_WALLPAPER, wallpaper)
            )
        }
    }
}
