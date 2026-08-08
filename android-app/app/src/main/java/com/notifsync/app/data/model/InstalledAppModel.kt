package com.notifsync.app.data.model

import android.graphics.drawable.Drawable

data class InstalledAppModel(
    val packageName: String,
    val appName: String,
    val icon: Drawable
)
