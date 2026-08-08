package com.notifsync.app.data.model

import com.google.gson.annotations.SerializedName

data class AuthRequest(
    val email: String,
    val password: String
)

data class AuthResponse(
    @SerializedName("access_token") val access_token: String?,
    @SerializedName("token_type") val token_type: String?,
    @SerializedName("expires_in") val expires_in: Long?,
    @SerializedName("expires_at") val expires_at: Long?,
    @SerializedName("refresh_token") val refresh_token: String?,
    val user: SupabaseUser
)

data class SupabaseUser(
    val id: String,
    val email: String
)
