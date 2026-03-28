package com.larissa.socialcontrol

import android.content.Context
import androidx.core.content.edit

data class UnlockGrant(
    val blockedPackage: String,
    val expiresAtEpochMs: Long,
)

class UnlockGrantStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun save(grant: UnlockGrant) {
        prefs.edit {
            putString(KEY_BLOCKED_PACKAGE, grant.blockedPackage)
            putLong(KEY_EXPIRES_AT_EPOCH_MS, grant.expiresAtEpochMs)
        }
    }

    fun load(): UnlockGrant? {
        val blockedPackage = prefs.getString(KEY_BLOCKED_PACKAGE, null) ?: return null
        val expiresAt = prefs.getLong(KEY_EXPIRES_AT_EPOCH_MS, -1L)
        if (expiresAt <= 0L) return null

        return UnlockGrant(
            blockedPackage = blockedPackage,
            expiresAtEpochMs = expiresAt,
        )
    }

    fun isUnlocked(blockedPackage: String, nowEpochMs: Long = System.currentTimeMillis()): Boolean {
        val grant = load() ?: return false
        if (grant.blockedPackage != blockedPackage) return false
        if (grant.expiresAtEpochMs <= nowEpochMs) {
            clear()
            return false
        }
        return true
    }

    fun clear() {
        prefs.edit { clear() }
    }

    private companion object {
        const val PREFS_NAME = "unlock_grant"
        const val KEY_BLOCKED_PACKAGE = "blocked_package"
        const val KEY_EXPIRES_AT_EPOCH_MS = "expires_at_epoch_ms"
    }
}
