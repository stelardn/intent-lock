package com.larissa.socialcontrol

import android.content.ComponentName
import android.content.Context
import android.provider.Settings

data class PermissionReadiness(
    val accessibilityEnabled: Boolean,
    val usageAccessEnabled: Boolean,
) {
    val isFullyReady: Boolean
        get() = accessibilityEnabled && usageAccessEnabled
}

class PermissionStatusRepository(
    private val context: Context,
    private val usageTracker: UsageStatsChallengeTracker = UsageStatsChallengeTracker(context),
) {
    fun getReadiness(): PermissionReadiness {
        return PermissionReadiness(
            accessibilityEnabled = isAccessibilityEnabled(),
            usageAccessEnabled = usageTracker.hasUsageAccess(),
        )
    }

    fun isAccessibilityEnabled(): Boolean {
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false

        val targetComponent = ComponentName(context, SocialAccessibilityService::class.java)
            .flattenToString()

        return enabledServices
            .split(':')
            .any { it.equals(targetComponent, ignoreCase = true) }
    }

    fun isUsageAccessEnabled(): Boolean = usageTracker.hasUsageAccess()
}
