package com.skd.wallpaper.utils

import android.graphics.Color
import com.skd.wallpaper.model.Category
import com.skd.wallpaper.model.Wallpaper
import java.util.Locale

/** Card texts: a readable title and a "4K • Portrait" quality line. */
object WallpaperText {

    /**
     * Your own images and Picsum photos use their file name / author. Wallhaven has no titles,
     * so one is made from the image's dominant color and the tab, e.g. "Violet Landscape".
     */
    fun title(wallpaper: Wallpaper, category: Category): String {
        wallpaper.author?.takeIf { it.isNotBlank() }?.let { return it.toTitleCase() }
        val noun = when {
            category.isLiked -> "Wallpaper"
            // These tabs are named after the sort order, so use what they show instead
            category.sorting == "date_added" || category.title == "Trending" ->
                category.query?.toTitleCase() ?: category.title
            else -> category.title.toTitleCase()
        }
        val mood = wallpaper.colors.firstOrNull()?.let { colorName(it) }
        return listOfNotNull(mood, noun).joinToString(" ")
    }

    /** "4K • Portrait", or null while the size of your own images is still unknown. */
    fun meta(width: Int, height: Int): String? {
        if (width <= 0 || height <= 0) return null
        val longSide = maxOf(width, height)
        val quality = when {
            longSide >= 7680 -> "8K"
            longSide >= 5120 -> "5K"
            longSide >= 3840 -> "4K"
            longSide >= 2560 -> "2K"
            longSide >= 1920 -> "Full HD"
            else -> "HD"
        }
        val orientation = when {
            height > width -> "Portrait"
            width > height -> "Landscape"
            else -> "Square"
        }
        return "$quality  •  $orientation"
    }

    private fun colorName(hex: String): String? {
        val color = runCatching { Color.parseColor(hex) }.getOrNull() ?: return null
        val hsv = FloatArray(3).also { Color.colorToHSV(color, it) }
        val (hue, saturation, value) = Triple(hsv[0], hsv[1], hsv[2])
        return when {
            value < 0.22f -> "Midnight"
            saturation < 0.18f -> if (value > 0.75f) "Snowy" else "Misty"
            hue < 15f || hue >= 345f -> "Crimson"
            hue < 40f -> "Amber"
            hue < 65f -> "Golden"
            hue < 150f -> "Emerald"
            hue < 190f -> "Teal"
            hue < 225f -> "Azure"
            hue < 255f -> "Indigo"
            hue < 290f -> "Violet"
            else -> "Rose"
        }
    }

    private fun String.toTitleCase(): String = split(' ').filter { it.isNotBlank() }
        .joinToString(" ") { word -> word.replaceFirstChar { it.titlecase(Locale.getDefault()) } }
}
