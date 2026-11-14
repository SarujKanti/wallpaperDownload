package com.skd.wallpaper.utils

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.Gravity
import android.view.Window
import android.view.WindowManager
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import com.skd.wallpaper.R

/**
 * One frosted-glass look for every dialog in the app (see [styleAsCenteredGlass]).
 * On Android 12+ the window blurs whatever is behind it; where blur isn't available
 * (Android 11 and older, or turned off by the system, e.g. battery saver) a more opaque
 * frosted color is used so text always stays readable.
 */
object Glass {

    /** True when the system can blur behind windows right now. */
    fun canBlur(activity: Activity): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && activity.windowManager.isCrossWindowBlurEnabled

    /** Rounded glass panel; [blurColor] is used over real blur, [solidColor] without it. */
    fun panel(
        context: Context,
        blur: Boolean,
        @ColorRes blurColor: Int = R.color.glass_panel,
        @ColorRes solidColor: Int = R.color.glass_panel_solid,
        radiusDp: Float = 28f
    ) = GradientDrawable().apply {
        val density = context.resources.displayMetrics.density
        cornerRadius = radiusDp * density
        setColor(ContextCompat.getColor(context, if (blur) blurColor else solidColor))
        setStroke((1 * density).toInt(), ContextCompat.getColor(context, R.color.glass_stroke))
    }

    /**
     * Blurs behind [window] (Android 12+ only, no-op elsewhere): the panel's own area gets a
     * strong frosted blur, and the whole screen behind the dialog is softly blurred and dimmed,
     * like iOS, so busy wallpapers never compete with the dialog text.
     */
    fun blurBehind(window: Window, context: Context, panelRadiusDp: Int = 40, screenRadiusDp: Int = 18) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val density = context.resources.displayMetrics.density
        window.setBackgroundBlurRadius((panelRadiusDp * density).toInt())
        window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
        window.attributes = window.attributes.apply { blurBehindRadius = (screenRadiusDp * density).toInt() }
    }
}

/**
 * Turns a custom [Dialog]'s window into a centered frosted-glass card (Set wallpaper,
 * Choose theme): glass panel + blur, dimmed screen, side margins, at most 420dp wide.
 */
fun Dialog.styleAsCenteredGlass(
    activity: Activity,
    @ColorRes blurColor: Int = R.color.glass_panel,
    @ColorRes solidColor: Int = R.color.glass_panel_solid
) {
    val window = window ?: return
    val density = activity.resources.displayMetrics.density
    val blur = Glass.canBlur(activity)
    window.setBackgroundDrawable(Glass.panel(activity, blur, blurColor, solidColor))
    if (blur) Glass.blurBehind(window, activity)
    window.setDimAmount(0.4f)

    val margin = (24 * density).toInt()
    val width = minOf(activity.resources.displayMetrics.widthPixels - 2 * margin, (420 * density).toInt())
    window.setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT)
    window.setGravity(Gravity.CENTER)
}
