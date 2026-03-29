package com.larissa.socialcontrol

import android.content.Context
import androidx.core.content.edit

data class UnlockGrant(
    val ruleId: String,
    val blockedPackage: String,
    val expiresAtEpochMs: Long,
)

data class ExpiredUnlockCredit(
    val ruleId: String,
    val blockedPackage: String,
    val expiredAtEpochMs: Long,
)

class UnlockGrantStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun save(grant: UnlockGrant) {
        prefs.edit {
            putString(KEY_RULE_ID, grant.ruleId)
            putString(KEY_BLOCKED_PACKAGE, grant.blockedPackage)
            putLong(KEY_EXPIRES_AT_EPOCH_MS, grant.expiresAtEpochMs)
            remove(KEY_EXPIRED_RULE_ID)
            remove(KEY_EXPIRED_BLOCKED_PACKAGE)
            remove(KEY_EXPIRED_AT_EPOCH_MS)
        }
    }

    fun load(): UnlockGrant? {
        val ruleId = prefs.getString(KEY_RULE_ID, null)
        val blockedPackage = prefs.getString(KEY_BLOCKED_PACKAGE, null) ?: return null
        val expiresAt = prefs.getLong(KEY_EXPIRES_AT_EPOCH_MS, -1L)

        return persistedUnlockGrantOrNull(
            ruleId = ruleId,
            blockedPackage = blockedPackage,
            expiresAtEpochMs = expiresAt,
        )
    }

    fun loadActive(nowEpochMs: Long = System.currentTimeMillis()): UnlockGrant? {
        val grant = load() ?: return null
        if (grant.expiresAtEpochMs > nowEpochMs) {
            return grant
        }
        recordExpiredCredit(grant)
        clearActiveGrant()
        return null
    }

    fun loadExpiredCreditForRule(
        ruleId: String,
        blockedPackage: String,
    ): ExpiredUnlockCredit? {
        val expiredCredit = loadExpiredCredit() ?: return null
        return expiredCredit.takeIf {
            it.ruleId == ruleId && it.blockedPackage == blockedPackage
        }
    }

    fun loadForRule(
        ruleId: String,
        blockedPackage: String,
        nowEpochMs: Long = System.currentTimeMillis(),
        clearOnMismatch: Boolean = true,
    ): UnlockGrant? {
        val grant = loadActive(nowEpochMs) ?: return null
        unlockGrantForRuleOrNull(
            grant = grant,
            ruleId = ruleId,
            blockedPackage = blockedPackage,
            nowEpochMs = nowEpochMs,
        )?.let {
            return it
        }
        if (clearOnMismatch) {
            clear()
        }
        return null
    }

    fun isUnlocked(
        ruleId: String,
        blockedPackage: String,
        nowEpochMs: Long = System.currentTimeMillis(),
    ): Boolean {
        return loadForRule(
            ruleId = ruleId,
            blockedPackage = blockedPackage,
            nowEpochMs = nowEpochMs,
        ) != null
    }

    fun clear() {
        prefs.edit { clear() }
    }

    private fun loadExpiredCredit(): ExpiredUnlockCredit? {
        val ruleId = prefs.getString(KEY_EXPIRED_RULE_ID, null)
        val blockedPackage = prefs.getString(KEY_EXPIRED_BLOCKED_PACKAGE, null)
        val expiredAtEpochMs = prefs.getLong(KEY_EXPIRED_AT_EPOCH_MS, -1L)
        if (ruleId.isNullOrBlank()) return null
        if (blockedPackage.isNullOrBlank()) return null
        if (expiredAtEpochMs <= 0L) return null

        return ExpiredUnlockCredit(
            ruleId = ruleId,
            blockedPackage = blockedPackage,
            expiredAtEpochMs = expiredAtEpochMs,
        )
    }

    private fun recordExpiredCredit(grant: UnlockGrant) {
        prefs.edit {
            putString(KEY_EXPIRED_RULE_ID, grant.ruleId)
            putString(KEY_EXPIRED_BLOCKED_PACKAGE, grant.blockedPackage)
            putLong(KEY_EXPIRED_AT_EPOCH_MS, grant.expiresAtEpochMs)
        }
    }

    private fun clearActiveGrant() {
        prefs.edit {
            remove(KEY_RULE_ID)
            remove(KEY_BLOCKED_PACKAGE)
            remove(KEY_EXPIRES_AT_EPOCH_MS)
        }
    }

    private companion object {
        const val PREFS_NAME = "unlock_grant"
        const val KEY_RULE_ID = "rule_id"
        const val KEY_BLOCKED_PACKAGE = "blocked_package"
        const val KEY_EXPIRES_AT_EPOCH_MS = "expires_at_epoch_ms"
        const val KEY_EXPIRED_RULE_ID = "expired_rule_id"
        const val KEY_EXPIRED_BLOCKED_PACKAGE = "expired_blocked_package"
        const val KEY_EXPIRED_AT_EPOCH_MS = "expired_at_epoch_ms"
    }
}

internal fun persistedUnlockGrantOrNull(
    ruleId: String?,
    blockedPackage: String?,
    expiresAtEpochMs: Long,
): UnlockGrant? {
    if (ruleId.isNullOrBlank()) return null
    if (blockedPackage.isNullOrBlank()) return null
    if (expiresAtEpochMs <= 0L) return null

    return UnlockGrant(
        ruleId = ruleId,
        blockedPackage = blockedPackage,
        expiresAtEpochMs = expiresAtEpochMs,
    )
}

internal fun unlockGrantForRuleOrNull(
    grant: UnlockGrant?,
    ruleId: String,
    blockedPackage: String,
    nowEpochMs: Long,
): UnlockGrant? {
    if (grant == null) return null
    if (grant.expiresAtEpochMs <= nowEpochMs) return null
    return grant.takeIf {
        it.ruleId == ruleId && it.blockedPackage == blockedPackage
    }
}
