package com.notifsync.app

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.notifsync.app.data.SupabaseRepository
import com.notifsync.app.data.local.SessionStore
import com.notifsync.app.data.remote.SupabaseApi

class AppContainer(context: Context) {
    val sessionStore: SessionStore
    val api: SupabaseApi
    val repository: SupabaseRepository

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
        api = SupabaseApi(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_ANON_KEY)
        repository = SupabaseRepository(api, sessionStore)
    }
}
