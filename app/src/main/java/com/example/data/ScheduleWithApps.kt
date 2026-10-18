package com.example.data

import androidx.room.Embedded
import androidx.room.Relation

data class ScheduleWithApps(
    @Embedded
    val schedule: ScheduleEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "scheduleId"
    )
    val apps: List<ScheduleAppEntity>
)
