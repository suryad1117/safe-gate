package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "protected_apps")
data class AppEntity(
    @PrimaryKey
    val packageName: String,
    val appName: String,
    val isProtected: Boolean = true,
    val iconUri: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
