package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vault_items")
data class VaultItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fileName: String,
    val originalName: String,
    val filePath: String,
    val fileType: String, // "PHOTO" or "FILE"
    val mimeType: String = "",
    val sizeBytes: Long = 0,
    val createdAt: Long = System.currentTimeMillis()
)
