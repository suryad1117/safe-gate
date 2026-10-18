package com.example.data

import kotlinx.coroutines.flow.Flow

class SafeGateRepository(
    private val database: SafeGateDatabase
) {
    private val appDao = database.appDao()
    private val scheduleDao = database.scheduleDao()
    private val vaultDao = database.vaultDao()

    /* ---------------- APPS ---------------- */
    fun getAllProtectedApps(): Flow<List<AppEntity>> = appDao.getAllProtectedApps()

    fun getAllApps(): Flow<List<AppEntity>> = appDao.getAllApps()

    fun getProtectedCount(): Flow<Int> = appDao.getProtectedCount()

    suspend fun addApp(app: AppEntity) {
        appDao.insertApp(app)
    }

    suspend fun addApps(apps: List<AppEntity>) {
        appDao.insertApps(apps)
    }

    suspend fun replaceAllProtectedApps(apps: List<AppEntity>) {
        appDao.deleteAllApps()
        if (apps.isNotEmpty()) {
            appDao.insertApps(apps)
        }
    }

    suspend fun removeApp(packageName: String) {
        appDao.deleteByPackageName(packageName)
    }

    suspend fun setProtectedStatus(packageName: String, isProtected: Boolean) {
        appDao.updateProtectedStatus(packageName, isProtected)
    }

    suspend fun getApp(packageName: String): AppEntity? {
        return appDao.getApp(packageName)
    }

    /* ---------------- SCHEDULES ---------------- */
    fun getAllSchedulesWithApps(): Flow<List<ScheduleWithApps>> = scheduleDao.getAllSchedulesWithApps()

    fun getAllSchedules(): Flow<List<ScheduleEntity>> = scheduleDao.getAllSchedules()

    fun getEnabledSchedules(): Flow<List<ScheduleEntity>> = scheduleDao.getEnabledSchedules()

    fun getSchedulesForApp(packageName: String): Flow<List<ScheduleEntity>> =
        scheduleDao.getSchedulesForApp(packageName)

    suspend fun getSchedulesForAppOnce(packageName: String): List<ScheduleEntity> =
        scheduleDao.getSchedulesForAppOnce(packageName)

    suspend fun createScheduleWithApps(
        schedule: ScheduleEntity,
        apps: List<ScheduleAppEntity>
    ): Long {
        val scheduleId = scheduleDao.insertSchedule(schedule)
        if (apps.isNotEmpty()) {
            val mappedApps = apps.map { it.copy(scheduleId = scheduleId) }
            scheduleDao.insertScheduleApps(mappedApps)
        }
        return scheduleId
    }

    suspend fun getScheduleById(id: Long): ScheduleEntity? = scheduleDao.getScheduleById(id)

    suspend fun updateSchedule(schedule: ScheduleEntity) {
        scheduleDao.updateSchedule(schedule)
    }

    suspend fun updateScheduleWithApps(
        schedule: ScheduleEntity,
        apps: List<ScheduleAppEntity>
    ) {
        scheduleDao.updateSchedule(schedule)
        scheduleDao.deleteScheduleApps(schedule.id)
        if (apps.isNotEmpty()) {
            val mappedApps = apps.map { it.copy(scheduleId = schedule.id) }
            scheduleDao.insertScheduleApps(mappedApps)
        }
    }

    suspend fun updateLastOpenedForPackage(packageName: String, time: Long = System.currentTimeMillis()) {
        scheduleDao.updateLastOpenedForPackage(packageName, time)
    }

    suspend fun deleteSchedule(schedule: ScheduleEntity) {
        scheduleDao.deleteSchedule(schedule)
    }

    suspend fun deleteScheduleById(id: Long) {
        scheduleDao.deleteScheduleApps(id)
        scheduleDao.deleteById(id)
    }

    suspend fun toggleSchedule(id: Long, enabled: Boolean) {
        scheduleDao.toggleScheduleStatus(id, enabled)
    }

    /* ---------------- PRIVATE VAULT ---------------- */
    fun getAllVaultItems(): Flow<List<VaultItemEntity>> = vaultDao.getAllVaultItems()

    fun getPhotos(): Flow<List<VaultItemEntity>> = vaultDao.getVaultItemsByType("PHOTO")

    fun getFiles(): Flow<List<VaultItemEntity>> = vaultDao.getVaultItemsByType("FILE")

    fun getPhotosCount(): Flow<Int> = vaultDao.getCountByType("PHOTO")

    fun getFilesCount(): Flow<Int> = vaultDao.getCountByType("FILE")

    suspend fun addVaultItem(item: VaultItemEntity): Long = vaultDao.insertVaultItem(item)

    suspend fun addVaultItems(items: List<VaultItemEntity>) = vaultDao.insertVaultItems(items)

    suspend fun deleteVaultItem(item: VaultItemEntity) = vaultDao.deleteVaultItem(item)

    suspend fun deleteVaultItemById(id: Long) = vaultDao.deleteVaultItemById(id)
}
