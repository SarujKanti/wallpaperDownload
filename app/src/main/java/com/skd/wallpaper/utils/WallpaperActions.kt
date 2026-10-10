package com.skd.wallpaper.utils

import android.Manifest
import android.app.DownloadManager
import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.size.Precision
import com.skd.wallpaper.R
import com.skd.wallpaper.model.Wallpaper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

/**
 * Saved under Pictures/Wallora/Download. Android 10+ only lets apps write into the
 * standard shared folders (Pictures, Download, …), not a new folder at the storage root.
 */
const val DOWNLOAD_FOLDER = "Wallora/Download"

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

    /** Fixed file name per wallpaper, e.g. "wallhaven_abc123.jpg", so downloads can be found again. */
    fun fileName(wallpaper: Wallpaper): String {
        val prefix = wallpaper.source.lowercase(Locale.US).replace(Regex("[^a-z0-9]+"), "_")
        return "${prefix}_${wallpaper.id}.${wallpaper.fileExtension}"
    }

    @Suppress("DEPRECATION")
    private fun downloadedFile(wallpaper: Wallpaper) = File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
        "$DOWNLOAD_FOLDER/${fileName(wallpaper)}"
    )

    /** Permission needed to see downloaded images: READ_MEDIA_IMAGES on Android 13+, storage before. */
    val readPermission: String
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_IMAGES
        else Manifest.permission.READ_EXTERNAL_STORAGE

    fun hasReadPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, readPermission) == PackageManager.PERMISSION_GRANTED

    /**
     * True if [wallpaper] is in Pictures/Wallora/Download. Checks the file itself (needs the read
     * permission for files the app doesn't own) and, on Android 10+, the system media index, which
     * also finds the app's own downloads without any permission. A file the user deleted counts
     * as not downloaded.
     */
    suspend fun isDownloaded(context: Context, wallpaper: Wallpaper): Boolean = withContext(Dispatchers.IO) {
        if (runCatching { downloadedFile(wallpaper).exists() }.getOrDefault(false)) return@withContext true
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return@withContext false
        runCatching {
            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.Images.Media._ID),
                "${MediaStore.Images.Media.DISPLAY_NAME} = ? AND ${MediaStore.Images.Media.RELATIVE_PATH} LIKE ?",
                arrayOf(fileName(wallpaper), "${Environment.DIRECTORY_PICTURES}/$DOWNLOAD_FOLDER%"),
                null
            )?.use { it.count > 0 } ?: false
        }.getOrDefault(false)
    }

    /**
     * Saves the full-resolution image to Pictures/Wallora/Download using the system
     * DownloadManager (shows a notification and survives the app being closed).
     * Emits progress until the download finishes.
     */
    fun download(context: Context, wallpaper: Wallpaper): Flow<DownloadStatus> = flow {
        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val fileName = fileName(wallpaper)

        // Redownload: replace the old copy where allowed (otherwise the system saves "name-1.jpg")
        runCatching { downloadedFile(wallpaper).takeIf { it.exists() }?.delete() }

        val request = DownloadManager.Request(Uri.parse(wallpaper.fullUrl))
            .setTitle(fileName)
            .setDescription(ImageDimensions.label(wallpaper)?.let { "Wallpaper $it" } ?: "Wallpaper")
            .setMimeType(wallpaper.fileType ?: "image/jpeg")
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

/** Opens the system share sheet with a link to [wallpaper]. */
fun shareWallpaper(context: Context, wallpaper: Wallpaper) {
    val link = wallpaper.pageUrl ?: wallpaper.fullUrl
    context.startActivity(
        Intent.createChooser(
            Intent(Intent.ACTION_SEND)
                .setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT, context.getString(R.string.lbl_share_text, link)),
            context.getString(R.string.lbl_share)
        )
    )
}
