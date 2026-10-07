package com.skd.wallpaper.utils

import android.app.DownloadManager
import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Environment
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.size.Precision
import com.skd.wallpaper.model.Wallpaper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.util.Locale

const val DOWNLOAD_FOLDER = "WallpaperView"

fun formatCount(count: Int): String = when {
    count >= 1_000_000 -> String.format(Locale.US, "%.1fM", count / 1_000_000f)
    count >= 1_000 -> String.format(Locale.US, "%.1fK", count / 1_000f)
    else -> count.toString()
}

fun formatFileSize(bytes: Long): String = when {
    bytes <= 0 -> ""
    bytes >= 1024 * 1024 -> String.format(Locale.US, "%.1f MB", bytes / (1024f * 1024f))
    else -> String.format(Locale.US, "%d KB", bytes / 1024)
}

sealed class DownloadStatus {
    data class Progress(val percent: Int) : DownloadStatus()
    object Success : DownloadStatus()
    object Failed : DownloadStatus()
}

object WallpaperDownloader {

    /**
     * Saves the full-resolution image to Pictures/WallpaperView using the system
     * DownloadManager (shows a notification and survives the app being closed).
     * Emits progress until the download finishes.
     */
    fun download(context: Context, wallpaper: Wallpaper): Flow<DownloadStatus> = flow {
        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val fileName = "${wallpaper.source.lowercase(Locale.US)}_${wallpaper.id}.${wallpaper.fileExtension}"

        val request = DownloadManager.Request(Uri.parse(wallpaper.fullUrl))
            .setTitle(fileName)
            .setDescription("Wallpaper ${wallpaper.resolution}")
            .setMimeType(if (wallpaper.fileExtension == "png") "image/png" else "image/jpeg")
            .addRequestHeader("User-Agent", "WallpaperView-Android/1.0")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_PICTURES, "$DOWNLOAD_FOLDER/$fileName")
        @Suppress("DEPRECATION")
        request.allowScanningByMediaScanner()

        val id = manager.enqueue(request)
        emit(DownloadStatus.Progress(0))

        while (true) {
            delay(300)
            val cursor = manager.query(DownloadManager.Query().setFilterById(id)) ?: break
            val status = cursor.use {
                if (!it.moveToFirst()) return@use DownloadManager.STATUS_FAILED
                val downloaded = it.getLong(it.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                val total = it.getLong(it.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                if (total > 0) emit(DownloadStatus.Progress((downloaded * 100 / total).toInt()))
                it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            }
            when (status) {
                DownloadManager.STATUS_SUCCESSFUL -> { emit(DownloadStatus.Success); return@flow }
                DownloadManager.STATUS_FAILED -> { emit(DownloadStatus.Failed); return@flow }
            }
        }
        emit(DownloadStatus.Failed)
    }.flowOn(Dispatchers.IO)
}

object WallpaperSetter {

    const val TARGET_HOME = WallpaperManager.FLAG_SYSTEM
    const val TARGET_LOCK = WallpaperManager.FLAG_LOCK
    const val TARGET_BOTH = WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK

    /** Downloads the full image and applies it. Returns true on success. */
    suspend fun apply(context: Context, wallpaper: Wallpaper, target: Int): Boolean {
        val metrics = context.resources.displayMetrics
        // Cap the decoded size so very large (e.g. 8K) images don't run out of memory
        val maxSide = maxOf(metrics.widthPixels, metrics.heightPixels) * 2
        val request = ImageRequest.Builder(context)
            .data(wallpaper.fullUrl)
            .size(maxSide, maxSide)
            .precision(Precision.INEXACT)
            .allowHardware(false)
            .build()

        val result = context.imageLoader.execute(request)
        val bitmap: Bitmap = ((result as? SuccessResult)?.drawable as? BitmapDrawable)?.bitmap ?: return false

        return withContext(Dispatchers.IO) {
            runCatching {
                WallpaperManager.getInstance(context).setBitmap(bitmap, null, true, target)
            }.isSuccess
        }
    }
}
