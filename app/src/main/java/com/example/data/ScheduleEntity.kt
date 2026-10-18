package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "schedules")
data class ScheduleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val packageName: String = "",
    val appName: String = "",
    val startDateTime: Long = 0L, // Start timestamp in milliseconds
    val endDateTime: Long = 0L,   // End timestamp in milliseconds
    val repeatType: String = "Once", // "Once", "Daily", "Weekly", "Custom"
    val customDays: String = "", // e.g. "Mon,Tue,Wed"
    val enabled: Boolean = true,
    val inactivityEnabled: Boolean = false,
    val inactivityMinutes: Int = 30, // Inactivity period in minutes before locking
    val lastOpenedTime: Long = 0L,   // Last opened timestamp
    val createdAt: Long = System.currentTimeMillis()
)
