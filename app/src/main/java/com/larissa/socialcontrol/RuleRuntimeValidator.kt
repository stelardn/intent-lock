package com.larissa.socialcontrol

enum class RuleValidationIssue {
    BLOCKED_APP_REQUIRED,
    CONTROL_APP_REQUIRED,
    BLOCKED_APP_ALREADY_USED,
    APPS_MUST_DIFFER,
    REQUIRED_SECONDS_OUT_OF_RANGE,
    UNLOCK_WINDOW_OUT_OF_RANGE,
    BLOCKED_APP_NOT_INSTALLED,
    CONTROL_APP_NOT_INSTALLED,
}

data class RuleValidationResult(
    val issues: Set<RuleValidationIssue> = emptySet(),
) {
    val isValid: Boolean
        get() = issues.isEmpty()

    fun hasIssue(issue: RuleValidationIssue): Boolean = issues.contains(issue)
}

data class RuleDraftValidationInput(
    val blockedPackage: String?,
    val controlPackage: String?,
    val requiredSeconds: Int?,
    val unlockWindowMinutes: Int?,
)

class RuleRuntimeValidator(
    private val installedAppLookup: InstalledAppLookup,
) {
    fun validateDraft(
        input: RuleDraftValidationInput,
        existingRules: List<InterventionRule> = emptyList(),
        editingRuleId: String? = null,
    ): RuleValidationResult {
        val issues = buildSet {
            if (input.blockedPackage.isNullOrBlank()) {
                add(RuleValidationIssue.BLOCKED_APP_REQUIRED)
            }
            if (input.controlPackage.isNullOrBlank()) {
                add(RuleValidationIssue.CONTROL_APP_REQUIRED)
            }
            if (!input.blockedPackage.isNullOrBlank() &&
                existingRules.any {
                    it.ruleId != editingRuleId && it.blockedPackage == input.blockedPackage
                }
            ) {
                add(RuleValidationIssue.BLOCKED_APP_ALREADY_USED)
            }

            if (!input.blockedPackage.isNullOrBlank() &&
                input.blockedPackage == input.controlPackage
            ) {
                add(RuleValidationIssue.APPS_MUST_DIFFER)
            }

            val requiredSeconds = input.requiredSeconds
            if (requiredSeconds == null || !isChallengeDurationSupported(requiredSeconds)) {
                add(RuleValidationIssue.REQUIRED_SECONDS_OUT_OF_RANGE)
            }

            val unlockWindowMinutes = input.unlockWindowMinutes
            if (unlockWindowMinutes == null || !isUnlockWindowSupported(unlockWindowMinutes)) {
                add(RuleValidationIssue.UNLOCK_WINDOW_OUT_OF_RANGE)
            }
        }

        return RuleValidationResult(issues)
    }

    fun validateSavedRule(
        rule: InterventionRule,
        allRules: List<InterventionRule> = emptyList(),
    ): RuleValidationResult {
        val issues = validateDraft(
            RuleDraftValidationInput(
                blockedPackage = rule.blockedPackage,
                controlPackage = rule.controlPackage,
                requiredSeconds = rule.requiredSeconds,
                unlockWindowMinutes = rule.unlockWindowMinutes,
            ),
            existingRules = allRules,
            editingRuleId = rule.ruleId,
        ).issues.toMutableSet()

        if (!installedAppLookup.isPackageInstalled(rule.blockedPackage)) {
            issues += RuleValidationIssue.BLOCKED_APP_NOT_INSTALLED
        }
        if (!installedAppLookup.isPackageInstalled(rule.controlPackage)) {
            issues += RuleValidationIssue.CONTROL_APP_NOT_INSTALLED
        }

        return RuleValidationResult(issues)
    }
}
