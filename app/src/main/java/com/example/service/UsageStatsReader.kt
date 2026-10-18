package com.example.service

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context

object UsageStatsReader {

    fun getForegroundPackage(context: Context): String? {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return null

        val end = System.currentTimeMillis()
        val start = end - 10_000L

        var foregroundPackage: String? = null
        var latestTime = 0L

        try {
            val events = usageStatsManager.queryEvents(start, end)
            val event = UsageEvents.Event()

            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                    if (event.timeStamp > latestTime) {
                        latestTime = event.timeStamp
                        foregroundPackage = event.packageName
                    }
                }
            }
        } catch (e: Exception) {
            // queryEvents may throw security or state exception if permission revoked
        }

        // Fallback: if queryEvents was empty, check queryUsageStats
        if (foregroundPackage == null) {
            try {
                val stats = usageStatsManager.queryUsageStats(
                    UsageStatsManager.INTERVAL_DAILY,
                    start,
                    end
                )
                val mostRecent = stats?.maxByOrNull { it.lastTimeUsed }
                if (mostRecent != null && mostRecent.lastTimeUsed > 0) {
                    foregroundPackage = mostRecent.packageName
                }
            } catch (e: Exception) {
                // Ignore fallback exceptions
            }
        }

        return foregroundPackage
    }
}
