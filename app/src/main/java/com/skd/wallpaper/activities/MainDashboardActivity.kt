package com.skd.wallpaper.activities

import android.app.Dialog
import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.skd.wallpaper.BuildConfig
import com.skd.wallpaper.R
import com.skd.wallpaper.databinding.ActivityMainDashboardBinding
import com.skd.wallpaper.databinding.DialogFullscreenPartialBinding
import com.skd.wallpaper.fragments.WallpaperGridFragment
import com.skd.wallpaper.model.Category
import com.skd.wallpaper.utils.BaseActivity

class MainDashboardActivity : BaseActivity<ActivityMainDashboardBinding>(R.layout.activity_main_dashboard) {

    private val categories = Category.ALL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initToolbar()
        initSearch()
        initTabs()
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
        binding.viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount() = categories.size
            override fun createFragment(position: Int): Fragment =
                WallpaperGridFragment.newInstance(categories[position])
        }
        binding.viewPager.offscreenPageLimit = 1

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
        view.setTextColor(ContextCompat.getColor(this, if (selected) R.color.white else R.color.text_secondary))
        view.animate().scaleX(if (selected) 1.05f else 1f).scaleY(if (selected) 1.05f else 1f).setDuration(150).start()
    }

    private fun openLiked() {
        binding.viewPager.setCurrentItem(categories.indexOf(Category.LIKED), true)
    }

    private fun showPartialFullscreenDialog() {
        val dialog = Dialog(this, R.style.SideDrawerDialog)
        val drawer = DialogFullscreenPartialBinding.inflate(layoutInflater)
        dialog.setContentView(drawer.root)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        // 80% of screen width, full height, anchored to the left like a navigation drawer
        val width = (resources.displayMetrics.widthPixels * 0.8).toInt()
        dialog.window?.setLayout(width, WindowManager.LayoutParams.MATCH_PARENT)
        dialog.window?.setGravity(Gravity.START)

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
}
