package com.skd.wallpaper.notifications

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Receives notifications you send from the Firebase console (Messaging → New campaign) to
 * every install. Every install subscribes to the topic [TOPIC_ALL], so you can target either
 * the whole app or that topic.
 *
 * Optional "Custom data" keys in the console:
 *   tab = a tab title to open, e.g. "Nature" (Trending when left out)
 *
 * When the app is closed Android shows the notification itself and the tap goes through the
 * splash screen, which passes the "tab" key on. When the app is open it arrives here instead.
 */
class PushService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        val notification = message.notification
        val title = notification?.title ?: message.data["title"] ?: return
        val text = notification?.body ?: message.data["body"]
        val imageUrl = notification?.imageUrl?.toString() ?: message.data["image"]
        // Called on a background thread with time to spare; give the picture a few seconds
        val preview = imageUrl?.let { url ->
            runBlocking { withTimeoutOrNull(8_000) { TrendingNotifier.loadBitmap(this@PushService, url) } }
        }
        TrendingNotifier.post(
            this, title, text,
            tab = message.data[KEY_TAB],
            preview = preview,
            id = (message.messageId ?: title).hashCode()
        )
    }

    // Topic subscriptions survive token changes; nothing to send to a server
    override fun onNewToken(token: String) {}

    companion object {
        // Unique to Wallora: the Firebase project is shared with other apps
        const val TOPIC_ALL = "wallora_all"
        const val KEY_TAB = "tab"

        /** Joins the [TOPIC_ALL] topic; does nothing when the app was built without Firebase config. */
        fun subscribe(context: Context) {
            if (FirebaseApp.getApps(context).isEmpty()) return
            FirebaseMessaging.getInstance().subscribeToTopic(TOPIC_ALL)
        }
    }
}
