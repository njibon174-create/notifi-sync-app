package com.notifsync.app.data.local

import android.content.SharedPreferences
import com.notifsync.app.data.model.AuthResponse
import com.notifsync.app.data.model.DeviceResponse

class SessionStore(private val prefs: SharedPreferences) {

    private companion object {
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
        const val KEY_EXPIRES_AT = "expires_at"
        const val KEY_EMAIL = "email"
        const val KEY_USER_ID = "user_id"
        const val KEY_DEVICE_ID = "device_id"
        const val KEY_DEVICE_NAME = "device_name"
        const val KEY_DEVICE_MODEL = "device_model"
    }

    fun saveAuth(auth: AuthResponse) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, auth.access_token)
            .putString(KEY_REFRESH_TOKEN, auth.refresh_token)
            .putLong(KEY_EXPIRES_AT, auth.expires_at ?: 0L)
            .putString(KEY_EMAIL, auth.user.email)
            .putString(KEY_USER_ID, auth.user.id)
            .apply()
    }

    fun requireAuth(): StoredAuth {
        val accessToken = getAccessToken() ?: error("Not authenticated")
        val refreshToken = getRefreshToken() ?: error("Not authenticated")
        val email = getEmail() ?: error("Not authenticated")
        val userId = getUserId() ?: error("Not authenticated")
        return StoredAuth(accessToken, refreshToken, email, userId)
    }

    fun saveDevice(device: DeviceResponse) {
        prefs.edit()
            .putString(KEY_DEVICE_ID, device.id)
            .putString(KEY_DEVICE_NAME, device.device_name)
            .putString(KEY_DEVICE_MODEL, device.device_model)
            .apply()
    }

    fun getAccessToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)
    fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH_TOKEN, null)
    fun getExpiresAt(): Long = prefs.getLong(KEY_EXPIRES_AT, 0L)
    fun getEmail(): String? = prefs.getString(KEY_EMAIL, null)
    fun getUserId(): String? = prefs.getString(KEY_USER_ID, null)
    fun getDeviceId(): String? = prefs.getString(KEY_DEVICE_ID, null)
    fun getDeviceName(): String? = prefs.getString(KEY_DEVICE_NAME, null)
    fun getDeviceModel(): String? = prefs.getString(KEY_DEVICE_MODEL, null)

    fun hasRegisteredDevice(): Boolean = !getDeviceId().isNullOrBlank()

    fun isAccessTokenExpired(): Boolean {
        val expiresAt = getExpiresAt()
        if (expiresAt <= 0L) return true
        val nowSeconds = System.currentTimeMillis() / 1000L
        return nowSeconds >= (expiresAt - 60L)
    }

    fun clearAuth() {
        prefs.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_EXPIRES_AT)
            .remove(KEY_EMAIL)
            .remove(KEY_USER_ID)
            .apply()
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }
}

data class StoredAuth(
    val accessToken: String,
    val refreshToken: String,
    val email: String,
    val userId: String
)
