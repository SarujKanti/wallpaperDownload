package com.skd.wallpaper.activities

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.Window
import android.view.WindowManager
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.fragment.app.FragmentTransaction
import com.skd.wallpaper.R
import com.skd.wallpaper.databinding.ActivityMainDashboardBinding
import com.skd.wallpaper.fragments.BookmarkedTabsFragment
import com.skd.wallpaper.fragments.ColorfullTabFragment
import com.skd.wallpaper.fragments.DarkTabFragment
import com.skd.wallpaper.fragments.FullScreenFragment
import com.skd.wallpaper.fragments.RecentTabsFragment
import com.skd.wallpaper.utils.BaseActivity

class MainDashboardActivity : BaseActivity<ActivityMainDashboardBinding>(R.layout.activity_main_dashboard) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initToolbar()
        if(savedInstanceState == null){
            val fragment = FullScreenFragment()
            fragment.arguments = intent.extras
            loadFragment(fragment)
        }

//        val adapter = TabsPagerAdapter(supportFragmentManager)
//        binding.viewPager.adapter = adapter
//        binding.tabLayout.setupWithViewPager(binding.viewPager)
    }

    private fun initToolbar() {
        binding.tvTitle.text = resources.getString(R.string.lbl_git_repo)
        binding.ivMore.setOnClickListener {
            showPartialFullscreenDialog()
        }
    }

    fun loadFragment(fragment: Fragment){
        val transaction: FragmentTransaction = supportFragmentManager.beginTransaction()
        transaction.add(R.id.fragmentContainer, fragment)
        transaction.commit()
    }


    class TabsPagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

        private val fragments = listOf(
            RecentTabsFragment(),
            BookmarkedTabsFragment(),
            DarkTabFragment(),
            ColorfullTabFragment()
        )

        private val titles = listOf(
            "Recent",
            "Liked",
            "Dark",
            "ColorFull"
        )

        override fun getCount(): Int = fragments.size

        override fun getItem(position: Int): Fragment = fragments[position]

        override fun getPageTitle(position: Int): CharSequence = titles[position]
    }

    private fun showPartialFullscreenDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_fullscreen_partial)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        // Set width to 80% of screen width and height to full
        val metrics = resources.displayMetrics
        val width = (metrics.widthPixels * 0.8).toInt()
        val height = WindowManager.LayoutParams.MATCH_PARENT

        dialog.window?.setLayout(width, height)
        dialog.window?.setGravity(Gravity.START)
        dialog.show()
    }


}