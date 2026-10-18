package com.example.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "schedule_apps",
    primaryKeys = ["scheduleId", "packageName"],
    foreignKeys = [
        ForeignKey(
            entity = ScheduleEntity::class,
            parentColumns = ["id"],
            childColumns = ["scheduleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["scheduleId"]), Index(value = ["packageName"])]
)
data class ScheduleAppEntity(
    val scheduleId: Long,
    val packageName: String,
    val appName: String = ""
)
