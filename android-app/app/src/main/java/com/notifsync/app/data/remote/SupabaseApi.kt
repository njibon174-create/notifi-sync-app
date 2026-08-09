package com.notifsync.app.data.remote

import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.notifsync.app.data.model.ApiResult
import com.notifsync.app.data.model.AuthRequest
import com.notifsync.app.data.model.AuthResponse
import com.notifsync.app.data.model.DeviceRequest
import com.notifsync.app.data.model.DeviceResponse
import com.notifsync.app.data.model.LeaderboardEntryRequest
import com.notifsync.app.data.model.LeaderboardEntryResponse
import com.notifsync.app.data.model.NotificationRequest
import com.notifsync.app.data.model.NotificationResponse
import com.notifsync.app.data.model.RewardOfferRequest
import com.notifsync.app.data.model.RewardOfferResponse
import com.notifsync.app.data.model.SpinStatusRequest
import com.notifsync.app.data.model.SpinStatusResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

class SupabaseApi(
    private val supabaseUrl: String,
    private val anonKey: String
) {
    init {
        Log.i(
            "SupabaseApi",
            "Initialized: url=${supabaseUrl.takeIf { it.isNotBlank() } ?: "<empty>"}, anonKeyPresent=${anonKey.isNotBlank()}"
        )
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson: Gson = GsonBuilder().create()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val authBase = "$supabaseUrl/auth/v1"
    private val restBase = "$supabaseUrl/rest/v1"
    private val rpcBase = "$supabaseUrl/rest/v1/rpc"

    suspend fun signUp(email: String, password: String): AuthResponse {
        return postJson(
            url = "$authBase/signup",
            body = AuthRequest(email, password),
            includeAuth = false,
            parser = { gson.fromJson(it, AuthResponse::class.java) }
        ).data
    }

    suspend fun signIn(email: String, password: String): AuthResponse {
        return postJson(
            url = "$authBase/token?grant_type=password",
            body = AuthRequest(email, password),
            includeAuth = false,
            parser = { gson.fromJson(it, AuthResponse::class.java) }
        ).data
    }

    suspend fun refresh(refreshToken: String): AuthResponse {
        val body = mapOf("refresh_token" to refreshToken)
        return postJson(
            url = "$authBase/token?grant_type=refresh_token",
            body = body,
            includeAuth = false,
            parser = { gson.fromJson(it, AuthResponse::class.java) }
        ).data
    }

    suspend fun registerDevice(accessToken: String, deviceName: String, deviceModel: String): DeviceResponse {
        val requestBody = DeviceRequest(
            deviceName = deviceName,
            deviceModel = deviceModel,
            lastActive = Instant.now().toString()
        )
        return postJson(
            url = "$restBase/devices",
            body = requestBody,
            accessToken = accessToken,
            parser = {
                val type = object : TypeToken<List<DeviceResponse>>() {}.type
                gson.fromJson<List<DeviceResponse>>(it, type).firstOrNull()
                    ?: error("Device insert returned no rows")
            }
        ).data
    }

    suspend fun insertNotification(accessToken: String, request: NotificationRequest): NotificationResponse {
        return postJson(
            url = "$restBase/notifications",
            body = request,
            accessToken = accessToken,
            parser = {
                val type = object : TypeToken<List<NotificationResponse>>() {}.type
                gson.fromJson<List<NotificationResponse>>(it, type).firstOrNull()
                    ?: error("Notification insert returned no rows")
            }
        ).data
    }

    suspend fun fetchRewardOffers(accessToken: String): ApiResult<List<RewardOfferResponse>> {
        return getJson(
            url = "$restBase/reward_offers?select=*&is_active=eq.true&order=sort_order.asc",
            accessToken = accessToken,
            parser = {
                val type = object : TypeToken<List<RewardOfferResponse>>() {}.type
                gson.fromJson<List<RewardOfferResponse>>(it, type)
            }
        )
    }

    suspend fun insertRewardOffers(accessToken: String, offers: List<RewardOfferRequest>): List<RewardOfferResponse> {
        return postJson(
            url = "$restBase/reward_offers",
            body = offers,
            accessToken = accessToken,
            parser = {
                val type = object : TypeToken<List<RewardOfferResponse>>() {}.type
                gson.fromJson<List<RewardOfferResponse>>(it, type)
            }
        ).data
    }

    suspend fun fetchLeaderboard(accessToken: String): ApiResult<List<LeaderboardEntryResponse>> {
        return getJson(
            url = "$restBase/leaderboard?select=*&order=coins.desc",
            accessToken = accessToken,
            parser = {
                val type = object : TypeToken<List<LeaderboardEntryResponse>>() {}.type
                gson.fromJson<List<LeaderboardEntryResponse>>(it, type)
            }
        )
    }

    suspend fun insertLeaderboard(accessToken: String, entries: List<LeaderboardEntryRequest>): List<LeaderboardEntryResponse> {
        return postJson(
            url = "$restBase/leaderboard",
            body = entries,
            accessToken = accessToken,
            parser = {
                val type = object : TypeToken<List<LeaderboardEntryResponse>>() {}.type
                gson.fromJson<List<LeaderboardEntryResponse>>(it, type)
            }
        ).data
    }

    suspend fun fetchSpinStatus(accessToken: String, deviceId: String): ApiResult<SpinStatusResponse?> {
        return getJson(
            url = "$restBase/spin_status?select=*&device_id=eq.$deviceId",
            accessToken = accessToken,
            parser = {
                val type = object : TypeToken<List<SpinStatusResponse>>() {}.type
                gson.fromJson<List<SpinStatusResponse>>(it, type).firstOrNull()
            }
        )
    }

    suspend fun upsertSpinStatus(accessToken: String, request: SpinStatusRequest): SpinStatusResponse {
        return postJson(
            url = "$restBase/spin_status?on_conflict=device_id",
            body = request,
            accessToken = accessToken,
            parser = {
                val type = object : TypeToken<List<SpinStatusResponse>>() {}.type
                gson.fromJson<List<SpinStatusResponse>>(it, type).firstOrNull()
                    ?: error("Spin status upsert returned no rows")
            }
        ).data
    }

    suspend fun fetchServerNow(accessToken: String): Instant? {
        return postJson(
            url = "$rpcBase/server_now",
            body = emptyMap<String, String>(),
            accessToken = accessToken,
            parser = { body -> body }
        ).serverTime
    }

    private suspend inline fun <T> getJson(
        url: String,
        accessToken: String? = null,
        crossinline parser: (String) -> T
    ): ApiResult<T> = withContext(Dispatchers.IO) {
        val reqBuilder = Request.Builder().url(url)
            .addHeader("apikey", anonKey)
            .addHeader("Content-Type", "application/json")
        if (!accessToken.isNullOrBlank()) {
            reqBuilder.addHeader("Authorization", "Bearer $accessToken")
        }
        val request = reqBuilder.build()

        executeRequest(request, parser)
    }

    private suspend inline fun <T> postJson(
        url: String,
        body: Any,
        accessToken: String? = null,
        includeAuth: Boolean = true,
        crossinline parser: (String) -> T
    ): ApiResult<T> = withContext(Dispatchers.IO) {
        val json = gson.toJson(body)
        val requestBody = json.toRequestBody(jsonMediaType)
        val reqBuilder = Request.Builder().url(url)
            .addHeader("apikey", anonKey)
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "return=representation,resolution=merge-duplicates")

        if (includeAuth) {
            val token = accessToken ?: error("Missing access token")
            reqBuilder.addHeader("Authorization", "Bearer $token")
        }

        val request = reqBuilder.post(requestBody).build()
        executeRequest(request, parser)
    }

    private suspend inline fun <T> executeRequest(
        request: Request,
        crossinline parser: (String) -> T
    ): ApiResult<T> {
        val response = client.newCall(request).execute()
        val bodyString = response.body?.string().orEmpty()
        if (!response.isSuccessful) {
            Log.e("SupabaseApi", "HTTP ${response.code} for ${request.url}: ${bodyString.take(500)}")
            throw SupabaseException(parseError(bodyString))
        }
        return ApiResult(
            data = parser(bodyString),
            serverTime = parseServerTime(response.header("Date"))
        )
    }

    private fun parseServerTime(header: String?): Instant? {
        if (header.isNullOrBlank()) return null
        return try {
            ZonedDateTime.parse(header, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant()
        } catch (_: Exception) {
            null
        }
    }

    private fun parseError(body: String): String {
        return try {
            val obj = JSONObject(body)
            obj.optString("msg")
                .ifBlank { obj.optString("message") }
                .ifBlank { obj.optString("error_description") }
                .ifBlank { body }
        } catch (_: Exception) {
            body
        }
    }
}

class SupabaseException(message: String) : IOException(message)
