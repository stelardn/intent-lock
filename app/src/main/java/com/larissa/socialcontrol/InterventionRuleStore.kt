package com.larissa.socialcontrol

import android.content.Context
import androidx.core.content.edit
import java.util.UUID

class InterventionRuleStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun save(
        draft: InterventionRuleDraft,
        nowEpochMs: Long = System.currentTimeMillis(),
    ): InterventionRule {
        require(draft.blockedPackage.isNotBlank()) { "blockedPackage is required" }
        require(draft.blockedAppName.isNotBlank()) { "blockedAppName is required" }
        require(draft.controlPackage.isNotBlank()) { "controlPackage is required" }
        require(draft.controlAppName.isNotBlank()) { "controlAppName is required" }
        require(isChallengeDurationSupported(draft.requiredSeconds)) {
            "requiredSeconds must be within ${RuleLimits.REQUIRED_SECONDS_RANGE}"
        }
        require(isUnlockWindowSupported(draft.unlockWindowMinutes)) {
            "unlockWindowMinutes must be within ${RuleLimits.UNLOCK_WINDOW_MINUTES_RANGE}"
        }

        val rule = InterventionRule(
            ruleId = UUID.randomUUID().toString(),
            blockedPackage = draft.blockedPackage,
            blockedAppName = draft.blockedAppName,
            controlPackage = draft.controlPackage,
            controlAppName = draft.controlAppName,
            requiredSeconds = draft.requiredSeconds,
            unlockWindowMinutes = draft.unlockWindowMinutes,
            savedAtEpochMs = nowEpochMs,
        )

        prefs.edit {
            putString(KEY_RULE_ID, rule.ruleId)
            putString(KEY_BLOCKED_PACKAGE, rule.blockedPackage)
            putString(KEY_BLOCKED_APP_NAME, rule.blockedAppName)
            putString(KEY_CONTROL_PACKAGE, rule.controlPackage)
            putString(KEY_CONTROL_APP_NAME, rule.controlAppName)
            putInt(KEY_REQUIRED_SECONDS, rule.requiredSeconds)
            putInt(KEY_UNLOCK_WINDOW_MINUTES, rule.unlockWindowMinutes)
            putLong(KEY_SAVED_AT_EPOCH_MS, rule.savedAtEpochMs)
        }

        return rule
    }

    fun load(): InterventionRule? {
        return StoredInterventionRuleMapper.fromPersistedValues(
            ruleId = prefs.getString(KEY_RULE_ID, null),
            blockedPackage = prefs.getString(KEY_BLOCKED_PACKAGE, null),
            blockedAppName = prefs.getString(KEY_BLOCKED_APP_NAME, null),
            controlPackage = prefs.getString(KEY_CONTROL_PACKAGE, null),
            controlAppName = prefs.getString(KEY_CONTROL_APP_NAME, null),
            requiredSeconds = prefs.getInt(KEY_REQUIRED_SECONDS, -1),
            unlockWindowMinutes = prefs.getInt(KEY_UNLOCK_WINDOW_MINUTES, -1),
            savedAtEpochMs = prefs.getLong(KEY_SAVED_AT_EPOCH_MS, -1L),
        )
    }

    fun clear() {
        prefs.edit { clear() }
    }

    private companion object {
        const val PREFS_NAME = "intervention_rule"
        const val KEY_RULE_ID = "rule_id"
        const val KEY_BLOCKED_PACKAGE = "blocked_package"
        const val KEY_BLOCKED_APP_NAME = "blocked_app_name"
        const val KEY_CONTROL_PACKAGE = "control_package"
        const val KEY_CONTROL_APP_NAME = "control_app_name"
        const val KEY_REQUIRED_SECONDS = "required_seconds"
        const val KEY_UNLOCK_WINDOW_MINUTES = "unlock_window_minutes"
        const val KEY_SAVED_AT_EPOCH_MS = "saved_at_epoch_ms"
    }
}

internal object StoredInterventionRuleMapper {
    fun fromPersistedValues(
        ruleId: String?,
        blockedPackage: String?,
        blockedAppName: String?,
        controlPackage: String?,
        controlAppName: String?,
        requiredSeconds: Int,
        unlockWindowMinutes: Int,
        savedAtEpochMs: Long,
    ): InterventionRule? {
        if (ruleId.isNullOrBlank()) return null
        if (blockedPackage.isNullOrBlank()) return null
        if (blockedAppName.isNullOrBlank()) return null
        if (controlPackage.isNullOrBlank()) return null
        if (controlAppName.isNullOrBlank()) return null
        if (!isChallengeDurationSupported(requiredSeconds)) return null
        if (!isUnlockWindowSupported(unlockWindowMinutes)) return null
        if (savedAtEpochMs <= 0L) return null

        return InterventionRule(
            ruleId = ruleId,
            blockedPackage = blockedPackage,
            blockedAppName = blockedAppName,
            controlPackage = controlPackage,
            controlAppName = controlAppName,
            requiredSeconds = requiredSeconds,
            unlockWindowMinutes = unlockWindowMinutes,
            savedAtEpochMs = savedAtEpochMs,
        )
    }
}
