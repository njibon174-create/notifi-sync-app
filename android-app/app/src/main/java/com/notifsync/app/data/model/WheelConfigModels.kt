package com.notifsync.app.data.model

import com.google.gson.annotations.SerializedName

data class WheelSegment(
    val label: String,
    val type: String, // "coin" or "gift"
    @SerializedName("coin_value")
    val coinValue: Int = 0
) {
    companion object {
        val DEFAULTS = listOf(
            WheelSegment("0", "coin", 0),
            WheelSegment("2", "coin", 2),
            WheelSegment("4", "coin", 4),
            WheelSegment("📱 Phone", "gift", 0),
            WheelSegment("5", "coin", 5),
            WheelSegment("🎧 Audio", "gift", 0),
            WheelSegment("7", "coin", 7),
            WheelSegment("🎁 Mystery", "gift", 0),
            WheelSegment("9", "coin", 9)
        )
    }
}

data class WheelConfigResponse(
    val id: String,
    @SerializedName("user_id")
    val userId: String,
    val segments: List<WheelSegment>,
    @SerializedName("updated_at")
    val updatedAt: String
)

data class WheelConfigUpsert(
    @SerializedName("user_id")
    val userId: String,
    val segments: List<WheelSegment>
)
