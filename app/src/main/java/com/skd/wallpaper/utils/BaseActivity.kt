package com.skd.wallpaper.utils

import android.os.Bundle
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding

/** Data-binding activity that draws edge to edge and keeps its content clear of the system bars. */
open class BaseActivity<T : ViewDataBinding>(@LayoutRes private val layoutResId: Int) :
    AppCompatActivity() {

    open lateinit var binding: T

    open fun T.initialize() {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, layoutResId)
        binding.initialize()
        uiView()
    }

    private fun uiView() {
        // Status bar icons follow the app theme so they stay visible
        applySystemBars()
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
}
