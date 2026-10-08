package com.skd.wallpaper.utils

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import com.skd.wallpaper.R

/** App theme chosen from the side drawer: follow the device, or force light / dark. */
enum class AppTheme(val nightMode: Int, @StringRes val label: Int) {
    SYSTEM(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, R.string.lbl_theme_system),
    LIGHT(AppCompatDelegate.MODE_NIGHT_NO, R.string.lbl_theme_light),
    DARK(AppCompatDelegate.MODE_NIGHT_YES, R.string.lbl_theme_dark)
}

object ThemeManager {

    private const val PREFS = "settings_prefs"
    private const val KEY_THEME = "app_theme"

    fun current(context: Context): AppTheme {
        val name = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_THEME, null)
        return AppTheme.values().firstOrNull { it.name == name } ?: AppTheme.SYSTEM
    }

    /** Applies the saved theme; call once from Application.onCreate. */
    fun applySaved(context: Context) {
        AppCompatDelegate.setDefaultNightMode(current(context).nightMode)
    }

    /** Saves and applies [theme]; open activities are recreated automatically. */
    fun set(context: Context, theme: AppTheme) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_THEME, theme.name).apply()
        AppCompatDelegate.setDefaultNightMode(theme.nightMode)
    }
}

/**
 * Draws behind the status/navigation bars and picks icon colors that stay visible:
 * dark icons on the light theme, light icons on the dark theme (or when [forceDark],
 * e.g. over a photo or gradient).
 */
fun ComponentActivity.applySystemBars(forceDark: Boolean = false) {
    val night = forceDark ||
        (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    val style = if (night) SystemBarStyle.dark(Color.TRANSPARENT)
    else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
    enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
}
