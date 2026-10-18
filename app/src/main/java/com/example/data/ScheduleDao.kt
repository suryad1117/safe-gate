package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleDao {
    @Transaction
    @Query("SELECT * FROM schedules ORDER BY id DESC")
    fun getAllSchedulesWithApps(): Flow<List<ScheduleWithApps>>

    @Query("SELECT * FROM schedules ORDER BY id DESC")
    fun getAllSchedules(): Flow<List<ScheduleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: ScheduleEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScheduleApps(apps: List<ScheduleAppEntity>)

    @Update
    suspend fun updateSchedule(schedule: ScheduleEntity)

    @Delete
    suspend fun deleteSchedule(schedule: ScheduleEntity)

    @Query("DELETE FROM schedules WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM schedule_apps WHERE scheduleId = :scheduleId")
    suspend fun deleteScheduleApps(scheduleId: Long)

    @Query("SELECT * FROM schedules WHERE id = :id")
    suspend fun getScheduleById(id: Long): ScheduleEntity?

    @Query("UPDATE schedules SET lastOpenedTime = :time WHERE id = :id")
    suspend fun updateLastOpenedTime(id: Long, time: Long)

    @Query("UPDATE schedules SET lastOpenedTime = :time WHERE packageName = :packageName OR id IN (SELECT scheduleId FROM schedule_apps WHERE packageName = :packageName)")
    suspend fun updateLastOpenedForPackage(packageName: String, time: Long)

    @Query("SELECT s.* FROM schedules s LEFT JOIN schedule_apps sa ON s.id = sa.scheduleId WHERE s.packageName = :packageName OR sa.packageName = :packageName")
    fun getSchedulesForApp(packageName: String): Flow<List<ScheduleEntity>>

    @Query("SELECT DISTINCT s.* FROM schedules s LEFT JOIN schedule_apps sa ON s.id = sa.scheduleId WHERE s.packageName = :packageName OR sa.packageName = :packageName")
    suspend fun getSchedulesForAppOnce(packageName: String): List<ScheduleEntity>

    @Query("SELECT * FROM schedules WHERE enabled = 1")
    fun getEnabledSchedules(): Flow<List<ScheduleEntity>>

    @Query("UPDATE schedules SET enabled = :enabled WHERE id = :id")
    suspend fun toggleScheduleStatus(id: Long, enabled: Boolean)
}
