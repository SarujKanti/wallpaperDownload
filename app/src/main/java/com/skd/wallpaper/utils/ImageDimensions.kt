package com.skd.wallpaper.utils

import android.content.Context
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import coil.annotation.ExperimentalCoilApi
import coil.imageLoader
import com.skd.wallpaper.model.Wallpaper
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

/**
 * Resolution label ("1920×1080") for any wallpaper. Wallhaven/Picsum report sizes in the API;
 * Supabase Storage doesn't, so for your own images the size is read from the header of the
 * file Coil already downloaded to its disk cache (no extra network use).
 */
object ImageDimensions {

    private val cache = ConcurrentHashMap<String, Pair<Int, Int>>()
    private val executor = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())

    /** The label if already known, otherwise null. */
    fun label(wallpaper: Wallpaper): String? = when {
        wallpaper.width > 0 && wallpaper.height > 0 -> "${wallpaper.width}×${wallpaper.height}"
        else -> cache[wallpaper.fullUrl]?.let { (w, h) -> "$w×$h" }
    }

    /**
     * Delivers the label on the main thread. For your own images call this after the image
     * has loaded, so the file is in Coil's disk cache. Does nothing if the size can't be read.
     */
    @OptIn(ExperimentalCoilApi::class)
    fun resolve(context: Context, wallpaper: Wallpaper, onResult: (String) -> Unit) {
        label(wallpaper)?.let { onResult(it); return }
        val diskCache = context.imageLoader.diskCache ?: return
        executor.execute {
            val size = runCatching {
                diskCache.openSnapshot(wallpaper.fullUrl)?.use { snapshot ->
                    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeFile(snapshot.data.toFile().path, options)
                    (options.outWidth to options.outHeight).takeIf { it.first > 0 && it.second > 0 }
                }
            }.getOrNull() ?: return@execute
            cache[wallpaper.fullUrl] = size
            main.post { onResult("${size.first}×${size.second}") }
        }
    }
}
