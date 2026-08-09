package com.notifsync.app.data

import com.notifsync.app.data.local.SessionStore
import com.notifsync.app.data.model.AuthResponse
import com.notifsync.app.data.model.DeviceResponse
import com.notifsync.app.data.model.LeaderboardEntryRequest
import com.notifsync.app.data.model.LeaderboardEntryResponse
import com.notifsync.app.data.model.NotificationRequest
import com.notifsync.app.data.model.NotificationResponse
import com.notifsync.app.data.model.RewardOfferRequest
import com.notifsync.app.data.model.RewardOfferResponse
import com.notifsync.app.data.model.SpinStatusRequest
import com.notifsync.app.data.model.SpinStatusResponse
import com.notifsync.app.data.remote.SupabaseApi
import java.time.Instant

class SupabaseRepository(
    private val api: SupabaseApi,
    private val sessionStore: SessionStore
) {
    suspend fun signUp(email: String, password: String): AuthResponse = api.signUp(email, password)
    suspend fun signIn(email: String, password: String): AuthResponse = api.signIn(email, password)
    suspend fun refreshSession(refreshToken: String): AuthResponse = api.refresh(refreshToken)
    suspend fun registerDevice(accessToken: String, deviceName: String, deviceModel: String): DeviceResponse =
        api.registerDevice(accessToken, deviceName, deviceModel)
    suspend fun insertNotification(accessToken: String, request: NotificationRequest): NotificationResponse =
        api.insertNotification(accessToken, request)

    suspend fun fetchRewardOffers(accessToken: String): List<RewardOfferResponse> = api.fetchRewardOffers(accessToken).data
    suspend fun insertRewardOffers(accessToken: String, offers: List<RewardOfferRequest>): List<RewardOfferResponse> =
        api.insertRewardOffers(accessToken, offers)

    suspend fun fetchLeaderboard(accessToken: String): List<LeaderboardEntryResponse> = api.fetchLeaderboard(accessToken).data
    suspend fun insertLeaderboard(accessToken: String, entries: List<LeaderboardEntryRequest>): List<LeaderboardEntryResponse> =
        api.insertLeaderboard(accessToken, entries)

    suspend fun fetchSpinStatus(accessToken: String, deviceId: String): Pair<SpinStatusResponse?, Instant?> {
        val result = api.fetchSpinStatus(accessToken, deviceId)
        return result.data to result.serverTime
    }

    suspend fun saveSpinStatus(accessToken: String, request: SpinStatusRequest): SpinStatusResponse =
        api.upsertSpinStatus(accessToken, request)

    suspend fun fetchServerNow(accessToken: String): Instant? = api.fetchServerNow(accessToken)

    suspend fun updateLastActive(accessToken: String, deviceId: String) =
        api.updateLastActive(accessToken, deviceId)
}
