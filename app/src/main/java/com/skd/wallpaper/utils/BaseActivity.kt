package com.skd.wallpaper.utils

import android.app.ProgressDialog
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.enableEdgeToEdge
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import com.skd.wallpaper.R
import com.skd.wallpaper.constants.Constants

open class BaseActivity<T : ViewDataBinding>(@LayoutRes private val layoutResId: Int) :
    AppCompatActivity() {

    open lateinit var binding: T
    private var sProgressDialog: ProgressDialog? = null

    open fun T.initialize() {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, layoutResId)
        initializeCircularProgressBar()
        binding.initialize()
        uiView()
    }

    private fun uiView(){
        enableEdgeToEdge()
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = true
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())

            // Only use bottom inset from keyboard if keyboard is visible, otherwise use system bar
            val bottomInset = if (imeVisible) imeInsets.bottom else systemBars.bottom
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, bottomInset)
            insets
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                isFragmentExist()
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        isFragmentExist()
        super.onBackPressed()
    }

    private fun isFragmentExist() {
        val backFragmentCount = supportFragmentManager.backStackEntryCount
        if (backFragmentCount == Constants.ZERO_INT || backFragmentCount == Constants.ONE_INT) {
            finish()
        }
    }

    private fun initializeCircularProgressBar() {
        sProgressDialog = ProgressDialog(this, R.style.CustomDialogStyle)
        sProgressDialog?.setIndeterminateDrawable(
            ContextCompat.getDrawable(this, R.drawable.progressbar_custome)
        )
        sProgressDialog?.setCancelable(false)
        sProgressDialog?.setProgressStyle(android.R.style.Widget_ProgressBar_Small)
    }

    open fun showProgressBar() {
        sProgressDialog?.show()
    }

    open fun dismissProgressBar() {
        sProgressDialog?.dismiss()
    }
}