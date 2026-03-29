package com.larissa.socialcontrol

data class InterventionRule(
    val ruleId: String,
    val blockedPackage: String,
    val blockedAppName: String,
    val controlPackage: String,
    val controlAppName: String,
    val requiredSeconds: Int,
    val unlockWindowMinutes: Int,
    val isEnabled: Boolean,
    val savedAtEpochMs: Long,
)

data class InterventionRuleDraft(
    val blockedPackage: String,
    val blockedAppName: String,
    val controlPackage: String,
    val controlAppName: String,
    val requiredSeconds: Int,
    val unlockWindowMinutes: Int,
    val isEnabled: Boolean = true,
)

fun InterventionRule.toDraft(): InterventionRuleDraft {
    return InterventionRuleDraft(
        blockedPackage = blockedPackage,
        blockedAppName = blockedAppName,
        controlPackage = controlPackage,
        controlAppName = controlAppName,
        requiredSeconds = requiredSeconds,
        unlockWindowMinutes = unlockWindowMinutes,
        isEnabled = isEnabled,
    )
}

fun isChallengeDurationSupported(requiredSeconds: Int): Boolean {
    return requiredSeconds in RuleLimits.REQUIRED_SECONDS_RANGE
}

fun isUnlockWindowSupported(unlockWindowMinutes: Int): Boolean {
    return unlockWindowMinutes in RuleLimits.UNLOCK_WINDOW_MINUTES_RANGE
}
