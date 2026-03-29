package com.larissa.socialcontrol

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class InterventionRuleStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun upsert(
        draft: InterventionRuleDraft,
        ruleId: String? = null,
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

        val storedRules = loadAll().associateBy { it.ruleId }.toMutableMap()
        val persistedRule = InterventionRule(
            ruleId = ruleId ?: UUID.randomUUID().toString(),
            blockedPackage = draft.blockedPackage,
            blockedAppName = draft.blockedAppName,
            controlPackage = draft.controlPackage,
            controlAppName = draft.controlAppName,
            requiredSeconds = draft.requiredSeconds,
            unlockWindowMinutes = draft.unlockWindowMinutes,
            isEnabled = draft.isEnabled,
            savedAtEpochMs = nowEpochMs,
        )

        storedRules[persistedRule.ruleId] = persistedRule
        persistRules(storedRules.values.sortedByDescending { it.savedAtEpochMs })
        return persistedRule
    }

    fun loadAll(): List<InterventionRule> {
        val persistedJson = prefs.getString(KEY_RULES_JSON, null)
        if (!persistedJson.isNullOrBlank()) {
            return InterventionRuleJsonCodec.decodeRules(persistedJson)
                .sortedByDescending { it.savedAtEpochMs }
        }

        return loadLegacyRule()?.let(::listOf).orEmpty()
    }

    fun load(ruleId: String): InterventionRule? = loadAll().firstOrNull { it.ruleId == ruleId }

    fun findByBlockedPackage(blockedPackage: String): InterventionRule? {
        return loadAll().firstOrNull { it.blockedPackage == blockedPackage }
    }

    fun delete(ruleId: String) {
        persistRules(loadAll().filterNot { it.ruleId == ruleId })
    }

    fun clear() {
        prefs.edit { clear() }
    }

    private fun persistRules(rules: List<InterventionRule>) {
        prefs.edit {
            if (rules.isEmpty()) {
                remove(KEY_RULES_JSON)
            } else {
                putString(KEY_RULES_JSON, InterventionRuleJsonCodec.encodeRules(rules))
            }
            remove(KEY_RULE_ID)
            remove(KEY_BLOCKED_PACKAGE)
            remove(KEY_BLOCKED_APP_NAME)
            remove(KEY_CONTROL_PACKAGE)
            remove(KEY_CONTROL_APP_NAME)
            remove(KEY_REQUIRED_SECONDS)
            remove(KEY_UNLOCK_WINDOW_MINUTES)
            remove(KEY_IS_ENABLED)
            remove(KEY_SAVED_AT_EPOCH_MS)
        }
    }

    private fun loadLegacyRule(): InterventionRule? {
        return StoredInterventionRuleMapper.fromPersistedValues(
            ruleId = prefs.getString(KEY_RULE_ID, null),
            blockedPackage = prefs.getString(KEY_BLOCKED_PACKAGE, null),
            blockedAppName = prefs.getString(KEY_BLOCKED_APP_NAME, null),
            controlPackage = prefs.getString(KEY_CONTROL_PACKAGE, null),
            controlAppName = prefs.getString(KEY_CONTROL_APP_NAME, null),
            requiredSeconds = prefs.getInt(KEY_REQUIRED_SECONDS, -1),
            unlockWindowMinutes = prefs.getInt(KEY_UNLOCK_WINDOW_MINUTES, -1),
            isEnabled = prefs.getBoolean(KEY_IS_ENABLED, true),
            savedAtEpochMs = prefs.getLong(KEY_SAVED_AT_EPOCH_MS, -1L),
        )
    }

    private companion object {
        const val PREFS_NAME = "intervention_rule"
        const val KEY_RULES_JSON = "rules_json"
        const val KEY_RULE_ID = "rule_id"
        const val KEY_BLOCKED_PACKAGE = "blocked_package"
        const val KEY_BLOCKED_APP_NAME = "blocked_app_name"
        const val KEY_CONTROL_PACKAGE = "control_package"
        const val KEY_CONTROL_APP_NAME = "control_app_name"
        const val KEY_REQUIRED_SECONDS = "required_seconds"
        const val KEY_UNLOCK_WINDOW_MINUTES = "unlock_window_minutes"
        const val KEY_IS_ENABLED = "is_enabled"
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
        isEnabled: Boolean,
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
            isEnabled = isEnabled,
            savedAtEpochMs = savedAtEpochMs,
        )
    }
}

internal object InterventionRuleJsonCodec {
    fun encodeRules(rules: List<InterventionRule>): String {
        val jsonArray = JSONArray()
        rules.forEach { rule ->
            jsonArray.put(
                JSONObject().apply {
                    put("ruleId", rule.ruleId)
                    put("blockedPackage", rule.blockedPackage)
                    put("blockedAppName", rule.blockedAppName)
                    put("controlPackage", rule.controlPackage)
                    put("controlAppName", rule.controlAppName)
                    put("requiredSeconds", rule.requiredSeconds)
                    put("unlockWindowMinutes", rule.unlockWindowMinutes)
                    put("isEnabled", rule.isEnabled)
                    put("savedAtEpochMs", rule.savedAtEpochMs)
                },
            )
        }
        return jsonArray.toString()
    }

    fun decodeRules(serializedRules: String): List<InterventionRule> {
        return runCatching {
            val jsonArray = JSONArray(serializedRules)
            buildList {
                for (index in 0 until jsonArray.length()) {
                    val jsonObject = jsonArray.optJSONObject(index) ?: continue
                    val rule = StoredInterventionRuleMapper.fromPersistedValues(
                        ruleId = jsonObject.optString("ruleId").takeIf { it.isNotBlank() },
                        blockedPackage = jsonObject.optString("blockedPackage").takeIf { it.isNotBlank() },
                        blockedAppName = jsonObject.optString("blockedAppName").takeIf { it.isNotBlank() },
                        controlPackage = jsonObject.optString("controlPackage").takeIf { it.isNotBlank() },
                        controlAppName = jsonObject.optString("controlAppName").takeIf { it.isNotBlank() },
                        requiredSeconds = jsonObject.optInt("requiredSeconds", -1),
                        unlockWindowMinutes = jsonObject.optInt("unlockWindowMinutes", -1),
                        isEnabled = jsonObject.optBoolean("isEnabled", true),
                        savedAtEpochMs = jsonObject.optLong("savedAtEpochMs", -1L),
                    )
                    if (rule != null) {
                        add(rule)
                    }
                }
            }
        }.getOrDefault(emptyList())
    }
}
