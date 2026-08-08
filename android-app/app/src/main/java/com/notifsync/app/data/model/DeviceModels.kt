package com.notifsync.app.data.model

import com.google.gson.annotations.SerializedName

data class DeviceRequest(
    @SerializedName("device_name") val deviceName: String,
    @SerializedName("device_model") val deviceModel: String,
    @SerializedName("push_token") val pushToken: String? = null,
    @SerializedName("last_active") val lastActive: String
)

data class DeviceResponse(
    val id: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("device_name") val device_name: String,
    @SerializedName("device_model") val device_model: String?,
    @SerializedName("push_token") val push_token: String?,
    @SerializedName("last_active") val last_active: String,
    @SerializedName("created_at") val created_at: String
)

data class NotificationRequest(
    @SerializedName("device_id") val deviceId: String,
    val type: String,
    @SerializedName("app_package_name") val appPackageName: String? = null,
    val sender: String,
    val title: String? = null,
    val body: String,
    @SerializedName("original_timestamp") val originalTimestamp: String,
    @SerializedName("is_read") val isRead: Boolean = false
)

data class NotificationResponse(
    val id: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("device_id") val deviceId: String,
    val type: String,
    @SerializedName("app_package_name") val appPackageName: String?,
    val sender: String,
    val title: String?,
    val body: String,
    @SerializedName("original_timestamp") val originalTimestamp: String,
    @SerializedName("is_read") val isRead: Boolean,
    @SerializedName("created_at") val createdAt: String
)
