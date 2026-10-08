package com.skd.wallpaper

import android.app.Application
import com.skd.wallpaper.utils.ThemeManager

class WallpaperApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Apply the user's Light / Dark / System choice before any screen is shown
        ThemeManager.applySaved(this)
    }
}
