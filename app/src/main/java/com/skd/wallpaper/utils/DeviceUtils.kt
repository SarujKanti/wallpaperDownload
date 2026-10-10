package com.skd.wallpaper.utils

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import com.skd.wallpaper.R

/** Phone vs tablet rules for layout and orientation. */
object DeviceUtils {

    // Phone: always 2 columns (phones are locked to portrait)
    private const val PHONE_COLUMNS = 2

    // Tablet: 3 columns in portrait, 4 in landscape
    private const val TABLET_PORTRAIT_COLUMNS = 3
    private const val TABLET_LANDSCAPE_COLUMNS = 4

    /** Tablet = smallest screen width of 600dp or more (see values-sw600dp/bools.xml). */
    fun isTablet(context: Context): Boolean = context.resources.getBoolean(R.bool.is_tablet)

    fun isLandscape(context: Context): Boolean =
        context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    /** Number of wallpaper columns in the grids. */
    fun gridColumns(context: Context): Int {
        if (!isTablet(context)) return PHONE_COLUMNS
        // Tablet
        return if (isLandscape(context)) TABLET_LANDSCAPE_COLUMNS else TABLET_PORTRAIT_COLUMNS
    }

    /** Phones stay in portrait even when rotated; tablets may rotate freely. */
    fun applyOrientation(activity: Activity) {
        if (!isTablet(activity)) {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }
}
