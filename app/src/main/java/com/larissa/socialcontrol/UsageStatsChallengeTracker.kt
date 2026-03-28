package com.larissa.socialcontrol

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import kotlin.math.max
import kotlin.math.min

data class ChallengeProgress(
    val trackedSeconds: Int,
    val requiredSeconds: Int,
    val isComplete: Boolean,
    val hasUsageAccess: Boolean,
)

class UsageStatsChallengeTracker(private val context: Context) {
    fun calculateProgress(session: ChallengeSession, nowEpochMs: Long = System.currentTimeMillis()): ChallengeProgress {
        val hasUsageAccess = hasUsageAccess()
        if (!hasUsageAccess) {
            return ChallengeProgress(
                trackedSeconds = 0,
                requiredSeconds = session.requiredSeconds,
                isComplete = false,
                hasUsageAccess = false,
            )
        }

        val trackedMs = trackedForegroundMs(
            packageName = session.controlPackage,
            fromEpochMs = session.startedAtEpochMs,
            toEpochMs = nowEpochMs,
        )
        val trackedSeconds = (trackedMs / 1_000L).toInt()

        return ChallengeProgress(
            trackedSeconds = trackedSeconds,
            requiredSeconds = session.requiredSeconds,
            isComplete = trackedSeconds >= session.requiredSeconds,
            hasUsageAccess = true,
        )
    }

    fun hasUsageAccess(): Boolean {
        val appOpsManager = context.getSystemService(AppOpsManager::class.java)
        val mode = appOpsManager.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName,
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun trackedForegroundMs(
        packageName: String,
        fromEpochMs: Long,
        toEpochMs: Long,
    ): Long {
        if (toEpochMs <= fromEpochMs) return 0L

        val usageStatsManager = context.getSystemService(UsageStatsManager::class.java)
        val usageEvents = usageStatsManager.queryEvents(fromEpochMs, toEpochMs)
        val event = UsageEvents.Event()

        var activeStart: Long? = null
        var totalMs = 0L

        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            if (event.packageName != packageName) continue

            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED,
                UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                    if (activeStart == null) {
                        activeStart = max(event.timeStamp, fromEpochMs)
                    }
                }

                UsageEvents.Event.ACTIVITY_PAUSED,
                UsageEvents.Event.ACTIVITY_STOPPED,
                UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                    val startedAt = activeStart ?: continue
                    totalMs += max(0L, min(event.timeStamp, toEpochMs) - startedAt)
                    activeStart = null
                }
            }
        }

        if (activeStart != null) {
            totalMs += max(0L, toEpochMs - activeStart)
        }

        return totalMs
    }
}
