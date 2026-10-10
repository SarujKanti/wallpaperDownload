package com.skd.wallpaper.activities

import android.app.Dialog
import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.style.CharacterStyle
import android.text.style.UpdateAppearance
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnLayout
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import coil.load
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.skd.wallpaper.BuildConfig
import com.skd.wallpaper.R
import com.skd.wallpaper.data.CategoryRepository
import com.skd.wallpaper.databinding.ActivityMainDashboardBinding
import com.skd.wallpaper.databinding.DialogFullscreenPartialBinding
import com.skd.wallpaper.fragments.FeedHost
import com.skd.wallpaper.fragments.WallpaperGridFragment
import com.skd.wallpaper.model.Category
import com.skd.wallpaper.model.Wallpaper
import com.skd.wallpaper.utils.AppTheme
import com.skd.wallpaper.utils.BaseActivity
import com.skd.wallpaper.utils.ThemeManager
import kotlinx.coroutines.launch

class MainDashboardActivity :
    BaseActivity<ActivityMainDashboardBinding>(R.layout.activity_main_dashboard), FeedHost {

    // Default tabs plus a tab per non-empty Supabase folder (found during the splash screen)
    private var categories: List<Category> = CategoryRepository.cached ?: Category.DEFAULT
    private lateinit var tabsAdapter: FragmentStateAdapter

    // Header photo per tab (picked from that tab's wallpapers) and the one currently shown
    private val heroUrls = mutableMapOf<Category, String>()
    private var heroUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        initHero()
        initToolbar()
        initSearch()
        initTabs()
        if (CategoryRepository.cached == null) {
            // Folder check still running (slow network): add those tabs when it finishes
            lifecycleScope.launch { updateTabs(CategoryRepository.load().await()) }
        }
    }

    private fun initHero() {
        // The header photo runs under the status bar; only its content is pushed below it
        val contentTop = binding.heroContent.paddingTop
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Bottom padding keeps the tabs and grid above the navigation bar (3-button or gesture)
            view.setPadding(bars.left, 0, bars.right, bars.bottom)
            binding.heroContent.updatePadding(top = contentTop + bars.top)
            // When the header scrolls away, a status-bar-high strip of it stays, so the pinned
            // tabs sit just below the status bar (that strip is covered by the scrim below)
            binding.hero.minimumHeight = bars.top
            binding.statusBarScrim.updateLayoutParams { height = bars.top }
            insets
        }
        ViewCompat.requestApplyInsets(binding.root)

        // Header = 25% of the screen height (photo included under the status bar)
        binding.root.doOnLayout { root ->
            binding.hero.updateLayoutParams { height = (root.height * HERO_HEIGHT_FRACTION).toInt() }
        }

        // Light status bar icons over the photo; theme-colored once the tabs reach the top
        setDarkStatusIcons(false)
        binding.appBar.addOnOffsetChangedListener { appBar, offset ->
            val range = appBar.totalScrollRange
            if (range <= 0) return@addOnOffsetChangedListener
            val progress = -offset / range.toFloat()
            // Fade a solid page-colored background in behind the status bar over the last part
            // of the scroll, so the photo never shows through above the pinned tabs
            binding.statusBarScrim.alpha = ((progress - 0.75f) / 0.25f).coerceIn(0f, 1f)
            setDarkStatusIcons(progress >= 0.9f && !isNightMode())
        }

        // "Wallora" in a white → pink → purple gradient
        binding.tvTitle.post {
            binding.tvTitle.paint.shader = LinearGradient(
                0f, 0f, binding.tvTitle.width.toFloat(), 0f,
                intArrayOf(Color.WHITE, Color.parseColor("#FFB3DE"), Color.parseColor("#C77DFF")),
                null, Shader.TileMode.CLAMP
            )
            binding.tvTitle.invalidate()
        }
        highlightHeadlineAccent()
    }

    /** Paints "vibe." in "Find your vibe.✨" with the purple → pink gradient. */
    /**
     * Light/dark changes (from the drawer's Theme option, or the phone's dark mode when the
     * theme follows the system) are handled here instead of by an automatic restart: the manifest
     * declares configChanges="uiMode". AppCompat's in-place restart reuses the old window, which
     * then loses edge-to-edge (header photo pushed below the status bar, icons invisible), so
     * the dashboard reopens itself in a fresh window instead.
     */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Phone switched dark mode while the theme follows the system
        if ((newConfig.uiMode and Configuration.UI_MODE_NIGHT_MASK) != startNightMode) reopenForNewTheme()
    }

    private var reopening = false

    /** Reopens the dashboard in a fresh window so it picks up the new light/dark theme. */
    private fun reopenForNewTheme() {
        if (reopening) return
        reopening = true
        startActivity(Intent(this, MainDashboardActivity::class.java))
        finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    // Light/dark mode this screen was created with
    private var startNightMode = 0

    private var darkStatusIcons: Boolean? = null

    private fun setDarkStatusIcons(dark: Boolean) {
        if (darkStatusIcons == dark) return
        darkStatusIcons = dark
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = dark
    }

    private fun isNightMode() =
        (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

    private fun highlightHeadlineAccent() {
        val text = getString(R.string.lbl_headline)
        val start = text.indexOf(getString(R.string.lbl_headline_accent))
        if (start < 0) return
        val end = start + getString(R.string.lbl_headline_accent).length
        binding.tvHeadline.post {
            val layout = binding.tvHeadline.layout ?: return@post
            val startX = layout.getPrimaryHorizontal(start)
            val endX = layout.getPrimaryHorizontal(end).takeIf { it > startX } ?: (startX + 1f)
            val gradient = object : CharacterStyle(), UpdateAppearance {
                override fun updateDrawState(paint: TextPaint) {
                    paint.shader = LinearGradient(
                        startX, 0f, endX, 0f,
                        Color.parseColor("#C77DFF"), Color.parseColor("#FF5FA2"), Shader.TileMode.CLAMP
                    )
                }
            }
            binding.tvHeadline.text = SpannableString(text).apply {
                setSpan(gradient, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
    }

    override fun onFeedLoaded(category: Category, items: List<Wallpaper>) {
        val url = heroUrls.getOrPut(category) { pickHeroUrl(items) ?: return }
        if (categories.getOrNull(binding.viewPager.currentItem) == category) showHero(url)
    }

    /** A sharp but not too heavy picture: the first full image up to ~5 MB, else the thumbnail. */
    private fun pickHeroUrl(items: List<Wallpaper>): String? =
        items.take(6).firstOrNull { it.fileSize in 1..5_000_000 }?.fullUrl ?: items.firstOrNull()?.thumbUrl

    private fun showHero(url: String) {
        if (url == heroUrl) return
        heroUrl = url
        val current = binding.ivHero.drawable
        binding.ivHero.load(url) {
            crossfade(600)
            // Keep the previous photo on screen until the new one is ready
            placeholder(current)
            error(current)
        }
    }

    private fun updateTabs(newCategories: List<Category>) {
        if (newCategories == categories) return
        val current = categories.getOrNull(binding.viewPager.currentItem)
        categories = newCategories
        tabsAdapter.notifyDataSetChanged()
        // Stay on the same category even if new tabs were inserted before it
        current?.let { binding.viewPager.setCurrentItem(categories.indexOf(it).coerceAtLeast(0), false) }
        for (i in 0 until binding.tabLayout.tabCount) {
            binding.tabLayout.getTabAt(i)?.let { styleTab(it, it.isSelected) }
        }
    }

    private fun initToolbar() {
        binding.tvTitle.text = resources.getString(R.string.lbl_git_repo)
        binding.ivMore.setOnClickListener { showPartialFullscreenDialog() }
        binding.ivLiked.setOnClickListener { openLiked() }
    }

    private fun initSearch() {
        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                submitSearch()
                true
            } else false
        }
        binding.btnSearch.setOnClickListener { submitSearch() }
    }

    private fun submitSearch() {
        val query = binding.etSearch.text?.toString()?.trim().orEmpty()
        if (query.isEmpty()) {
            binding.etSearch.requestFocus()
            return
        }
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.etSearch.windowToken, 0)
        binding.etSearch.clearFocus()
        SearchResultsActivity.start(this, query)
    }

    private fun initTabs() {
        tabsAdapter = object : FragmentStateAdapter(this) {
            override fun getItemCount() = categories.size
            override fun createFragment(position: Int): Fragment =
                WallpaperGridFragment.newInstance(categories[position])

            // Stable ids per category, so inserting tabs keeps each tab's loaded wallpapers
            override fun getItemId(position: Int) = categories[position].hashCode().toLong()
            override fun containsItem(itemId: Long) = categories.any { it.hashCode().toLong() == itemId }
        }
        binding.viewPager.adapter = tabsAdapter
        binding.viewPager.offscreenPageLimit = 1
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                // The header photo follows the selected tab
                categories.getOrNull(position)?.let { heroUrls[it] }?.let { showHero(it) }
            }
        })

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            val view = LayoutInflater.from(this).inflate(R.layout.tab_item, binding.tabLayout, false) as TextView
            val category = categories[position]
            view.text = "${category.emoji}  ${category.title}"
            tab.customView = view
        }.attach()

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) = styleTab(tab, true)
            override fun onTabUnselected(tab: TabLayout.Tab) = styleTab(tab, false)
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
        for (i in 0 until binding.tabLayout.tabCount) {
            binding.tabLayout.getTabAt(i)?.let { styleTab(it, it.isSelected) }
        }
    }

    private fun styleTab(tab: TabLayout.Tab, selected: Boolean) {
        val view = tab.customView as? TextView ?: return
        view.setBackgroundResource(if (selected) R.drawable.tab_background_selected else R.drawable.tab_background_default)
        view.setTextColor(ContextCompat.getColor(this, if (selected) R.color.white else R.color.text_primary))
        // Remember the tab's original font, so deselecting goes back to it (not a bold copy)
        val baseFont = view.getTag(R.id.tabText) as? Typeface ?: view.typeface.also { view.setTag(R.id.tabText, it) }
        view.typeface = if (selected) Typeface.create(baseFont, Typeface.BOLD) else baseFont
        view.animate().scaleX(if (selected) 1.04f else 1f).scaleY(if (selected) 1.04f else 1f).setDuration(150).start()
    }

    private fun openLiked() {
        binding.viewPager.setCurrentItem(categories.indexOf(Category.LIKED), true)
    }

    private fun showPartialFullscreenDialog() {
        val dialog = Dialog(this, R.style.SideDrawerDialog)
        val drawer = DialogFullscreenPartialBinding.inflate(layoutInflater)
        dialog.setContentView(drawer.root)
        dialog.window?.let { window -> setUpDrawerWindow(window) }
        padDrawerForSystemBars(drawer)

        drawer.tvVersion.text = getString(R.string.lbl_version, BuildConfig.VERSION_NAME)
        drawer.menuExplore.setOnClickListener {
            binding.viewPager.setCurrentItem(0, true)
            dialog.dismiss()
        }
        drawer.menuLiked.setOnClickListener {
            openLiked()
            dialog.dismiss()
        }
        drawer.menuDownloads.setOnClickListener {
            dialog.dismiss()
            openDownloads()
        }
        drawer.tvThemeValue.text = getString(ThemeManager.current(this).label)
        drawer.menuTheme.setOnClickListener {
            dialog.dismiss()
            chooseTheme()
        }
        drawer.menuShare.setOnClickListener {
            dialog.dismiss()
            shareApp()
        }
        drawer.menuAbout.setOnClickListener {
            dialog.dismiss()
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.lbl_git_repo)
                .setMessage(R.string.lbl_about_msg)
                .setPositiveButton(R.string.lbl_close, null)
                .show()
        }
        dialog.show()
    }

    /**
     * Full-height drawer on the left, drawn behind the status and navigation bars like a real
     * navigation drawer. Width is 82% of the screen, capped so it isn't huge on tablets/landscape.
     */
    private fun setUpDrawerWindow(window: Window) {
        applyGlass(window)
        val maxWidth = (DRAWER_MAX_WIDTH_DP * resources.displayMetrics.density).toInt()
        val width = minOf((resources.displayMetrics.widthPixels * 0.82).toInt(), maxWidth)
        window.setLayout(width, WindowManager.LayoutParams.MATCH_PARENT)
        window.setGravity(Gravity.START)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Dialogs avoid the system bars by default on Android 11+; let this one go under them
            window.attributes = window.attributes.apply { setFitInsetsTypes(0) }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
        // White status bar icons on the gradient header; nav bar icons follow the theme
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars =
                (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) != Configuration.UI_MODE_NIGHT_YES
        }
    }

    /**
     * iOS-style frosted glass. On Android 12+ the drawer blurs the app behind it (and the dimmed
     * screen behind is blurred too); the window background is the see-through panel drawn on
     * top of that blur. Where blur isn't available (Android 11 and older, or turned off by the
     * system, e.g. battery saver) a more opaque frosted color keeps everything readable.
     */
    private fun applyGlass(window: Window) {
        val density = resources.displayMetrics.density
        val canBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && windowManager.isCrossWindowBlurEnabled
        val panel = GradientDrawable().apply {
            val r = 28 * density
            // Rounded on the right edge only (the left edge is the screen edge)
            cornerRadii = floatArrayOf(0f, 0f, r, r, r, r, 0f, 0f)
            setColor(ContextCompat.getColor(this@MainDashboardActivity,
                if (canBlur) R.color.glass_panel else R.color.glass_panel_solid))
            setStroke((1 * density).toInt(), ContextCompat.getColor(this@MainDashboardActivity, R.color.glass_stroke))
        }
        window.setBackgroundDrawable(panel)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && canBlur) {
            window.setBackgroundBlurRadius((40 * density).toInt())
            window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            window.attributes = window.attributes.apply {
                blurBehindRadius = (12 * density).toInt()
                dimAmount = 0.25f
            }
        }
    }

    /** Keeps the drawer's header text below the status bar and its footer above the nav bar. */
    private fun padDrawerForSystemBars(drawer: DialogFullscreenPartialBinding) {
        // Taken from the dashboard, which always receives the real system bar sizes
        val bars = ViewCompat.getRootWindowInsets(binding.root)
            ?.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            ?: return
        drawer.headerContent.updatePadding(top = drawer.headerContent.paddingTop + bars.top)
        drawer.drawerFooter.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            bottomMargin += bars.bottom
            marginStart += bars.left
        }
        drawer.headerContent.updatePadding(left = drawer.headerContent.paddingLeft + bars.left)
    }

    private fun chooseTheme() {
        val themes = AppTheme.values()
        val selected = themes.indexOf(ThemeManager.current(this))
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.lbl_choose_theme)
            .setSingleChoiceItems(themes.map { getString(it.label) }.toTypedArray(), selected) { dialog, which ->
                dialog.dismiss()
                if (themes[which] == ThemeManager.current(this)) return@setSingleChoiceItems
                ThemeManager.set(this, themes[which])
                // This screen handles uiMode changes itself (see onConfigurationChanged), so
                // AppCompat won't restart it: reopen it with the new theme
                reopenForNewTheme()
            }
            .show()
    }

    private fun openDownloads() {
        try {
            startActivity(Intent(DownloadManager.ACTION_VIEW_DOWNLOADS))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, R.string.msg_download_done, Toast.LENGTH_LONG).show()
        }
    }

    private fun shareApp() {
        val text = "${getString(R.string.lbl_git_repo)} – ${getString(R.string.lbl_tagline)}\n" +
            "https://play.google.com/store/apps/details?id=${BuildConfig.APPLICATION_ID}"
        startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text),
                getString(R.string.lbl_menu_share)
            )
        )
    }

    companion object {
        // Like Material's navigation drawer: never wider than this, even on tablets/landscape
        private const val DRAWER_MAX_WIDTH_DP = 360
        private const val HERO_HEIGHT_FRACTION = 0.25f
    }
}
