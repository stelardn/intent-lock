package com.larissa.socialcontrol

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel

enum class HeroStatus {
    NOT_CONFIGURED,
    CONFIGURED_MISSING_PERMISSIONS,
    READY,
    CHALLENGE_IN_PROGRESS,
    UNLOCKED,
    CREDITS_EXHAUSTED,
    SAVED_RULE_INVALID,
}

data class SelectedApp(
    val packageName: String,
    val appName: String,
    val isInstalled: Boolean = true,
)

data class RuleDraftUiState(
    val blockedApp: SelectedApp? = null,
    val controlApp: SelectedApp? = null,
    val requiredSecondsInput: String = AppConfig.DEFAULT_REQUIRED_SECONDS.toString(),
    val unlockWindowMinutesInput: String = AppConfig.DEFAULT_UNLOCK_WINDOW_MINUTES.toString(),
)

data class MainUiState(
    val heroStatus: HeroStatus = HeroStatus.NOT_CONFIGURED,
    val readiness: PermissionReadiness = PermissionReadiness(
        accessibilityEnabled = false,
        usageAccessEnabled = false,
    ),
    val savedRule: InterventionRule? = null,
    val savedRuleValidation: RuleValidationResult = RuleValidationResult(),
    val installedApps: List<InstalledAppInfo> = emptyList(),
    val draft: RuleDraftUiState = RuleDraftUiState(),
    val draftValidation: RuleValidationResult = RuleValidationResult(),
    val isSaveEnabled: Boolean = false,
    val session: ChallengeSession? = null,
    val progress: ChallengeProgress? = null,
    val unlockGrant: UnlockGrant? = null,
    val expiredCredit: ExpiredUnlockCredit? = null,
) {
    val canClearRule: Boolean
        get() = savedRule != null || session != null || unlockGrant != null
}

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

    private var hasInitializedDraft = false

    var uiState by mutableStateOf(MainUiState())
        private set

    init {
        refresh()
    }

    fun refresh() {
        val installedApps = installedAppRepository.getLaunchableApps()
        val savedRule = ruleStore.load()
        val savedRuleValidation = savedRule?.let(runtimeValidator::validateSavedRule)
            ?: RuleValidationResult()
        val readiness = permissionStatusRepository.getReadiness()
        val draftBase = when {
            !hasInitializedDraft -> ruleToDraftUiState(savedRule)
            !draftDiffersFrom(uiState.savedRule, uiState.draft) -> ruleToDraftUiState(savedRule)
            else -> uiState.draft
        }
        val draft = reconcileDraftSelections(draftBase, installedApps)
        val draftValidation = validateDraft(draft)

        val usableRule = savedRule?.takeIf { savedRuleValidation.isValid }
        if (usableRule == null) {
            sessionStore.clear()
            unlockGrantStore.clear()
        }

        var session = usableRule?.let { sessionStore.loadForRule(it.ruleId) }
        var progress = session?.let(challengeTracker::calculateProgress)

        if (usableRule != null &&
            session != null &&
            progress?.isComplete == true &&
            session.completedAtEpochMs == null
        ) {
            val completedAt = System.currentTimeMillis()
            sessionStore.markCompleted(completedAt)
            unlockGrantStore.save(
                UnlockGrant(
                    ruleId = usableRule.ruleId,
                    blockedPackage = usableRule.blockedPackage,
                    expiresAtEpochMs = completedAt + usableRule.unlockWindowMinutes * 60_000L,
                ),
            )
            session = sessionStore.loadForRule(usableRule.ruleId)
            progress = session?.let(challengeTracker::calculateProgress)
        }

        val unlockGrant = usableRule?.let {
            unlockGrantStore.loadForRule(
                ruleId = it.ruleId,
                blockedPackage = it.blockedPackage,
            )
        }
        val expiredCredit = usableRule?.let {
            unlockGrantStore.loadExpiredCreditForRule(
                ruleId = it.ruleId,
                blockedPackage = it.blockedPackage,
            )
        }

        hasInitializedDraft = true
        uiState = MainUiState(
            heroStatus = resolveHeroStatus(
                savedRule = savedRule,
                savedRuleValidation = savedRuleValidation,
                readiness = readiness,
                session = session,
                unlockGrant = unlockGrant,
                expiredCredit = expiredCredit,
            ),
            readiness = readiness,
            savedRule = savedRule,
            savedRuleValidation = savedRuleValidation,
            installedApps = installedApps,
            draft = draft,
            draftValidation = draftValidation,
            isSaveEnabled = draftValidation.isValid && draftDiffersFrom(savedRule, draft),
            session = session,
            progress = progress,
            unlockGrant = unlockGrant,
            expiredCredit = expiredCredit,
        )
    }

    fun onBlockedAppSelected(app: InstalledAppInfo) {
        updateDraft(uiState.draft.copy(blockedApp = app.toSelectedApp()))
    }

    fun onBlockedAppCleared() {
        updateDraft(uiState.draft.copy(blockedApp = null))
    }

    fun onControlAppSelected(app: InstalledAppInfo) {
        updateDraft(uiState.draft.copy(controlApp = app.toSelectedApp()))
    }

    fun onControlAppCleared() {
        updateDraft(uiState.draft.copy(controlApp = null))
    }

    fun onRequiredSecondsChanged(value: String) {
        updateDraft(
            uiState.draft.copy(
                requiredSecondsInput = value.filter(Char::isDigit).take(3),
            ),
        )
    }

    fun onUnlockWindowMinutesChanged(value: String) {
        updateDraft(
            uiState.draft.copy(
                unlockWindowMinutesInput = value.filter(Char::isDigit).take(2),
            ),
        )
    }

    fun saveRule() {
        val draft = uiState.draft
        val blockedApp = draft.blockedApp ?: return
        val controlApp = draft.controlApp ?: return
        if (!uiState.isSaveEnabled) return

        ruleStore.save(
            InterventionRuleDraft(
                blockedPackage = blockedApp.packageName,
                blockedAppName = blockedApp.appName,
                controlPackage = controlApp.packageName,
                controlAppName = controlApp.appName,
                requiredSeconds = draft.requiredSecondsInput.toInt(),
                unlockWindowMinutes = draft.unlockWindowMinutesInput.toInt(),
            ),
        )
        sessionStore.clear()
        unlockGrantStore.clear()
        hasInitializedDraft = false
        refresh()
    }

    fun clearRule() {
        ruleStore.clear()
        sessionStore.clear()
        unlockGrantStore.clear()
        hasInitializedDraft = false
        refresh()
    }

    private fun updateDraft(nextDraft: RuleDraftUiState) {
        val draft = reconcileDraftSelections(nextDraft, uiState.installedApps)
        val draftValidation = validateDraft(draft)
        uiState = uiState.copy(
            draft = draft,
            draftValidation = draftValidation,
            isSaveEnabled = draftValidation.isValid && draftDiffersFrom(uiState.savedRule, draft),
        )
    }

    private fun validateDraft(draft: RuleDraftUiState): RuleValidationResult {
        val issues = runtimeValidator.validateDraft(
            RuleDraftValidationInput(
                blockedPackage = draft.blockedApp?.packageName,
                controlPackage = draft.controlApp?.packageName,
                requiredSeconds = draft.requiredSecondsInput.toIntOrNull(),
                unlockWindowMinutes = draft.unlockWindowMinutesInput.toIntOrNull(),
            ),
        ).issues.toMutableSet()

        if (draft.blockedApp != null && !draft.blockedApp.isInstalled) {
            issues += RuleValidationIssue.BLOCKED_APP_NOT_INSTALLED
        }
        if (draft.controlApp != null && !draft.controlApp.isInstalled) {
            issues += RuleValidationIssue.CONTROL_APP_NOT_INSTALLED
        }

        return RuleValidationResult(issues)
    }

    private fun reconcileDraftSelections(
        draft: RuleDraftUiState,
        installedApps: List<InstalledAppInfo>,
    ): RuleDraftUiState {
        return draft.copy(
            blockedApp = reconcileSelection(draft.blockedApp, installedApps),
            controlApp = reconcileSelection(draft.controlApp, installedApps),
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

    private fun ruleToDraftUiState(rule: InterventionRule?): RuleDraftUiState {
        if (rule == null) {
            return RuleDraftUiState()
        }

        return RuleDraftUiState(
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
        )
    }

    private fun resolveHeroStatus(
        savedRule: InterventionRule?,
        savedRuleValidation: RuleValidationResult,
        readiness: PermissionReadiness,
        session: ChallengeSession?,
        unlockGrant: UnlockGrant?,
        expiredCredit: ExpiredUnlockCredit?,
    ): HeroStatus {
        if (savedRule == null) return HeroStatus.NOT_CONFIGURED
        if (!savedRuleValidation.isValid) return HeroStatus.SAVED_RULE_INVALID
        if (unlockGrant != null) return HeroStatus.UNLOCKED
        if (session != null) return HeroStatus.CHALLENGE_IN_PROGRESS
        if (expiredCredit != null) return HeroStatus.CREDITS_EXHAUSTED
        if (!readiness.isFullyReady) return HeroStatus.CONFIGURED_MISSING_PERMISSIONS
        return HeroStatus.READY
    }

    private fun draftDiffersFrom(
        savedRule: InterventionRule?,
        draft: RuleDraftUiState,
    ): Boolean {
        if (savedRule == null) {
            return draft.blockedApp != null ||
                draft.controlApp != null ||
                draft.requiredSecondsInput != AppConfig.DEFAULT_REQUIRED_SECONDS.toString() ||
                draft.unlockWindowMinutesInput != AppConfig.DEFAULT_UNLOCK_WINDOW_MINUTES.toString()
        }

        return savedRule.blockedPackage != draft.blockedApp?.packageName ||
            savedRule.blockedAppName != draft.blockedApp?.appName ||
            savedRule.controlPackage != draft.controlApp?.packageName ||
            savedRule.controlAppName != draft.controlApp?.appName ||
            savedRule.requiredSeconds.toString() != draft.requiredSecondsInput ||
            savedRule.unlockWindowMinutes.toString() != draft.unlockWindowMinutesInput
    }
}

private fun InstalledAppInfo.toSelectedApp(): SelectedApp {
    return SelectedApp(
        packageName = packageName,
        appName = label,
        isInstalled = true,
    )
}
