package com.example.ui

import androidx.lifecycle.ViewModel
import com.example.data.AppEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ScheduleAppSelectionItem(
    val packageName: String,
    val appName: String
)

class SelectedAppState : ViewModel() {
    // For direct protection selection
    private val _selectedApps = MutableStateFlow<List<AppEntity>>(emptyList())
    val selectedApps: StateFlow<List<AppEntity>> = _selectedApps.asStateFlow()

    // For schedule multi-app selection (starts empty)
    private val _scheduleApps = MutableStateFlow<List<ScheduleAppSelectionItem>>(emptyList())
    val scheduleApps: StateFlow<List<ScheduleAppSelectionItem>> = _scheduleApps.asStateFlow()

    fun setSelectedApps(apps: List<AppEntity>) {
        _selectedApps.value = apps
    }

    fun clearSelection() {
        _selectedApps.value = emptyList()
    }

    fun setScheduleApps(apps: List<ScheduleAppSelectionItem>) {
        _scheduleApps.value = apps
    }

    fun clearScheduleApps() {
        _scheduleApps.value = emptyList()
    }
}
