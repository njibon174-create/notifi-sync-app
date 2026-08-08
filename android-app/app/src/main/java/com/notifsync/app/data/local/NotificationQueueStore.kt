package com.notifsync.app.data.local

import android.content.Context
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.notifsync.app.data.model.NotificationRequest

class NotificationQueueStore(context: Context) {
    private val prefs = context.getSharedPreferences("notification_sync_queue", Context.MODE_PRIVATE)
    private val gson = Gson()

    private companion object {
        const val KEY_PENDING = "pending_notifications_json"
    }

    fun getPendingNotifications(): MutableList<NotificationRequest> {
        val json = prefs.getString(KEY_PENDING, null).orEmpty()
        if (json.isBlank()) return mutableListOf()
        return try {
            val type = object : TypeToken<MutableList<NotificationRequest>>() {}.type
            gson.fromJson<MutableList<NotificationRequest>>(json, type) ?: mutableListOf()
        } catch (_: Exception) {
            mutableListOf()
        }
    }

    fun savePendingNotifications(items: List<NotificationRequest>) {
        prefs.edit { putString(KEY_PENDING, gson.toJson(items)) }
    }

    fun enqueue(item: NotificationRequest) {
        val items = getPendingNotifications()
        items.add(item)
        savePendingNotifications(items)
    }

    fun clear() {
        prefs.edit { remove(KEY_PENDING) }
    }
}
