package com.notifsync.app.data.remote

import android.util.Log
import com.notifsync.app.data.SupabaseRepository
import com.notifsync.app.data.local.NotificationQueueStore
import com.notifsync.app.data.local.SessionStore
import com.notifsync.app.data.model.NotificationRequest

class NotificationUploadManager(
    private val repository: SupabaseRepository,
    private val sessionStore: SessionStore,
    private val queueStore: NotificationQueueStore
) {
    private companion object {
        const val TAG = "NotificationUpload"
    }

    suspend fun uploadOrQueue(request: NotificationRequest) {
        val deviceId = sessionStore.getDeviceId()
        if (deviceId.isNullOrBlank()) {
            Log.w(TAG, "Skipping upload because device is not registered")
            return
        }

        try {
            flushQueued(deviceId)
            val auth = ensureAuth()
            repository.insertNotification(auth.accessToken, request.copy(deviceId = deviceId))
            sessionStore.markSyncedNow()
            Log.i(TAG, "Uploaded notification type=${request.type} sender=${request.sender}")
        } catch (e: Exception) {
            Log.e(TAG, "Upload failed; queuing notification", e)
            queueStore.enqueue(request.copy(deviceId = deviceId))
        }
    }

    suspend fun flushQueued(deviceId: String? = sessionStore.getDeviceId()) {
        val actualDeviceId = deviceId ?: return
        val pending = queueStore.getPendingNotifications()
        if (pending.isEmpty()) return

        val auth = ensureAuth()
        val remaining = mutableListOf<NotificationRequest>()
        for (item in pending) {
            try {
                repository.insertNotification(auth.accessToken, item.copy(deviceId = actualDeviceId))
                sessionStore.markSyncedNow()
            } catch (e: Exception) {
                Log.e(TAG, "Queued notification upload failed; keeping queue", e)
                remaining.add(item)
            }
        }
        if (remaining.isEmpty()) queueStore.clear() else queueStore.savePendingNotifications(remaining)
    }

    private suspend fun ensureAuth() = try {
        if (sessionStore.getAccessToken().isNullOrBlank() || sessionStore.isAccessTokenExpired()) {
            val refreshToken = sessionStore.getRefreshToken() ?: error("Not authenticated")
            val refreshed = repository.refreshSession(refreshToken)
            sessionStore.saveAuth(refreshed)
        }
        sessionStore.requireAuth()
    } catch (e: Exception) {
        throw e
    }
}
