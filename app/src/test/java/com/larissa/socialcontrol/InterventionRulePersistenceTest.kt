package com.larissa.socialcontrol

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InterventionRulePersistenceTest {
    @Test
    fun `incomplete persisted rule loads as null`() {
        val rule = StoredInterventionRuleMapper.fromPersistedValues(
            ruleId = "rule-1",
            blockedPackage = "com.blocked",
            blockedAppName = "Blocked",
            controlPackage = "com.control",
            controlAppName = null,
            requiredSeconds = 30,
            unlockWindowMinutes = 10,
            savedAtEpochMs = 123L,
        )

        assertNull(rule)
    }

    @Test
    fun `stale session rule id is rejected`() {
        val session = ChallengeSession(
            ruleId = "old-rule",
            blockedPackage = "com.blocked",
            controlPackage = "com.control",
            requiredSeconds = 20,
            startedAtEpochMs = 100L,
        )

        val result = sessionForRuleOrNull(session, "new-rule")

        assertNull(result)
    }

    @Test
    fun `stale unlock grant rule id is rejected`() {
        val grant = UnlockGrant(
            ruleId = "old-rule",
            blockedPackage = "com.blocked",
            expiresAtEpochMs = 10_000L,
        )

        val result = unlockGrantForRuleOrNull(
            grant = grant,
            ruleId = "new-rule",
            blockedPackage = "com.blocked",
            nowEpochMs = 1_000L,
        )

        assertNull(result)
    }

    @Test
    fun `matching unlock grant survives reconciliation`() {
        val grant = UnlockGrant(
            ruleId = "rule-1",
            blockedPackage = "com.blocked",
            expiresAtEpochMs = 10_000L,
        )

        val result = unlockGrantForRuleOrNull(
            grant = grant,
            ruleId = "rule-1",
            blockedPackage = "com.blocked",
            nowEpochMs = 1_000L,
        )

        assertEquals(grant, result)
    }
}
