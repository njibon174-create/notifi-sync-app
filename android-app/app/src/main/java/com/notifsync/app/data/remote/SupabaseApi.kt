package com.notifsync.app.data.remote

import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.notifsync.app.data.model.AuthRequest
import com.notifsync.app.data.model.AuthResponse
import com.notifsync.app.data.model.DeviceRequest
import com.notifsync.app.data.model.DeviceResponse
import com.notifsync.app.data.model.NotificationRequest
import com.notifsync.app.data.model.NotificationResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.time.Instant
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

    suspend fun signUp(email: String, password: String): AuthResponse {
        return postJson(
            url = "$authBase/signup",
            body = AuthRequest(email, password),
            includeAuth = false,
            parser = { gson.fromJson(it, AuthResponse::class.java) }
        )
    }

    suspend fun signIn(email: String, password: String): AuthResponse {
        return postJson(
            url = "$authBase/token?grant_type=password",
            body = AuthRequest(email, password),
            includeAuth = false,
            parser = { gson.fromJson(it, AuthResponse::class.java) }
        )
    }

    suspend fun refresh(refreshToken: String): AuthResponse {
        val body = mapOf("refresh_token" to refreshToken)
        return postJson(
            url = "$authBase/token?grant_type=refresh_token",
            body = body,
            includeAuth = false,
            parser = { gson.fromJson(it, AuthResponse::class.java) }
        )
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
        )
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
        )
    }

    private suspend inline fun <T> postJson(
        url: String,
        body: Any,
        accessToken: String? = null,
        includeAuth: Boolean = true,
        crossinline parser: (String) -> T
    ): T = withContext(Dispatchers.IO) {
        val json = gson.toJson(body)
        val requestBody = json.toRequestBody(jsonMediaType)
        val reqBuilder = Request.Builder().url(url)
            .addHeader("apikey", anonKey)
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "return=representation")

        if (includeAuth) {
            val token = accessToken ?: error("Missing access token")
            reqBuilder.addHeader("Authorization", "Bearer $token")
        }

        val request = reqBuilder.post(requestBody).build()
        try {
            val response = client.newCall(request).execute()
            val bodyString = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                Log.e("SupabaseApi", "HTTP ${response.code} for $url: ${bodyString.take(500)}")
                throw SupabaseException(parseError(bodyString))
            }
            parser(bodyString)
        } catch (e: IOException) {
            // Log full exception chain for ConnectException debugging
            var cause: Throwable? = e
            var depth = 0
            while (cause != null && depth < 10) {
                Log.e("SupabaseApi", "Exception chain depth=$depth: ${cause::class.java.name}: ${cause.message}", cause)
                cause = cause.cause
                depth++
            }
            throw e
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
