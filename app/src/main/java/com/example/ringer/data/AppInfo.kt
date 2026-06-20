package com.example.ringer.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "locked_apps")
data class AppInfo(
    @PrimaryKey
    val packageName: String,
    val appName: String,
    val isLocked: Boolean = true
)