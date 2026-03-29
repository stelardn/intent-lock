package com.larissa.socialcontrol

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

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
        val activeGrants = loadPersistedGrants()
            .filterNot { it.ruleId == grant.ruleId }
            .plus(grant)
            .sortedBy { it.expiresAtEpochMs }
        val expiredCredits = loadPersistedExpiredCredits().filterNot { it.ruleId == grant.ruleId }

        persist(activeGrants = activeGrants, expiredCredits = expiredCredits)
    }

    fun loadAllActive(nowEpochMs: Long = System.currentTimeMillis()): List<UnlockGrant> {
        val activeGrants = loadPersistedGrants()
        if (activeGrants.isEmpty()) {
            return emptyList()
        }

        val (validGrants, expiredGrants) = activeGrants.partition { it.expiresAtEpochMs > nowEpochMs }
        if (expiredGrants.isNotEmpty()) {
            val mergedExpiredCredits = mergeExpiredCredits(
                existingCredits = loadPersistedExpiredCredits(),
                expiredGrants = expiredGrants,
            )
            persist(activeGrants = validGrants, expiredCredits = mergedExpiredCredits)
        }

        return validGrants.sortedBy { it.expiresAtEpochMs }
    }

    fun loadExpiredCredits(): List<ExpiredUnlockCredit> {
        return loadPersistedExpiredCredits().sortedByDescending { it.expiredAtEpochMs }
    }

    fun loadExpiredCreditForRule(
        ruleId: String,
        blockedPackage: String,
    ): ExpiredUnlockCredit? {
        return loadExpiredCredits().firstOrNull {
            it.ruleId == ruleId && it.blockedPackage == blockedPackage
        }
    }

    fun loadForRule(
        ruleId: String,
        blockedPackage: String,
        nowEpochMs: Long = System.currentTimeMillis(),
    ): UnlockGrant? {
        val grant = loadAllActive(nowEpochMs).firstOrNull { it.ruleId == ruleId }
        return unlockGrantForRuleOrNull(
            grant = grant,
            ruleId = ruleId,
            blockedPackage = blockedPackage,
            nowEpochMs = nowEpochMs,
        )
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

    fun clearForRule(ruleId: String) {
        clearForRules(setOf(ruleId))
    }

    fun clearForRules(ruleIds: Set<String>) {
        if (ruleIds.isEmpty()) return

        persist(
            activeGrants = loadPersistedGrants().filterNot { it.ruleId in ruleIds },
            expiredCredits = loadPersistedExpiredCredits().filterNot { it.ruleId in ruleIds },
        )
    }

    fun clear() {
        prefs.edit { clear() }
    }

    private fun persist(
        activeGrants: List<UnlockGrant>,
        expiredCredits: List<ExpiredUnlockCredit>,
    ) {
        prefs.edit {
            if (activeGrants.isEmpty()) {
                remove(KEY_ACTIVE_GRANTS_JSON)
            } else {
                putString(KEY_ACTIVE_GRANTS_JSON, UnlockGrantJsonCodec.encodeGrants(activeGrants))
            }

            if (expiredCredits.isEmpty()) {
                remove(KEY_EXPIRED_CREDITS_JSON)
            } else {
                putString(KEY_EXPIRED_CREDITS_JSON, UnlockGrantJsonCodec.encodeExpiredCredits(expiredCredits))
            }

            remove(KEY_RULE_ID)
            remove(KEY_BLOCKED_PACKAGE)
            remove(KEY_EXPIRES_AT_EPOCH_MS)
            remove(KEY_EXPIRED_RULE_ID)
            remove(KEY_EXPIRED_BLOCKED_PACKAGE)
            remove(KEY_EXPIRED_AT_EPOCH_MS)
        }
    }

    private fun loadPersistedGrants(): List<UnlockGrant> {
        val persistedJson = prefs.getString(KEY_ACTIVE_GRANTS_JSON, null)
        if (!persistedJson.isNullOrBlank()) {
            return UnlockGrantJsonCodec.decodeGrants(persistedJson)
        }

        return loadLegacyGrant()?.let(::listOf).orEmpty()
    }

    private fun loadPersistedExpiredCredits(): List<ExpiredUnlockCredit> {
        val persistedJson = prefs.getString(KEY_EXPIRED_CREDITS_JSON, null)
        if (!persistedJson.isNullOrBlank()) {
            return UnlockGrantJsonCodec.decodeExpiredCredits(persistedJson)
        }

        return loadLegacyExpiredCredit()?.let(::listOf).orEmpty()
    }

    private fun loadLegacyGrant(): UnlockGrant? {
        return persistedUnlockGrantOrNull(
            ruleId = prefs.getString(KEY_RULE_ID, null),
            blockedPackage = prefs.getString(KEY_BLOCKED_PACKAGE, null),
            expiresAtEpochMs = prefs.getLong(KEY_EXPIRES_AT_EPOCH_MS, -1L),
        )
    }

    private fun loadLegacyExpiredCredit(): ExpiredUnlockCredit? {
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

    private fun mergeExpiredCredits(
        existingCredits: List<ExpiredUnlockCredit>,
        expiredGrants: List<UnlockGrant>,
    ): List<ExpiredUnlockCredit> {
        val mergedCredits = existingCredits.associateBy { it.ruleId }.toMutableMap()
        expiredGrants.forEach { grant ->
            mergedCredits[grant.ruleId] = ExpiredUnlockCredit(
                ruleId = grant.ruleId,
                blockedPackage = grant.blockedPackage,
                expiredAtEpochMs = grant.expiresAtEpochMs,
            )
        }
        return mergedCredits.values.sortedByDescending { it.expiredAtEpochMs }
    }

    private companion object {
        const val PREFS_NAME = "unlock_grant"
        const val KEY_ACTIVE_GRANTS_JSON = "active_grants_json"
        const val KEY_EXPIRED_CREDITS_JSON = "expired_credits_json"
        const val KEY_RULE_ID = "rule_id"
        const val KEY_BLOCKED_PACKAGE = "blocked_package"
        const val KEY_EXPIRES_AT_EPOCH_MS = "expires_at_epoch_ms"
        const val KEY_EXPIRED_RULE_ID = "expired_rule_id"
        const val KEY_EXPIRED_BLOCKED_PACKAGE = "expired_blocked_package"
        const val KEY_EXPIRED_AT_EPOCH_MS = "expired_at_epoch_ms"
    }
}

internal object UnlockGrantJsonCodec {
    fun encodeGrants(grants: List<UnlockGrant>): String {
        val jsonArray = JSONArray()
        grants.forEach { grant ->
            jsonArray.put(
                JSONObject().apply {
                    put("ruleId", grant.ruleId)
                    put("blockedPackage", grant.blockedPackage)
                    put("expiresAtEpochMs", grant.expiresAtEpochMs)
                },
            )
        }
        return jsonArray.toString()
    }

    fun decodeGrants(serializedGrants: String): List<UnlockGrant> {
        return runCatching {
            val jsonArray = JSONArray(serializedGrants)
            buildList {
                for (index in 0 until jsonArray.length()) {
                    val jsonObject = jsonArray.optJSONObject(index) ?: continue
                    val grant = persistedUnlockGrantOrNull(
                        ruleId = jsonObject.optString("ruleId").takeIf { it.isNotBlank() },
                        blockedPackage = jsonObject.optString("blockedPackage").takeIf { it.isNotBlank() },
                        expiresAtEpochMs = jsonObject.optLong("expiresAtEpochMs", -1L),
                    )
                    if (grant != null) {
                        add(grant)
                    }
                }
            }
        }.getOrDefault(emptyList())
    }

    fun encodeExpiredCredits(credits: List<ExpiredUnlockCredit>): String {
        val jsonArray = JSONArray()
        credits.forEach { credit ->
            jsonArray.put(
                JSONObject().apply {
                    put("ruleId", credit.ruleId)
                    put("blockedPackage", credit.blockedPackage)
                    put("expiredAtEpochMs", credit.expiredAtEpochMs)
                },
            )
        }
        return jsonArray.toString()
    }

    fun decodeExpiredCredits(serializedCredits: String): List<ExpiredUnlockCredit> {
        return runCatching {
            val jsonArray = JSONArray(serializedCredits)
            buildList {
                for (index in 0 until jsonArray.length()) {
                    val jsonObject = jsonArray.optJSONObject(index) ?: continue
                    val ruleId = jsonObject.optString("ruleId").takeIf { it.isNotBlank() }
                    val blockedPackage = jsonObject.optString("blockedPackage").takeIf { it.isNotBlank() }
                    val expiredAtEpochMs = jsonObject.optLong("expiredAtEpochMs", -1L)
                    if (!ruleId.isNullOrBlank() && !blockedPackage.isNullOrBlank() && expiredAtEpochMs > 0L) {
                        add(
                            ExpiredUnlockCredit(
                                ruleId = ruleId,
                                blockedPackage = blockedPackage,
                                expiredAtEpochMs = expiredAtEpochMs,
                            ),
                        )
                    }
                }
            }
        }.getOrDefault(emptyList())
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
