package com.notifsync.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.whitelistDataStore by preferencesDataStore(name = "notification_whitelist")

class WhitelistStore(private val context: Context) {
    private companion object {
        val KEY_WHITELIST = stringSetPreferencesKey("selected_packages")
    }

    suspend fun getWhitelistedPackages(): Set<String> {
        val prefs = context.whitelistDataStore.data.first()
        return prefs[KEY_WHITELIST] ?: emptySet()
    }

    suspend fun saveWhitelistedPackages(packages: Set<String>) {
        context.whitelistDataStore.edit { prefs ->
            prefs[KEY_WHITELIST] = packages
        }
    }

    suspend fun isWhitelisted(packageName: String): Boolean {
        return getWhitelistedPackages().contains(packageName)
    }
}
