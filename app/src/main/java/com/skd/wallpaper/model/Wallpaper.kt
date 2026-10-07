package com.skd.wallpaper.model

import java.io.Serializable

/**
 * Source-independent wallpaper model used across the UI.
 * Built from either Wallhaven or Picsum responses.
 */
data class Wallpaper(
    val id: String,
    val thumbUrl: String,
    val fullUrl: String,
    val width: Int,
    val height: Int,
    val author: String? = null,
    val source: String,
    val pageUrl: String? = null,
    val views: Int = 0,
    val favorites: Int = 0,
    val fileSize: Long = 0,
    val fileType: String? = null,
    val colors: List<String> = emptyList(),
    val category: String? = null
) : Serializable {

    val resolution: String get() = "${width}×$height"

    val fileExtension: String
        get() = when {
            fileType?.contains("png") == true -> "png"
            else -> "jpg"
        }

    /** Unique key so favorites from different sources never collide. */
    val key: String get() = "$source-$id"
}
