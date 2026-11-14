package com.skd.wallpaper.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.skd.wallpaper.R
import com.skd.wallpaper.activities.MainDashboardActivity
import com.skd.wallpaper.data.WallpaperRepository
import com.skd.wallpaper.model.Category
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.TimeUnit

/**
 * "New trending wallpapers" reminder, posted on every install every [INTERVAL_HOURS] hours.
 * Runs on the phone itself (WorkManager), so no server or push service is needed; it keeps
 * its schedule across restarts and app updates. Tapping it opens the Trending tab.
 */
object TrendingNotifier {

    private const val INTERVAL_HOURS = 14L
    private const val WORK_NAME = "trending_reminder"
    private const val CHANNEL_ID = "trending"
    private const val NOTIFICATION_ID = 1401

    /** Starts the repeating reminder (does nothing if it is already scheduled). */
    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<TrendingWorker>(INTERVAL_HOURS, TimeUnit.HOURS)
            // First one 14 hours after install, not the moment the app is opened
            .setInitialDelay(INTERVAL_HOURS, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notif_channel_trending),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = context.getString(R.string.notif_channel_trending_desc) }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun canNotify(context: Context): Boolean =
        (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED) &&
            NotificationManagerCompat.from(context).areNotificationsEnabled()

    /** The 14-hour reminder. */
    fun show(context: Context, preview: Bitmap?) = post(
        context,
        title = context.getString(R.string.notif_trending_title),
        text = context.getString(R.string.notif_trending_text),
        tab = null,
        preview = preview,
        id = NOTIFICATION_ID
    )

    /**
     * Posts a notification that opens the dashboard on [tab] (a tab title such as "Nature";
     * Trending when null or unknown). Also used for pushes sent from the Firebase console.
     */
    @android.annotation.SuppressLint("MissingPermission") // checked by canNotify()
    fun post(context: Context, title: String, text: String?, tab: String?, preview: Bitmap?, id: Int) {
        if (!canNotify(context)) return
        val openTab = Intent(context, MainDashboardActivity::class.java)
            .putExtra(MainDashboardActivity.EXTRA_TAB, tab ?: Category.DEFAULT.first().title)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        val tap = PendingIntent.getActivity(
            context, id, openTab,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ContextCompat.getColor(context, R.color.notification_accent))
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(tap)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        if (preview != null) {
            builder.setLargeIcon(preview).setStyle(
                NotificationCompat.BigPictureStyle().bigPicture(preview).bigLargeIcon(null as Bitmap?)
            )
        }
        NotificationManagerCompat.from(context).notify(id, builder.build())
    }

    /** Loads an image URL as a software bitmap for a notification, or null on failure. */
    suspend fun loadBitmap(context: Context, url: String): Bitmap? = runCatching {
        val request = ImageRequest.Builder(context)
            .data(url)
            .allowHardware(false) // notifications need a software bitmap
            .size(720, 720)
            .build()
        (context.imageLoader.execute(request) as? SuccessResult)?.drawable?.toBitmap()
    }.getOrNull()
}

/** Fetches today's top trending wallpaper as the preview, then posts the reminder. */
class TrendingWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (!TrendingNotifier.canNotify(applicationContext)) return Result.success()
        // A missing preview never blocks the reminder: show it text-only instead
        val preview = withTimeoutOrNull(20_000) { runCatching { loadPreview() }.getOrNull() }
        TrendingNotifier.show(applicationContext, preview)
        return Result.success()
    }

    private suspend fun loadPreview(): Bitmap? {
        val top = WallpaperRepository.load(Category.DEFAULT.first(), 1).items.firstOrNull() ?: return null
        return TrendingNotifier.loadBitmap(applicationContext, top.thumbUrl)
    }
}
