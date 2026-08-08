package com.notifsync.app.data

import com.notifsync.app.data.local.SessionStore
import com.notifsync.app.data.model.AuthResponse
import com.notifsync.app.data.model.DeviceResponse
import com.notifsync.app.data.remote.SupabaseApi

class SupabaseRepository(
    private val api: SupabaseApi,
    private val sessionStore: SessionStore
) {
    suspend fun signUp(email: String, password: String): AuthResponse = api.signUp(email, password)
    suspend fun signIn(email: String, password: String): AuthResponse = api.signIn(email, password)
    suspend fun refreshSession(refreshToken: String): AuthResponse = api.refresh(refreshToken)
    suspend fun registerDevice(accessToken: String, deviceName: String, deviceModel: String): DeviceResponse =
        api.registerDevice(accessToken, deviceName, deviceModel)
}
