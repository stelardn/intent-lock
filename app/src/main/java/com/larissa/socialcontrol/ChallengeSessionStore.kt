package com.larissa.socialcontrol

import android.content.Context
import androidx.core.content.edit

data class ChallengeSession(
    val ruleId: String,
    val blockedPackage: String,
    val controlPackage: String,
    val requiredSeconds: Int,
    val startedAtEpochMs: Long,
    val completedAtEpochMs: Long? = null,
)

class ChallengeSessionStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun save(session: ChallengeSession) {
        prefs.edit {
            putString(KEY_RULE_ID, session.ruleId)
            putString(KEY_BLOCKED_PACKAGE, session.blockedPackage)
            putString(KEY_CONTROL_PACKAGE, session.controlPackage)
            putInt(KEY_REQUIRED_SECONDS, session.requiredSeconds)
            putLong(KEY_STARTED_AT_EPOCH_MS, session.startedAtEpochMs)
            if (session.completedAtEpochMs != null) {
                putLong(KEY_COMPLETED_AT_EPOCH_MS, session.completedAtEpochMs)
            } else {
                remove(KEY_COMPLETED_AT_EPOCH_MS)
            }
        }
    }

    fun load(): ChallengeSession? {
        val ruleId = prefs.getString(KEY_RULE_ID, null)
        val blockedPackage = prefs.getString(KEY_BLOCKED_PACKAGE, null) ?: return null
        val controlPackage = prefs.getString(KEY_CONTROL_PACKAGE, null) ?: return null
        val requiredSeconds = prefs.getInt(KEY_REQUIRED_SECONDS, -1)
        val startedAt = prefs.getLong(KEY_STARTED_AT_EPOCH_MS, -1L)
        val completedAt = prefs.getLong(KEY_COMPLETED_AT_EPOCH_MS, -1L)

        return persistedChallengeSessionOrNull(
            ruleId = ruleId,
            blockedPackage = blockedPackage,
            controlPackage = controlPackage,
            requiredSeconds = requiredSeconds,
            startedAtEpochMs = startedAt,
            completedAtEpochMs = completedAt.takeIf { it > 0L },
        )
    }

    fun loadForRule(ruleId: String, clearOnMismatch: Boolean = true): ChallengeSession? {
        val session = load() ?: return null
        sessionForRuleOrNull(session, ruleId)?.let {
            return it
        }
        if (clearOnMismatch) {
            clear()
        }
        return null
    }

    fun markCompleted(completedAtEpochMs: Long) {
        val session = load() ?: return
        save(session.copy(completedAtEpochMs = completedAtEpochMs))
    }

    fun clear() {
        prefs.edit { clear() }
    }

    private companion object {
        const val PREFS_NAME = "challenge_session"
        const val KEY_RULE_ID = "rule_id"
        const val KEY_BLOCKED_PACKAGE = "blocked_package"
        const val KEY_CONTROL_PACKAGE = "control_package"
        const val KEY_REQUIRED_SECONDS = "required_seconds"
        const val KEY_STARTED_AT_EPOCH_MS = "started_at_epoch_ms"
        const val KEY_COMPLETED_AT_EPOCH_MS = "completed_at_epoch_ms"
    }
}

internal fun persistedChallengeSessionOrNull(
    ruleId: String?,
    blockedPackage: String?,
    controlPackage: String?,
    requiredSeconds: Int,
    startedAtEpochMs: Long,
    completedAtEpochMs: Long?,
): ChallengeSession? {
    if (ruleId.isNullOrBlank()) return null
    if (blockedPackage.isNullOrBlank()) return null
    if (controlPackage.isNullOrBlank()) return null
    if (requiredSeconds <= 0) return null
    if (startedAtEpochMs <= 0L) return null

    return ChallengeSession(
        ruleId = ruleId,
        blockedPackage = blockedPackage,
        controlPackage = controlPackage,
        requiredSeconds = requiredSeconds,
        startedAtEpochMs = startedAtEpochMs,
        completedAtEpochMs = completedAtEpochMs,
    )
}

internal fun sessionForRuleOrNull(
    session: ChallengeSession?,
    ruleId: String,
): ChallengeSession? {
    return session?.takeIf { it.ruleId == ruleId }
}
