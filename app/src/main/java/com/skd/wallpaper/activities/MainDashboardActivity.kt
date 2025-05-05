package com.skd.wallpaper.activities

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.skd.wallpaper.R
import com.skd.wallpaper.databinding.ActivityMainDashboardBinding
import com.skd.wallpaper.fragments.FullScreenFragment
import com.skd.wallpaper.utils.BaseActivity

class MainDashboardActivity  : BaseActivity<ActivityMainDashboardBinding>(R.layout.activity_main_dashboard) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        initToolbar()
        if(savedInstanceState == null){
            val fragment = FullScreenFragment()
            fragment.arguments = intent.extras
            loadFragment(fragment)
        }
    }

    private fun initToolbar() {
        setSupportActionBar(binding.childToolbar.toolbar)
        title = resources.getString(R.string.lbl_git_repo)
    }

    fun loadFragment(fragment: Fragment){
        val transaction: FragmentTransaction = supportFragmentManager.beginTransaction()
        transaction.add(R.id.fragmentContainer, fragment)
        transaction.commit()
    }


}