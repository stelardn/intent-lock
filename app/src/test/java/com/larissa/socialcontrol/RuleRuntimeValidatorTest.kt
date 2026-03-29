package com.larissa.socialcontrol

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RuleRuntimeValidatorTest {
    @Test
    fun `same app is rejected in draft validation`() {
        val validator = RuleRuntimeValidator(FakeInstalledAppLookup())

        val result = validator.validateDraft(
            RuleDraftValidationInput(
                blockedPackage = "com.same",
                controlPackage = "com.same",
                requiredSeconds = 30,
                unlockWindowMinutes = 10,
            ),
        )

        assertTrue(result.hasIssue(RuleValidationIssue.APPS_MUST_DIFFER))
    }

    @Test
    fun `duration and unlock bounds are enforced`() {
        val validator = RuleRuntimeValidator(FakeInstalledAppLookup())

        val result = validator.validateDraft(
            RuleDraftValidationInput(
                blockedPackage = "com.blocked",
                controlPackage = "com.control",
                requiredSeconds = 5,
                unlockWindowMinutes = 70,
            ),
        )

        assertTrue(result.hasIssue(RuleValidationIssue.REQUIRED_SECONDS_OUT_OF_RANGE))
        assertTrue(result.hasIssue(RuleValidationIssue.UNLOCK_WINDOW_OUT_OF_RANGE))
    }

    @Test
    fun `missing installed app invalidates saved rule`() {
        val validator = RuleRuntimeValidator(
            FakeInstalledAppLookup(installedPackages = setOf("com.control")),
        )
        val rule = InterventionRule(
            ruleId = "rule-1",
            blockedPackage = "com.blocked",
            blockedAppName = "Blocked",
            controlPackage = "com.control",
            controlAppName = "Control",
            requiredSeconds = 30,
            unlockWindowMinutes = 10,
            savedAtEpochMs = 123L,
        )

        val result = validator.validateSavedRule(rule)

        assertTrue(result.hasIssue(RuleValidationIssue.BLOCKED_APP_NOT_INSTALLED))
        assertFalse(result.isValid)
    }

    private class FakeInstalledAppLookup(
        private val installedPackages: Set<String> = emptySet(),
    ) : InstalledAppLookup {
        override fun isPackageInstalled(packageName: String): Boolean {
            return packageName in installedPackages
        }
    }
}
