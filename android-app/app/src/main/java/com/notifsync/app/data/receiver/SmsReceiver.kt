package com.notifsync.app.data.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.notifsync.app.NotificationSyncApplication
import com.notifsync.app.data.model.NotificationRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.Instant

class SmsReceiver : BroadcastReceiver() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val pendingResult = goAsync()
        scope.launch {
            try {
                val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
                if (messages.isNullOrEmpty()) {
                    Log.w(TAG, "No SMS messages found in broadcast")
                    return@launch
                }

                val sender = messages.first().originatingAddress.orEmpty()
                val body = messages.joinToString(separator = "") { it.messageBody.orEmpty() }
                val timestamp = messages.first().timestampMillis
                val request = NotificationRequest(
                    deviceId = "",
                    type = "sms",
                    sender = sender.ifBlank { "Unknown" },
                    body = body,
                    originalTimestamp = Instant.ofEpochMilli(timestamp).toString()
                )
                application(context).uploader.uploadOrQueue(request)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to process incoming SMS", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun application(context: Context): NotificationSyncApplication {
        return context.applicationContext as NotificationSyncApplication
    }

    private companion object {
        const val TAG = "SmsReceiver"
    }
}
