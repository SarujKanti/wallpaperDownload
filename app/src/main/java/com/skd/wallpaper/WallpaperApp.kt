package com.skd.wallpaper

import android.app.Activity
import android.app.Application
import android.os.Build
import android.os.Bundle
import com.skd.wallpaper.utils.DeviceUtils
import com.skd.wallpaper.utils.ThemeManager

class WallpaperApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Apply the user's Light / Dark / System choice before any screen is shown
        ThemeManager.applySaved(this)
        // Phones: every screen stays in portrait; tablets can rotate (see DeviceUtils)
        registerActivityLifecycleCallbacks(OrientationLock)
    }

    private object OrientationLock : ActivityLifecycleCallbacks {
        // Android 10+: before the screen is created, so it never flashes in landscape
        override fun onActivityPreCreated(activity: Activity, savedInstanceState: Bundle?) =
            DeviceUtils.applyOrientation(activity)

        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) DeviceUtils.applyOrientation(activity)
        }

        override fun onActivityStarted(activity: Activity) {}
        override fun onActivityResumed(activity: Activity) {}
        override fun onActivityPaused(activity: Activity) {}
        override fun onActivityStopped(activity: Activity) {}
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
        override fun onActivityDestroyed(activity: Activity) {}
    }
}
