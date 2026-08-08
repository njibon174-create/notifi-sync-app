package com.notifsync.app

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.notifsync.app.data.SupabaseRepository
import com.notifsync.app.data.local.NotificationQueueStore
import com.notifsync.app.data.local.SessionStore
import com.notifsync.app.data.local.WhitelistStore
import com.notifsync.app.data.remote.NotificationUploadManager
import com.notifsync.app.data.remote.SupabaseApi

class AppContainer(context: Context) {
    val sessionStore: SessionStore
    val whitelistStore: WhitelistStore
    val notificationQueueStore: NotificationQueueStore
    val api: SupabaseApi
    val repository: SupabaseRepository
    val uploader: NotificationUploadManager

    init {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val prefs = EncryptedSharedPreferences.create(
            context,
            "notification_sync_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        sessionStore = SessionStore(prefs)
        whitelistStore = WhitelistStore(context.applicationContext)
        notificationQueueStore = NotificationQueueStore(context.applicationContext)
        Log.i(
            "SupabaseConfig",
            "Loaded config: url=${BuildConfig.SUPABASE_URL.takeIf { it.isNotBlank() } ?: "<empty>"}, anonKeyPresent=${BuildConfig.SUPABASE_ANON_KEY.isNotBlank()}"
        )
        api = SupabaseApi(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_ANON_KEY)
        repository = SupabaseRepository(api, sessionStore)
        uploader = NotificationUploadManager(repository, sessionStore, notificationQueueStore)
    }
}
