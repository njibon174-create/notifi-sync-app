package com.notifsync.app.data.model

import java.time.Instant

data class ApiResult<T>(
    val data: T,
    val serverTime: Instant? = null
)
