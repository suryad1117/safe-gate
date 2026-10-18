package com.example.ui

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppEntity
import com.example.data.SafeGateRepository
import com.example.data.ScheduleAppEntity
import com.example.data.ScheduleEntity
import com.example.data.ScheduleWithApps
import com.example.data.VaultItemEntity
import com.example.scheduling.ScheduleCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

enum class LockType {
    DIRECT,
    SCHEDULED
}

data class ActiveLockItem(
    val packageName: String,
    val appName: String,
    val lockType: LockType,
    val subtitle: String,
    val schedule: ScheduleEntity? = null
)

class SafeGateViewModel(
    private val repository: SafeGateRepository
) : ViewModel() {

    val protectedApps: StateFlow<List<AppEntity>> =
        repository.getAllProtectedApps()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    val schedulesWithApps: StateFlow<List<ScheduleWithApps>> =
        repository.getAllSchedulesWithApps()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    // ACTIVE LOCKS = Directly Protected Apps + Apps currently inside Active Schedules (start <= now < end)
    val activeLocks: StateFlow<List<ActiveLockItem>> =
        combine(protectedApps, schedulesWithApps) { apps, scheduleList ->
            val result = mutableListOf<ActiveLockItem>()
            val now = System.currentTimeMillis()

            // 1. Apps with currently active schedules (START <= now < END)
            for (item in scheduleList) {
                if (item.schedule.enabled && ScheduleCalculator.isScheduleActiveNow(item.schedule, now)) {
                    for (app in item.apps) {
                        if (result.none { it.packageName == app.packageName }) {
                            val startFormatted = ScheduleCalculator.formatTime(item.schedule.startDateTime)
                            val endFormatted = ScheduleCalculator.formatTime(item.schedule.endDateTime)
                            result.add(
                                ActiveLockItem(
                                    packageName = app.packageName,
                                    appName = app.appName.ifBlank { app.packageName },
                                    lockType = LockType.SCHEDULED,
                                    subtitle = "Locked until ${ScheduleCalculator.formatDateTime(item.schedule.endDateTime)}",
                                    schedule = item.schedule
                                )
                            )
                        }
                    }
                }
            }

            // 2. Directly protected apps (if not already scheduled active)
            for (app in apps.filter { it.isProtected }) {
                if (result.none { it.packageName == app.packageName }) {
                    result.add(
                        ActiveLockItem(
                            packageName = app.packageName,
                            appName = app.appName,
                            lockType = LockType.DIRECT,
                            subtitle = "Always Locked",
                            schedule = null
                        )
                    )
                }
            }

            result
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val activeLocksCount: StateFlow<Int> =
        activeLocks.map { it.size }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = 0
            )

    val protectedAppsCount: StateFlow<Int> =
        protectedApps.map { it.size }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = 0
            )

    /* ---------------- PRIVATE VAULT ---------------- */
    val photosList: StateFlow<List<VaultItemEntity>> =
        repository.getPhotos()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    val filesList: StateFlow<List<VaultItemEntity>> =
        repository.getFiles()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    val photosCount: StateFlow<Int> =
        repository.getPhotosCount()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = 0
            )

    val filesCount: StateFlow<Int> =
        repository.getFilesCount()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = 0
            )

    /* ---------------- APPS MANAGEMENT ---------------- */
    fun saveProtectedApps(apps: List<AppEntity>) {
        viewModelScope.launch {
            repository.replaceAllProtectedApps(apps)
        }
    }

    fun addApp(appName: String, packageName: String) {
        viewModelScope.launch {
            repository.addApp(
                AppEntity(
                    packageName = packageName,
                    appName = appName,
                    isProtected = true
                )
            )
        }
    }

    fun removeApp(packageName: String) {
        viewModelScope.launch {
            repository.removeApp(packageName)
        }
    }

    /* ---------------- SCHEDULES MANAGEMENT ---------------- */
    fun createMultiAppSchedule(
        selectedApps: List<Pair<String, String>>, // Pair(packageName, appName)
        startDateTime: Long,
        endDateTime: Long,
        repeatType: String = "Once",
        customDays: String = "",
        enabled: Boolean = true,
        inactivityEnabled: Boolean = false,
        inactivityMinutes: Int = 30
    ) {
        viewModelScope.launch {
            val primaryApp = selectedApps.firstOrNull()
            val schedule = ScheduleEntity(
                packageName = primaryApp?.first ?: "",
                appName = primaryApp?.second ?: "",
                startDateTime = startDateTime,
                endDateTime = endDateTime,
                repeatType = repeatType,
                customDays = customDays,
                enabled = enabled,
                inactivityEnabled = inactivityEnabled,
                inactivityMinutes = inactivityMinutes,
                createdAt = System.currentTimeMillis()
            )
            val apps = selectedApps.map { (pkg, name) ->
                ScheduleAppEntity(
                    scheduleId = 0,
                    packageName = pkg,
                    appName = name
                )
            }
            repository.createScheduleWithApps(schedule, apps)
        }
    }

    fun updateMultiAppSchedule(
        scheduleId: Long,
        selectedApps: List<Pair<String, String>>,
        startDateTime: Long,
        endDateTime: Long,
        repeatType: String = "Once",
        customDays: String = "",
        enabled: Boolean = true,
        inactivityEnabled: Boolean = false,
        inactivityMinutes: Int = 0
    ) {
        viewModelScope.launch {
            val existing = repository.getScheduleById(scheduleId)
            // Immutable while strict inactivity lock is active!
            if (existing != null && ScheduleCalculator.isStrictInactivityActive(existing)) {
                return@launch
            }
            val primaryApp = selectedApps.firstOrNull()
            val schedule = ScheduleEntity(
                id = scheduleId,
                packageName = primaryApp?.first ?: existing?.packageName ?: "",
                appName = primaryApp?.second ?: existing?.appName ?: "",
                startDateTime = startDateTime,
                endDateTime = endDateTime,
                repeatType = repeatType,
                customDays = customDays,
                enabled = enabled,
                inactivityEnabled = inactivityEnabled,
                inactivityMinutes = inactivityMinutes,
                lastOpenedTime = existing?.lastOpenedTime ?: 0L,
                createdAt = existing?.createdAt ?: System.currentTimeMillis()
            )
            val apps = selectedApps.map { (pkg, name) ->
                ScheduleAppEntity(
                    scheduleId = scheduleId,
                    packageName = pkg,
                    appName = name
                )
            }
            repository.updateScheduleWithApps(schedule, apps)
        }
    }

    fun toggleSchedule(id: Long, enabled: Boolean) {
        viewModelScope.launch {
            val existing = repository.getScheduleById(id)
            // Cannot be turned off while strict inactivity lock is active!
            if (existing != null && !enabled && ScheduleCalculator.isStrictInactivityActive(existing)) {
                return@launch
            }
            repository.toggleSchedule(id, enabled)
        }
    }

    fun deleteScheduleById(id: Long) {
        viewModelScope.launch {
            val existing = repository.getScheduleById(id)
            // Cannot be deleted while strict inactivity lock is active!
            if (existing != null && ScheduleCalculator.isStrictInactivityActive(existing)) {
                return@launch
            }
            repository.deleteScheduleById(id)
        }
    }

    /* ---------------- VAULT OPERATIONS ---------------- */
    fun addVaultPhoto(file: File, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            if (file.exists() && file.length() > 0) {
                val item = VaultItemEntity(
                    fileName = file.name,
                    originalName = "Camera_${file.name}",
                    filePath = file.absolutePath,
                    fileType = "PHOTO",
                    mimeType = "image/jpeg",
                    sizeBytes = file.length()
                )
                repository.addVaultItem(item)
            }
            onComplete()
        }
    }
    fun importPhotos(context: Context, uris: List<Uri>, onComplete: (Int) -> Unit = {}) {
        viewModelScope.launch {
            val count = withContext(Dispatchers.IO) {
                val vaultDir = File(context.filesDir, "vault/photos").apply { mkdirs() }
                val imported = mutableListOf<VaultItemEntity>()

                for (uri in uris) {
                    try {
                        val originalName = queryFileName(context, uri) ?: "Photo_${System.currentTimeMillis()}.jpg"
                        val ext = originalName.substringAfterLast('.', "jpg")
                        val uniqueName = "photo_${UUID.randomUUID()}.$ext"
                        val destFile = File(vaultDir, uniqueName)

                        context.contentResolver.openInputStream(uri)?.use { input ->
                            FileOutputStream(destFile).use { output ->
                                input.copyTo(output)
                            }
                        }

                        if (destFile.exists() && destFile.length() > 0) {
                            imported.add(
                                VaultItemEntity(
                                    fileName = uniqueName,
                                    originalName = originalName,
                                    filePath = destFile.absolutePath,
                                    fileType = "PHOTO",
                                    mimeType = context.contentResolver.getType(uri) ?: "image/*",
                                    sizeBytes = destFile.length()
                                )
                            )
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                if (imported.isNotEmpty()) {
                    repository.addVaultItems(imported)
                }
                imported.size
            }
            onComplete(count)
        }
    }

    fun importFiles(context: Context, uris: List<Uri>, onComplete: (Int) -> Unit = {}) {
        viewModelScope.launch {
            val count = withContext(Dispatchers.IO) {
                val vaultDir = File(context.filesDir, "vault/files").apply { mkdirs() }
                val imported = mutableListOf<VaultItemEntity>()

                for (uri in uris) {
                    try {
                        val originalName = queryFileName(context, uri) ?: "Document_${System.currentTimeMillis()}"
                        val ext = originalName.substringAfterLast('.', "bin")
                        val uniqueName = "file_${UUID.randomUUID()}.$ext"
                        val destFile = File(vaultDir, uniqueName)

                        context.contentResolver.openInputStream(uri)?.use { input ->
                            FileOutputStream(destFile).use { output ->
                                input.copyTo(output)
                            }
                        }

                        if (destFile.exists() && destFile.length() > 0) {
                            imported.add(
                                VaultItemEntity(
                                    fileName = uniqueName,
                                    originalName = originalName,
                                    filePath = destFile.absolutePath,
                                    fileType = "FILE",
                                    mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream",
                                    sizeBytes = destFile.length()
                                )
                            )
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                if (imported.isNotEmpty()) {
                    repository.addVaultItems(imported)
                }
                imported.size
            }
            onComplete(count)
        }
    }

    fun deleteVaultItem(item: VaultItemEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = File(item.filePath)
                if (file.exists()) {
                    file.delete()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            repository.deleteVaultItem(item)
        }
    }

    private fun queryFileName(context: Context, uri: Uri): String? {
        if (uri.scheme == "content") {
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) {
                            return cursor.getString(nameIndex)
                        }
                    }
                }
            } catch (e: Exception) {
                // fallback
            }
        }
        return uri.lastPathSegment
    }
}
