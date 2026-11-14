package com.skd.wallpaper.utils

import android.content.Context
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.skd.wallpaper.BuildConfig
import com.skd.wallpaper.R

/**
 * Play Store in-app updates for the dashboard:
 *
 * 1. A new version on Play shows a snackbar with "Update", which downloads it in the
 *    background (flexible update); when it is downloaded a "Restart" snackbar installs it.
 * 2. If the user still hasn't updated [FORCE_AFTER_MS] (3 hours) after first being told,
 *    Play's full-screen update screen is shown instead, which installs it and restarts the app.
 *    (Apps can't install updates silently; this is the closest Play allows.)
 * 3. The first launch after an update shows "Wallvyn updated to vX".
 *
 * Only works for installs from Google Play (incl. internal testing / internal app sharing);
 * debug builds and sideloaded APKs simply never see an update.
 */
class AppUpdater(
    private val activity: ComponentActivity,
    private val snackbarParent: () -> View
) : DefaultLifecycleObserver {

    // Lazy: this object is created before the activity has a context to use
    private val manager: AppUpdateManager by lazy { AppUpdateManagerFactory.create(activity) }
    private val prefs by lazy { activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
    private var promptShownThisSession = false

    // Play's update screens report back here; nothing to do (we re-check on every resume)
    private val updateFlow: ActivityResultLauncher<IntentSenderRequest> =
        activity.registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {}

    private val downloadListener = InstallStateUpdatedListener { state ->
        if (state.installStatus() == InstallStatus.DOWNLOADED) showRestartSnackbar()
    }

    init {
        activity.lifecycle.addObserver(this)
    }

    override fun onCreate(owner: LifecycleOwner) {
        manager.registerListener(downloadListener)
        showUpdatedMessageOnce()
    }

    override fun onResume(owner: LifecycleOwner) = check()

    override fun onDestroy(owner: LifecycleOwner) = manager.unregisterListener(downloadListener)

    private fun check() {
        manager.appUpdateInfo.addOnSuccessListener { info ->
            if (activity.isFinishing || activity.isDestroyed) return@addOnSuccessListener
            when {
                // Flexible download finished while the app was in the background
                info.installStatus() == InstallStatus.DOWNLOADED -> showRestartSnackbar()

                // A forced update was interrupted (e.g. app left): resume it
                info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS ->
                    start(info, AppUpdateType.IMMEDIATE)

                info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE -> offer(info)
            }
        }
        // No Play Store / not installed from Play: nothing to do
    }

    private fun offer(info: AppUpdateInfo) {
        // Already downloading in the background: wait for the "Restart" snackbar
        if (info.installStatus() == InstallStatus.DOWNLOADING ||
            info.installStatus() == InstallStatus.PENDING
        ) return

        val firstSeen = firstSeenAt(info.availableVersionCode())
        val overdue = System.currentTimeMillis() - firstSeen >= FORCE_AFTER_MS
        when {
            overdue && info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE) ->
                start(info, AppUpdateType.IMMEDIATE)
            info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) && !promptShownThisSession -> {
                promptShownThisSession = true
                Snackbar.make(snackbarParent(), R.string.msg_update_available, Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.lbl_update) { start(info, AppUpdateType.FLEXIBLE) }
                    .show()
            }
        }
    }

    /** When the user was first told about [versionCode]; starts the 3-hour countdown. */
    private fun firstSeenAt(versionCode: Int): Long {
        if (prefs.getInt(KEY_SEEN_VERSION, -1) != versionCode) {
            prefs.edit()
                .putInt(KEY_SEEN_VERSION, versionCode)
                .putLong(KEY_SEEN_AT, System.currentTimeMillis())
                .apply()
        }
        return prefs.getLong(KEY_SEEN_AT, System.currentTimeMillis())
    }

    private fun start(info: AppUpdateInfo, type: Int) {
        runCatching {
            manager.startUpdateFlowForResult(info, updateFlow, AppUpdateOptions.newBuilder(type).build())
        }
    }

    private fun showRestartSnackbar() {
        if (activity.isFinishing || activity.isDestroyed) return
        Snackbar.make(snackbarParent(), R.string.msg_update_downloaded, Snackbar.LENGTH_INDEFINITE)
            .setAction(R.string.lbl_restart) { manager.completeUpdate() }
            .show()
    }

    /** "Wallvyn updated to vX" on the first launch after the version changed. */
    private fun showUpdatedMessageOnce() {
        val current = BuildConfig.VERSION_CODE
        val last = prefs.getInt(KEY_LAST_VERSION, -1)
        if (last == current) return
        prefs.edit().putInt(KEY_LAST_VERSION, current).apply()
        // Nothing stored yet: either a fresh install, or an update from a version without this
        // check. Android's own install/update times tell the two apart.
        if (last == -1 && !wasUpdatedFromPlay()) return
        snackbarParent().post {
            Snackbar.make(
                snackbarParent(),
                activity.getString(R.string.msg_app_updated, BuildConfig.VERSION_NAME),
                Snackbar.LENGTH_LONG
            ).show()
        }
    }

    private fun wasUpdatedFromPlay(): Boolean = runCatching {
        val info = activity.packageManager.getPackageInfo(activity.packageName, 0)
        info.lastUpdateTime > info.firstInstallTime
    }.getOrDefault(false)

    private companion object {
        const val FORCE_AFTER_MS = 3 * 60 * 60 * 1000L
        const val PREFS = "app_updates"
        const val KEY_SEEN_VERSION = "seen_version"
        const val KEY_SEEN_AT = "seen_at"
        const val KEY_LAST_VERSION = "last_version"
    }
}
