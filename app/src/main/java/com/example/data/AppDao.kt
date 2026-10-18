package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Query("SELECT * FROM protected_apps WHERE isProtected = 1 ORDER BY appName ASC")
    fun getAllProtectedApps(): Flow<List<AppEntity>>

    @Query("SELECT * FROM protected_apps ORDER BY appName ASC")
    fun getAllApps(): Flow<List<AppEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApp(app: AppEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApps(apps: List<AppEntity>)

    @Update
    suspend fun updateApp(app: AppEntity)

    @Delete
    suspend fun deleteApp(app: AppEntity)

    @Query("DELETE FROM protected_apps WHERE packageName = :packageName")
    suspend fun deleteByPackageName(packageName: String)

    @Query("DELETE FROM protected_apps")
    suspend fun deleteAllApps()

    @Query("UPDATE protected_apps SET isProtected = :isProtected WHERE packageName = :packageName")
    suspend fun updateProtectedStatus(packageName: String, isProtected: Boolean)

    @Query("SELECT * FROM protected_apps WHERE packageName = :packageName LIMIT 1")
    suspend fun getApp(packageName: String): AppEntity?

    @Query("SELECT COUNT(*) FROM protected_apps WHERE isProtected = 1")
    fun getProtectedCount(): Flow<Int>
}
