package com.notifsync.app.data.service

import android.app.Notification
import android.content.Context
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.notifsync.app.NotificationSyncApplication
import com.notifsync.app.data.model.NotificationRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.time.Instant

class AppNotificationListenerService : NotificationListenerService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return
        scope.launch {
            try {
                val context = applicationContext
                val whitelistStore = application(context).container.whitelistStore
                if (!whitelistStore.isWhitelisted(sbn.packageName)) {
                    return@launch
                }

                val packageManager = context.packageManager
                val appName = resolveAppName(packageManager, sbn.packageName)
                val extras = sbn.notification.extras
                val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
                    ?: extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()
                val body = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
                    ?: extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()

                if (body.isBlank() && title.isNullOrBlank()) {
                    Log.d(TAG, "Skipping empty notification from ${sbn.packageName}")
                    return@launch
                }

                val request = NotificationRequest(
                    deviceId = "",
                    type = "app",
                    appPackageName = sbn.packageName,
                    sender = appName,
                    title = title,
                    body = body.ifBlank { title.orEmpty() },
                    originalTimestamp = Instant.ofEpochMilli(sbn.postTime).toString()
                )
                application(context).container.uploader.uploadOrQueue(request)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to process notification", e)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    private fun resolveAppName(packageManager: PackageManager, packageName: String): String {
        return try {
            val info = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(info).toString().ifBlank { packageName }
        } catch (_: Exception) {
            packageName
        }
    }

    private fun application(context: Context): NotificationSyncApplication {
        return context.applicationContext as NotificationSyncApplication
    }

    private companion object {
        const val TAG = "AppNotifListener"
    }
}
