package com.notifsync.app.data.model

import com.google.gson.annotations.SerializedName

data class RewardOfferResponse(
    val id: String,
    @SerializedName("user_id") val userId: String,
    val label: String,
    val description: String?,
    @SerializedName("coin_cost") val coinCost: Int,
    @SerializedName("is_active") val isActive: Boolean,
    @SerializedName("sort_order") val sortOrder: Int,
    @SerializedName("created_at") val createdAt: String
)

data class RewardOfferRequest(
    @SerializedName("user_id") val userId: String,
    val label: String,
    val description: String?,
    @SerializedName("coin_cost") val coinCost: Int,
    @SerializedName("is_active") val isActive: Boolean = true,
    @SerializedName("sort_order") val sortOrder: Int = 0
)

data class LeaderboardEntryResponse(
    val id: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("display_name") val displayName: String,
    val coins: Int,
    val rank: Int?,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String
)

data class LeaderboardEntryRequest(
    @SerializedName("user_id") val userId: String,
    @SerializedName("display_name") val displayName: String,
    val coins: Int,
    val rank: Int? = null
)

data class SpinStatusResponse(
    val id: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("device_id") val deviceId: String,
    @SerializedName("last_spin_at") val lastSpinAt: String?,
    @SerializedName("is_unlocked") val isUnlocked: Boolean,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String
)

data class SpinStatusRequest(
    @SerializedName("user_id") val userId: String,
    @SerializedName("device_id") val deviceId: String,
    @SerializedName("last_spin_at") val lastSpinAt: String?,
    @SerializedName("is_unlocked") val isUnlocked: Boolean = false
)

data class WalletResponse(
    val id: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("device_id") val deviceId: String,
    val balance: Int,
    @SerializedName("total_earned") val totalEarned: Int,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String
)
