package com.larissa.socialcontrol

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel

enum class HeroStatus {
    NOT_CONFIGURED,
    MISSING_PERMISSIONS,
    READY,
    CHALLENGE_IN_PROGRESS,
    CREDITS_ACTIVE,
    RULES_WITH_PROBLEM,
}

enum class RuleCardStatus {
    ACTIVE,
    INACTIVE,
    INVALID,
    PERMISSIONS_INCOMPLETE,
}

data class SelectedApp(
    val packageName: String,
    val appName: String,
    val isInstalled: Boolean = true,
)

data class RuleEditorUiState(
    val editingRuleId: String? = null,
    val blockedApp: SelectedApp? = null,
    val controlApp: SelectedApp? = null,
    val requiredSecondsInput: String = AppConfig.DEFAULT_REQUIRED_SECONDS.toString(),
    val unlockWindowMinutesInput: String = AppConfig.DEFAULT_UNLOCK_WINDOW_MINUTES.toString(),
    val isEnabled: Boolean = true,
    val validation: RuleValidationResult = RuleValidationResult(),
    val isSaveEnabled: Boolean = false,
) {
    val isCreating: Boolean
        get() = editingRuleId == null
}

data class RuleCardUiState(
    val ruleId: String,
    val blockedAppName: String,
    val blockedPackage: String,
    val controlAppName: String,
    val controlPackage: String,
    val requiredSeconds: Int,
    val unlockWindowMinutes: Int,
    val isEnabled: Boolean,
    val status: RuleCardStatus,
    val validation: RuleValidationResult,
)

data class ActiveCreditUiState(
    val ruleId: String,
    val blockedAppName: String,
    val expiresAtEpochMs: Long,
    val unlockWindowMinutes: Int,
)

data class ActiveChallengeUiState(
    val ruleId: String,
    val blockedAppName: String,
    val controlAppName: String,
    val trackedSeconds: Int,
    val requiredSeconds: Int,
    val hasUsageAccess: Boolean,
)

data class DashboardUiState(
    val protectedApps: List<String> = emptyList(),
    val activeCredits: List<ActiveCreditUiState> = emptyList(),
    val activeChallenges: List<ActiveChallengeUiState> = emptyList(),
    val alerts: List<String> = emptyList(),
)

data class MainUiState(
    val heroStatus: HeroStatus = HeroStatus.NOT_CONFIGURED,
    val readiness: PermissionReadiness = PermissionReadiness(
        accessibilityEnabled = false,
        usageAccessEnabled = false,
    ),
    val installedApps: List<InstalledAppInfo> = emptyList(),
    val storedRules: List<InterventionRule> = emptyList(),
    val rules: List<RuleCardUiState> = emptyList(),
    val dashboard: DashboardUiState = DashboardUiState(),
    val editor: RuleEditorUiState? = null,
)

class MainViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val ruleStore = InterventionRuleStore(application)
    private val sessionStore = ChallengeSessionStore(application)
    private val unlockGrantStore = UnlockGrantStore(application)
    private val installedAppRepository = InstalledAppRepository(application)
    private val permissionStatusRepository = PermissionStatusRepository(application)
    private val challengeTracker = UsageStatsChallengeTracker(application)
    private val runtimeValidator = RuleRuntimeValidator(installedAppRepository)

    var uiState by mutableStateOf(MainUiState())
        private set

    init {
        refresh()
    }

    fun refresh() {
        val installedApps = installedAppRepository.getLaunchableApps()
        val readiness = permissionStatusRepository.getReadiness()
        val rules = ruleStore.loadAll().sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.blockedAppName })
        val validations = rules.associate { rule ->
            rule.ruleId to runtimeValidator.validateSavedRule(rule, rules)
        }

        clearStaleTransientState(validRuleIds = rules.map { it.ruleId }.toSet())

        val disabledOrInvalidRuleIds = rules
            .filter { rule -> !rule.isEnabled || validations[rule.ruleId]?.isValid != true }
            .map { it.ruleId }
            .toSet()
        sessionStore.clearForRules(disabledOrInvalidRuleIds)
        unlockGrantStore.clearForRules(disabledOrInvalidRuleIds)

        completeFinishedChallenges(rules = rules, validations = validations)

        val activeSessions = sessionStore.loadAll()
            .filter { session -> rules.any { it.ruleId == session.ruleId } }
            .sortedByDescending { it.startedAtEpochMs }
        val activeGrants = unlockGrantStore.loadAllActive()
            .filter { grant -> rules.any { it.ruleId == grant.ruleId } }
            .sortedBy { it.expiresAtEpochMs }

        val rulesById = rules.associateBy { it.ruleId }
        val challengeSummaries = activeSessions.mapNotNull { session ->
            val rule = rulesById[session.ruleId] ?: return@mapNotNull null
            val progress = challengeTracker.calculateProgress(session)
            ActiveChallengeUiState(
                ruleId = rule.ruleId,
                blockedAppName = rule.blockedAppName,
                controlAppName = rule.controlAppName,
                trackedSeconds = progress.trackedSeconds,
                requiredSeconds = progress.requiredSeconds,
                hasUsageAccess = progress.hasUsageAccess,
            )
        }

        val creditSummaries = activeGrants.mapNotNull { grant ->
            val rule = rulesById[grant.ruleId] ?: return@mapNotNull null
            ActiveCreditUiState(
                ruleId = rule.ruleId,
                blockedAppName = rule.blockedAppName,
                expiresAtEpochMs = grant.expiresAtEpochMs,
                unlockWindowMinutes = rule.unlockWindowMinutes,
            )
        }

        val protectedApps = rules
            .filter { rule -> rule.isEnabled && validations[rule.ruleId]?.isValid == true }
            .map { it.blockedAppName }

        val alerts = buildAlerts(
            readiness = readiness,
            rules = rules,
            validations = validations,
        )

        val editor = reconcileEditor(
            editor = uiState.editor,
            installedApps = installedApps,
            existingRules = rules,
        )

        uiState = MainUiState(
            heroStatus = resolveHeroStatus(
                rules = rules,
                validations = validations,
                readiness = readiness,
                activeChallenges = challengeSummaries,
                activeCredits = creditSummaries,
            ),
            readiness = readiness,
            installedApps = installedApps,
            storedRules = rules,
            rules = rules.map { rule ->
                RuleCardUiState(
                    ruleId = rule.ruleId,
                    blockedAppName = rule.blockedAppName,
                    blockedPackage = rule.blockedPackage,
                    controlAppName = rule.controlAppName,
                    controlPackage = rule.controlPackage,
                    requiredSeconds = rule.requiredSeconds,
                    unlockWindowMinutes = rule.unlockWindowMinutes,
                    isEnabled = rule.isEnabled,
                    status = resolveRuleStatus(
                        rule = rule,
                        validation = validations.getValue(rule.ruleId),
                        readiness = readiness,
                    ),
                    validation = validations.getValue(rule.ruleId),
                )
            },
            dashboard = DashboardUiState(
                protectedApps = protectedApps,
                activeCredits = creditSummaries,
                activeChallenges = challengeSummaries,
                alerts = alerts,
            ),
            editor = editor,
        )
    }

    fun startCreatingRule() {
        uiState = uiState.copy(
            editor = validateEditor(
                editor = RuleEditorUiState(),
                existingRules = uiState.storedRules,
                installedApps = uiState.installedApps,
            ),
        )
    }

    fun startEditingRule(ruleId: String) {
        val rule = uiState.storedRules.firstOrNull { it.ruleId == ruleId } ?: return
        uiState = uiState.copy(
            editor = validateEditor(
                editor = RuleEditorUiState(
                    editingRuleId = rule.ruleId,
                    blockedApp = SelectedApp(
                        packageName = rule.blockedPackage,
                        appName = rule.blockedAppName,
                    ),
                    controlApp = SelectedApp(
                        packageName = rule.controlPackage,
                        appName = rule.controlAppName,
                    ),
                    requiredSecondsInput = rule.requiredSeconds.toString(),
                    unlockWindowMinutesInput = rule.unlockWindowMinutes.toString(),
                    isEnabled = rule.isEnabled,
                ),
                existingRules = uiState.storedRules,
                installedApps = uiState.installedApps,
            ),
        )
    }

    fun cancelEditing() {
        uiState = uiState.copy(editor = null)
    }

    fun onBlockedAppSelected(app: InstalledAppInfo) {
        updateEditor { it.copy(blockedApp = app.toSelectedApp()) }
    }

    fun onBlockedAppCleared() {
        updateEditor { it.copy(blockedApp = null) }
    }

    fun onControlAppSelected(app: InstalledAppInfo) {
        updateEditor { it.copy(controlApp = app.toSelectedApp()) }
    }

    fun onControlAppCleared() {
        updateEditor { it.copy(controlApp = null) }
    }

    fun onRequiredSecondsChanged(value: String) {
        updateEditor {
            it.copy(requiredSecondsInput = value.filter(Char::isDigit).take(3))
        }
    }

    fun onUnlockWindowMinutesChanged(value: String) {
        updateEditor {
            it.copy(unlockWindowMinutesInput = value.filter(Char::isDigit).take(2))
        }
    }

    fun onRuleEnabledChanged(enabled: Boolean) {
        updateEditor { it.copy(isEnabled = enabled) }
    }

    fun saveRule() {
        val editor = uiState.editor ?: return
        if (!editor.isSaveEnabled) return

        val blockedApp = editor.blockedApp ?: return
        val controlApp = editor.controlApp ?: return

        val savedRule = ruleStore.upsert(
            draft = InterventionRuleDraft(
                blockedPackage = blockedApp.packageName,
                blockedAppName = blockedApp.appName,
                controlPackage = controlApp.packageName,
                controlAppName = controlApp.appName,
                requiredSeconds = editor.requiredSecondsInput.toInt(),
                unlockWindowMinutes = editor.unlockWindowMinutesInput.toInt(),
                isEnabled = editor.isEnabled,
            ),
            ruleId = editor.editingRuleId,
        )

        sessionStore.clearForRule(savedRule.ruleId)
        unlockGrantStore.clearForRule(savedRule.ruleId)
        uiState = uiState.copy(editor = null)
        refresh()
    }

    fun deleteRule(ruleId: String) {
        ruleStore.delete(ruleId)
        sessionStore.clearForRule(ruleId)
        unlockGrantStore.clearForRule(ruleId)
        if (uiState.editor?.editingRuleId == ruleId) {
            uiState = uiState.copy(editor = null)
        }
        refresh()
    }

    fun toggleRuleEnabled(ruleId: String) {
        val rule = uiState.storedRules.firstOrNull { it.ruleId == ruleId } ?: return
        val updatedRule = ruleStore.upsert(
            draft = rule.toDraft().copy(isEnabled = !rule.isEnabled),
            ruleId = rule.ruleId,
        )
        if (!updatedRule.isEnabled) {
            sessionStore.clearForRule(updatedRule.ruleId)
            unlockGrantStore.clearForRule(updatedRule.ruleId)
        }
        refresh()
    }

    private fun updateEditor(transform: (RuleEditorUiState) -> RuleEditorUiState) {
        val editor = uiState.editor ?: return
        uiState = uiState.copy(
            editor = validateEditor(
                editor = transform(editor),
                existingRules = uiState.storedRules,
                installedApps = uiState.installedApps,
            ),
        )
    }

    private fun validateEditor(
        editor: RuleEditorUiState,
        existingRules: List<InterventionRule>,
        installedApps: List<InstalledAppInfo>,
    ): RuleEditorUiState {
        val reconciledEditor = editor.copy(
            blockedApp = reconcileSelection(editor.blockedApp, installedApps),
            controlApp = reconcileSelection(editor.controlApp, installedApps),
        )

        val issues = runtimeValidator.validateDraft(
            input = RuleDraftValidationInput(
                blockedPackage = reconciledEditor.blockedApp?.packageName,
                controlPackage = reconciledEditor.controlApp?.packageName,
                requiredSeconds = reconciledEditor.requiredSecondsInput.toIntOrNull(),
                unlockWindowMinutes = reconciledEditor.unlockWindowMinutesInput.toIntOrNull(),
            ),
            existingRules = existingRules,
            editingRuleId = reconciledEditor.editingRuleId,
        ).issues.toMutableSet()

        if (reconciledEditor.blockedApp != null && !reconciledEditor.blockedApp.isInstalled) {
            issues += RuleValidationIssue.BLOCKED_APP_NOT_INSTALLED
        }
        if (reconciledEditor.controlApp != null && !reconciledEditor.controlApp.isInstalled) {
            issues += RuleValidationIssue.CONTROL_APP_NOT_INSTALLED
        }

        val validation = RuleValidationResult(issues)
        return reconciledEditor.copy(
            validation = validation,
            isSaveEnabled = validation.isValid && editorDiffersFromSaved(reconciledEditor, existingRules),
        )
    }

    private fun reconcileEditor(
        editor: RuleEditorUiState?,
        installedApps: List<InstalledAppInfo>,
        existingRules: List<InterventionRule>,
    ): RuleEditorUiState? {
        if (editor == null) return null
        if (editor.editingRuleId != null &&
            existingRules.none { it.ruleId == editor.editingRuleId }
        ) {
            return null
        }

        return validateEditor(
            editor = editor,
            existingRules = existingRules,
            installedApps = installedApps,
        )
    }

    private fun reconcileSelection(
        selection: SelectedApp?,
        installedApps: List<InstalledAppInfo>,
    ): SelectedApp? {
        if (selection == null) return null
        val installedApp = installedApps.firstOrNull { it.packageName == selection.packageName }
        return if (installedApp != null) {
            installedApp.toSelectedApp()
        } else {
            selection.copy(isInstalled = false)
        }
    }

    private fun editorDiffersFromSaved(
        editor: RuleEditorUiState,
        existingRules: List<InterventionRule>,
    ): Boolean {
        val savedRule = editor.editingRuleId?.let { ruleId ->
            existingRules.firstOrNull { it.ruleId == ruleId }
        }

        if (savedRule == null) {
            return editor.blockedApp != null ||
                editor.controlApp != null ||
                editor.requiredSecondsInput != AppConfig.DEFAULT_REQUIRED_SECONDS.toString() ||
                editor.unlockWindowMinutesInput != AppConfig.DEFAULT_UNLOCK_WINDOW_MINUTES.toString() ||
                !editor.isEnabled
        }

        return savedRule.blockedPackage != editor.blockedApp?.packageName ||
            savedRule.blockedAppName != editor.blockedApp?.appName ||
            savedRule.controlPackage != editor.controlApp?.packageName ||
            savedRule.controlAppName != editor.controlApp?.appName ||
            savedRule.requiredSeconds.toString() != editor.requiredSecondsInput ||
            savedRule.unlockWindowMinutes.toString() != editor.unlockWindowMinutesInput ||
            savedRule.isEnabled != editor.isEnabled
    }

    private fun clearStaleTransientState(validRuleIds: Set<String>) {
        val staleSessionIds = sessionStore.loadAll()
            .map { it.ruleId }
            .filterNot { it in validRuleIds }
            .toSet()
        sessionStore.clearForRules(staleSessionIds)

        val staleGrantIds = (
            unlockGrantStore.loadAllActive().map { it.ruleId } +
                unlockGrantStore.loadExpiredCredits().map { it.ruleId }
            )
            .filterNot { it in validRuleIds }
            .toSet()
        unlockGrantStore.clearForRules(staleGrantIds)
    }

    private fun completeFinishedChallenges(
        rules: List<InterventionRule>,
        validations: Map<String, RuleValidationResult>,
    ) {
        val rulesById = rules.associateBy { it.ruleId }
        sessionStore.loadAll().forEach { session ->
            val rule = rulesById[session.ruleId] ?: return@forEach
            val validation = validations[rule.ruleId] ?: return@forEach
            if (!rule.isEnabled || !validation.isValid) {
                sessionStore.clearForRule(rule.ruleId)
                unlockGrantStore.clearForRule(rule.ruleId)
                return@forEach
            }

            val progress = challengeTracker.calculateProgress(session)
            if (progress.isComplete) {
                val completedAt = System.currentTimeMillis()
                unlockGrantStore.save(
                    UnlockGrant(
                        ruleId = rule.ruleId,
                        blockedPackage = rule.blockedPackage,
                        expiresAtEpochMs = completedAt + rule.unlockWindowMinutes * 60_000L,
                    ),
                )
                sessionStore.clearForRule(rule.ruleId)
            }
        }
    }

    private fun buildAlerts(
        readiness: PermissionReadiness,
        rules: List<InterventionRule>,
        validations: Map<String, RuleValidationResult>,
    ): List<String> {
        return buildList {
            val invalidRuleCount = rules.count { validations[it.ruleId]?.isValid == false }
            if (invalidRuleCount > 0) {
                add(
                    if (invalidRuleCount == 1) {
                        "1 regra precisa ser revisada."
                    } else {
                        "$invalidRuleCount regras precisam ser revisadas."
                    },
                )
            }
            if (!readiness.accessibilityEnabled) {
                add("A acessibilidade precisa ser ativada.")
            }
            if (!readiness.usageAccessEnabled) {
                add("Dados de uso ainda não estão ativos.")
            }
        }
    }

    private fun resolveHeroStatus(
        rules: List<InterventionRule>,
        validations: Map<String, RuleValidationResult>,
        readiness: PermissionReadiness,
        activeChallenges: List<ActiveChallengeUiState>,
        activeCredits: List<ActiveCreditUiState>,
    ): HeroStatus {
        if (rules.isEmpty()) return HeroStatus.NOT_CONFIGURED
        if (activeChallenges.isNotEmpty()) return HeroStatus.CHALLENGE_IN_PROGRESS
        if (activeCredits.isNotEmpty()) return HeroStatus.CREDITS_ACTIVE
        if (rules.any { validations[it.ruleId]?.isValid == false }) return HeroStatus.RULES_WITH_PROBLEM
        if (!readiness.isFullyReady) return HeroStatus.MISSING_PERMISSIONS
        return HeroStatus.READY
    }

    private fun resolveRuleStatus(
        rule: InterventionRule,
        validation: RuleValidationResult,
        readiness: PermissionReadiness,
    ): RuleCardStatus {
        if (!validation.isValid) return RuleCardStatus.INVALID
        if (!rule.isEnabled) return RuleCardStatus.INACTIVE
        if (!readiness.isFullyReady) return RuleCardStatus.PERMISSIONS_INCOMPLETE
        return RuleCardStatus.ACTIVE
    }
}

private fun InstalledAppInfo.toSelectedApp(): SelectedApp {
    return SelectedApp(
        packageName = packageName,
        appName = label,
        isInstalled = true,
    )
}
