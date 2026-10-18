package com.example.scheduling

import com.example.data.ScheduleEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object ScheduleCalculator {
    private val dayOfWeekFormatter = SimpleDateFormat("EEE", Locale.ENGLISH) // e.g. Mon, Tue
    private val fullDateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val timeFormatter = SimpleDateFormat("hh:mm a", Locale.getDefault())

    /**
     * Inactivity Lock operates as a strict scheduled lock across its active window.
     * While active, no bypass is permitted and the schedule cannot be edited or deleted.
     */
    fun isInactivityTriggered(
        schedule: ScheduleEntity,
        currentTime: Long = System.currentTimeMillis()
    ): Boolean {
        return isStrictInactivityActive(schedule, currentTime)
    }

    /**
     * Checks if this schedule has strict Inactivity Lock enabled and is currently within its active window.
     */
    fun isStrictInactivityActive(
        schedule: ScheduleEntity,
        currentTime: Long = System.currentTimeMillis()
    ): Boolean {
        return schedule.enabled && schedule.inactivityEnabled && isTimeWindowActive(schedule, currentTime)
    }

    fun isTimeWindowActive(
        schedule: ScheduleEntity,
        currentTime: Long = System.currentTimeMillis()
    ): Boolean {
        if (!schedule.enabled) return false
        if (schedule.startDateTime <= 0L || schedule.endDateTime <= 0L) return false

        return when (schedule.repeatType.uppercase(Locale.ROOT)) {
            "ONCE" -> {
                currentTime >= schedule.startDateTime && currentTime < schedule.endDateTime
            }
            "DAILY" -> {
                isCurrentTimeWithinDailyWindow(schedule.startDateTime, schedule.endDateTime, currentTime)
            }
            "WEEKLY", "CUSTOM" -> {
                val now = Calendar.getInstance().apply { timeInMillis = currentTime }
                val currentDay = dayOfWeekFormatter.format(now.time)
                val daysList = if (schedule.customDays.isNotBlank()) {
                    schedule.customDays.split(",").map { it.trim() }
                } else {
                    listOf("Mon", "Tue", "Wed", "Thu", "Fri")
                }
                if (daysList.any { it.equals(currentDay, ignoreCase = true) }) {
                    isCurrentTimeWithinDailyWindow(schedule.startDateTime, schedule.endDateTime, currentTime)
                } else {
                    false
                }
            }
            else -> {
                currentTime >= schedule.startDateTime && currentTime < schedule.endDateTime
            }
        }
    }

    fun isScheduleActiveNow(
        schedule: ScheduleEntity,
        currentTime: Long = System.currentTimeMillis()
    ): Boolean {
        if (!schedule.enabled) return false
        return isTimeWindowActive(schedule, currentTime)
    }

    private fun isCurrentTimeWithinDailyWindow(
        startDateTime: Long,
        endDateTime: Long,
        currentTime: Long
    ): Boolean {
        val startCal = Calendar.getInstance().apply { timeInMillis = startDateTime }
        val endCal = Calendar.getInstance().apply { timeInMillis = endDateTime }
        val nowCal = Calendar.getInstance().apply { timeInMillis = currentTime }

        val startMinutes = startCal.get(Calendar.HOUR_OF_DAY) * 60 + startCal.get(Calendar.MINUTE)
        val endMinutes = endCal.get(Calendar.HOUR_OF_DAY) * 60 + endCal.get(Calendar.MINUTE)
        val currentMinutes = nowCal.get(Calendar.HOUR_OF_DAY) * 60 + nowCal.get(Calendar.MINUTE)

        return if (endMinutes > startMinutes) {
            // e.g. 13:00 to 17:00
            currentMinutes >= startMinutes && currentMinutes < endMinutes
        } else if (endMinutes < startMinutes) {
            // Overnight window e.g. 22:00 to 06:00
            currentMinutes >= startMinutes || currentMinutes < endMinutes
        } else {
            // Same time - active all day if 24h
            true
        }
    }

    fun formatDateTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun formatDate(timestamp: Long): String {
        return fullDateFormatter.format(Date(timestamp))
    }

    fun formatTime(timestamp: Long): String {
        return timeFormatter.format(Date(timestamp))
    }
}
