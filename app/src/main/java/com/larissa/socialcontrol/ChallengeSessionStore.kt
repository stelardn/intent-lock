package com.larissa.socialcontrol

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

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
        val updatedSessions = loadAll()
            .filterNot { it.ruleId == session.ruleId }
            .plus(session)
            .sortedByDescending { it.startedAtEpochMs }

        persistSessions(updatedSessions)
    }

    fun loadAll(): List<ChallengeSession> {
        val persistedJson = prefs.getString(KEY_SESSIONS_JSON, null)
        if (!persistedJson.isNullOrBlank()) {
            return ChallengeSessionJsonCodec.decodeSessions(persistedJson)
                .sortedByDescending { it.startedAtEpochMs }
        }

        return loadLegacySession()?.let(::listOf).orEmpty()
    }

    fun loadForRule(ruleId: String): ChallengeSession? {
        return loadAll().firstOrNull { it.ruleId == ruleId }
    }

    fun markCompleted(ruleId: String, completedAtEpochMs: Long) {
        val session = loadForRule(ruleId) ?: return
        save(session.copy(completedAtEpochMs = completedAtEpochMs))
    }

    fun clearForRule(ruleId: String) {
        persistSessions(loadAll().filterNot { it.ruleId == ruleId })
    }

    fun clearForRules(ruleIds: Set<String>) {
        if (ruleIds.isEmpty()) return
        persistSessions(loadAll().filterNot { it.ruleId in ruleIds })
    }

    fun clear() {
        prefs.edit { clear() }
    }

    private fun persistSessions(sessions: List<ChallengeSession>) {
        prefs.edit {
            if (sessions.isEmpty()) {
                remove(KEY_SESSIONS_JSON)
            } else {
                putString(KEY_SESSIONS_JSON, ChallengeSessionJsonCodec.encodeSessions(sessions))
            }
            remove(KEY_RULE_ID)
            remove(KEY_BLOCKED_PACKAGE)
            remove(KEY_CONTROL_PACKAGE)
            remove(KEY_REQUIRED_SECONDS)
            remove(KEY_STARTED_AT_EPOCH_MS)
            remove(KEY_COMPLETED_AT_EPOCH_MS)
        }
    }

    private fun loadLegacySession(): ChallengeSession? {
        val completedAt = prefs.getLong(KEY_COMPLETED_AT_EPOCH_MS, -1L)
        return persistedChallengeSessionOrNull(
            ruleId = prefs.getString(KEY_RULE_ID, null),
            blockedPackage = prefs.getString(KEY_BLOCKED_PACKAGE, null),
            controlPackage = prefs.getString(KEY_CONTROL_PACKAGE, null),
            requiredSeconds = prefs.getInt(KEY_REQUIRED_SECONDS, -1),
            startedAtEpochMs = prefs.getLong(KEY_STARTED_AT_EPOCH_MS, -1L),
            completedAtEpochMs = completedAt.takeIf { it > 0L },
        )
    }

    private companion object {
        const val PREFS_NAME = "challenge_session"
        const val KEY_SESSIONS_JSON = "sessions_json"
        const val KEY_RULE_ID = "rule_id"
        const val KEY_BLOCKED_PACKAGE = "blocked_package"
        const val KEY_CONTROL_PACKAGE = "control_package"
        const val KEY_REQUIRED_SECONDS = "required_seconds"
        const val KEY_STARTED_AT_EPOCH_MS = "started_at_epoch_ms"
        const val KEY_COMPLETED_AT_EPOCH_MS = "completed_at_epoch_ms"
    }
}

internal object ChallengeSessionJsonCodec {
    fun encodeSessions(sessions: List<ChallengeSession>): String {
        val jsonArray = JSONArray()
        sessions.forEach { session ->
            jsonArray.put(
                JSONObject().apply {
                    put("ruleId", session.ruleId)
                    put("blockedPackage", session.blockedPackage)
                    put("controlPackage", session.controlPackage)
                    put("requiredSeconds", session.requiredSeconds)
                    put("startedAtEpochMs", session.startedAtEpochMs)
                    session.completedAtEpochMs?.let { put("completedAtEpochMs", it) }
                },
            )
        }
        return jsonArray.toString()
    }

    fun decodeSessions(serializedSessions: String): List<ChallengeSession> {
        return runCatching {
            val jsonArray = JSONArray(serializedSessions)
            buildList {
                for (index in 0 until jsonArray.length()) {
                    val jsonObject = jsonArray.optJSONObject(index) ?: continue
                    val session = persistedChallengeSessionOrNull(
                        ruleId = jsonObject.optString("ruleId").takeIf { it.isNotBlank() },
                        blockedPackage = jsonObject.optString("blockedPackage").takeIf { it.isNotBlank() },
                        controlPackage = jsonObject.optString("controlPackage").takeIf { it.isNotBlank() },
                        requiredSeconds = jsonObject.optInt("requiredSeconds", -1),
                        startedAtEpochMs = jsonObject.optLong("startedAtEpochMs", -1L),
                        completedAtEpochMs = jsonObject.optLong("completedAtEpochMs", -1L)
                            .takeIf { it > 0L },
                    )
                    if (session != null) {
                        add(session)
                    }
                }
            }
        }.getOrDefault(emptyList())
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
