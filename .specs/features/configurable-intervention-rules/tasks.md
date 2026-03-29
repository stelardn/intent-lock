# Configurable Intervention Rules Tasks

## Task Format

Each task follows the `tlc-spec-driven` phase-3 structure:

- What: exact deliverable
- Where: primary files or modules to change
- Depends on: prerequisites that must land first
- Reuses: existing code or patterns to leverage
- Done when: verifiable completion criteria
- Covers: requirement IDs from `spec.md`

## Execution Plan

```text
T001 -> T002 -> T004 -> T005 -> T006 -> T007 -> T009 -> T010
   \-> T003 -----^             \-------> T008 -----^
```

Parallel opportunities after dependencies clear:

- `T002` and `T003` can run in parallel after `T001`
- `T007` and `T008` can run in parallel after `T006` and `T003`

## Tasks

### T001 - Create configurable rule model and persistence

- Status: Completed
- What: Add the single-rule domain model, persistence store, save-time ID generation, and basic draft validation primitives for challenge duration and unlock window ranges.
- Where:
  - `app/src/main/java/com/larissa/socialcontrol/InterventionRule.kt`
  - `app/src/main/java/com/larissa/socialcontrol/InterventionRuleStore.kt`
  - `app/src/main/java/com/larissa/socialcontrol/AppConfig.kt` or a replacement constant holder for shared limits
- Depends on: None
- Reuses:
  - `ChallengeSessionStore` preference-backed persistence pattern
  - existing synchronous read/write approach used by the spike
- Done when:
  - a saved rule can be written, loaded, and cleared through one store
  - incomplete persisted rule data loads as `null` instead of a partially usable rule
  - challenge duration is constrained to `10..300` seconds
  - unlock window is constrained to `1..60` minutes
  - every successful save produces a new `ruleId`
- Covers: `RULE-01`, `RULE-03`, `RULE-04`

### T002 - Extend challenge and unlock state with `ruleId`

- Status: Completed
- What: Add `ruleId` to `ChallengeSession` and `UnlockGrant`, update both stores, and make stale-state handling possible without depending on the old hardcoded rule.
- Where:
  - `app/src/main/java/com/larissa/socialcontrol/ChallengeSessionStore.kt`
  - `app/src/main/java/com/larissa/socialcontrol/UnlockGrantStore.kt`
- Depends on: `T001`
- Reuses:
  - existing session/grant persistence structure
  - current `markCompleted()` and `isUnlocked()` behavior
- Done when:
  - both models persist and restore `ruleId`
  - session and grant data can be safely compared against the currently saved rule
  - loading helpers expose enough information for later stale-data rejection
- Covers: `RULE-06`, `RULE-08`, `RULE-13`, `RULE-14`

### T003 - Add installed-app and permission repositories

- Status: Completed
- What: Create repositories for launcher-visible installed apps and permission readiness, and replace hardcoded manifest visibility entries with a generic launcher query.
- Where:
  - `app/src/main/java/com/larissa/socialcontrol/InstalledAppRepository.kt`
  - `app/src/main/java/com/larissa/socialcontrol/PermissionStatusRepository.kt`
  - `app/src/main/AndroidManifest.xml`
- Depends on: `T001`
- Reuses:
  - `packageManager.getLaunchIntentForPackage()`
  - `UsageStatsChallengeTracker.hasUsageAccess()`
- Done when:
  - the app can list launchable installed apps with label and package name
  - the app can verify whether a stored blocked or control package is still installed
  - the app can resolve a launch intent for the chosen control app
  - Accessibility readiness and Usage Access readiness are available through one repository surface
  - manifest `queries` use `ACTION_MAIN` + `CATEGORY_LAUNCHER` instead of Instagram/Duolingo-specific package entries
- Covers: `RULE-02`, `RULE-09`, `RULE-10`, `RULE-12`

### T004 - Add runtime validation and stale-state protection

- Status: Completed
- What: Implement runtime validation for same-app rejection, value ranges, missing installed packages, and `ruleId` mismatch cleanup/ignore behavior.
- Where:
  - `app/src/main/java/com/larissa/socialcontrol/RuleRuntimeValidator.kt`
  - supporting validation or coordinator code near the rule/session/grant stores
- Depends on: `T001`, `T002`, `T003`
- Reuses:
  - requirement limits from the feature design
  - the new repositories and stores from `T001` to `T003`
- Done when:
  - blocked app equals control app is rejected with a machine-readable validation result
  - out-of-range duration or unlock values are rejected consistently
  - a saved rule becomes invalid when either stored app is no longer installed
  - session and unlock data with a non-matching `ruleId` are ignored or cleared safely
- Covers: `RULE-02`, `RULE-12`, `RULE-13`, `RULE-14`

### T005 - Make interception and lock flow rule-driven

- Status: Completed
- What: Refactor `SocialAccessibilityService` and `LockActivity` to load the saved rule instead of `AppConfig`, keep debounce behavior, and show a recovery path when the rule is missing or invalid.
- Where:
  - `app/src/main/java/com/larissa/socialcontrol/SocialAccessibilityService.kt`
  - `app/src/main/java/com/larissa/socialcontrol/LockActivity.kt`
  - `app/src/main/java/com/larissa/socialcontrol/AppConfig.kt` if reduced to non-rule constants only
- Depends on: `T002`, `T003`, `T004`
- Reuses:
  - existing debounce logic
  - existing lock-screen navigation flow
- Done when:
  - the service intercepts only `rule.blockedPackage`
  - a valid unlock grant for the same rule bypasses interception
  - `LockActivity` renders the saved blocked/control app names and required duration
  - starting the challenge saves a `ChallengeSession` with `ruleId`, blocked package, control package, and required seconds
  - missing rule, invalid rule, or missing launch intent show a recovery state instead of assuming hardcoded packages exist
- Covers: `RULE-05`, `RULE-06`, `RULE-08`, `RULE-12`, `RULE-13`, `RULE-14`

### T006 - Introduce `MainViewModel` and aggregated screen state

- Status: Completed
- What: Add the first real screen state holder that combines saved rule, editable draft, permission readiness, active session, unlock grant, and challenge progress, including save/clear/refresh actions.
- Where:
  - `app/build.gradle.kts`
  - `app/src/main/java/com/larissa/socialcontrol/MainViewModel.kt`
  - supporting UI-state models in the same package or a `ui/` subpackage
- Depends on: `T001`, `T002`, `T003`, `T004`
- Reuses:
  - current refresh-on-resume behavior from `MainActivity`
  - `UsageStatsChallengeTracker`
- Done when:
  - lifecycle/viewmodel dependencies required by the MVP screen are present
  - `MainViewModel` exposes one observable UI state for the home/configuration screen
  - saving a rule creates a new `ruleId` and clears old session/unlock data
  - clearing a rule removes the saved rule plus current session and unlock state
  - refresh on resume recalculates readiness, rule validity, progress, and unlock state
- Covers: `RULE-01`, `RULE-03`, `RULE-04`, `RULE-09`, `RULE-10`, `RULE-11`, `RULE-13`, `RULE-14`

### T007 - Rebuild `MainActivity` as the MVP home and configuration screen

- Status: Completed
- What: Replace the spike screen with the four-section MVP UI: hero/status, readiness, rule configuration, and current activity, all wired to `MainViewModel`.
- Where:
  - `app/src/main/java/com/larissa/socialcontrol/MainActivity.kt`
  - optional extracted composables under `app/src/main/java/com/larissa/socialcontrol/ui/`
  - `app/src/main/res/values/strings.xml`
- Depends on: `T006`
- Reuses:
  - current Compose Material 3 screen scaffolding
  - existing settings intents for Accessibility and Usage Access
- Done when:
  - the hero card shows one primary state: `Not configured`, `Configured, missing permissions`, `Ready`, `Challenge in progress`, `Unlocked until <time>`, or `Saved rule invalid`
  - the readiness card shows Accessibility and Usage Access readiness with settings actions
  - the rule card shows blocked app, control app, challenge duration, unlock window, `Save rule`, and `Clear rule`
  - the save action is disabled unless the draft is both valid and changed
  - the current activity card shows progress or unlock information when applicable
- Covers: `RULE-01`, `RULE-04`, `RULE-09`, `RULE-10`, `RULE-11`, `RULE-12`

### T008 - Add a reusable app picker with search

- Status: Completed
- What: Build one reusable app picker flow for selecting blocked and control apps, with search and clear display of label, package name, and icon.
- Where:
  - new picker composables under `app/src/main/java/com/larissa/socialcontrol/ui/`
  - `app/src/main/java/com/larissa/socialcontrol/MainActivity.kt` or the extracted rule form module
- Depends on: `T003`, `T006`
- Reuses:
  - `InstalledAppRepository`
  - Compose modal or full-screen selection patterns already used by the app
- Done when:
  - both rule fields use the same picker component
  - only launchable apps are displayed
  - the picker supports search by app label or package
  - the form prevents selecting the same app for blocked and control without relying on manual package entry
  - a previously saved selection round-trips back into the form correctly
- Covers: `RULE-01`, `RULE-02`, `RULE-12`

### T009 - Wire challenge completion and unlock lifecycle to the saved rule

- Status: Completed
- What: Move challenge completion and unlock-grant creation fully onto the saved rule, including `ruleId` checks, configured unlock window minutes, and expired grant cleanup.
- Where:
  - `app/src/main/java/com/larissa/socialcontrol/MainViewModel.kt`
  - `app/src/main/java/com/larissa/socialcontrol/UnlockGrantStore.kt`
  - `app/src/main/java/com/larissa/socialcontrol/UsageStatsChallengeTracker.kt`
  - `app/src/main/java/com/larissa/socialcontrol/MainActivity.kt`
- Depends on: `T002`, `T004`, `T006`, `T007`
- Reuses:
  - existing usage-stats progress calculation
  - existing unlock-expiry cleanup behavior
- Done when:
  - progress calculation uses the active session tied to the saved rule
  - completing the required control-app time marks the session complete once
  - unlock creation uses `unlockWindowMinutes` from the saved rule
  - expired grants are cleared automatically
  - stale session or unlock data with a different `ruleId` never drive current UI or runtime behavior
- Covers: `RULE-07`, `RULE-08`, `RULE-11`, `RULE-14`

### T010 - Add focused tests and run the device validation pass

- Status: In Progress
- What: Add unit tests for the new rule/state logic and execute the manual device checklist covering the full end-to-end flow and invalid-state recovery.
- Where:
  - `app/src/test/java/com/larissa/socialcontrol/`
  - feature validation notes captured in the active work log if the project keeps one
- Depends on: `T001`, `T002`, `T003`, `T004`, `T005`, `T006`, `T007`, `T008`, `T009`
- Reuses:
  - edge cases from `spec.md`
  - manual checklist from `design.md`
- Done when:
  - unit tests cover same-app rejection, duration/window bounds, incomplete rule load, stale `ruleId` rejection, and missing-installed-app detection
  - manual validation covers save, restart persistence, interception, control-app launch, completion, unlock bypass, unlock expiry, rule replacement, and app uninstall invalidation
  - each `RULE-01` through `RULE-14` has at least one task and at least one verification path
- Covers: `RULE-01`, `RULE-02`, `RULE-03`, `RULE-04`, `RULE-05`, `RULE-06`, `RULE-07`, `RULE-08`, `RULE-09`, `RULE-10`, `RULE-11`, `RULE-12`, `RULE-13`, `RULE-14`

## Requirement Coverage Summary

| Requirement | Covered by |
| --- | --- |
| `RULE-01` | `T001`, `T006`, `T007`, `T008`, `T010` |
| `RULE-02` | `T003`, `T004`, `T008`, `T010` |
| `RULE-03` | `T001`, `T006`, `T010` |
| `RULE-04` | `T001`, `T006`, `T007`, `T010` |
| `RULE-05` | `T005`, `T010` |
| `RULE-06` | `T002`, `T005`, `T010` |
| `RULE-07` | `T009`, `T010` |
| `RULE-08` | `T002`, `T005`, `T009`, `T010` |
| `RULE-09` | `T003`, `T006`, `T007`, `T010` |
| `RULE-10` | `T003`, `T006`, `T007`, `T010` |
| `RULE-11` | `T006`, `T007`, `T009`, `T010` |
| `RULE-12` | `T003`, `T004`, `T005`, `T007`, `T008`, `T010` |
| `RULE-13` | `T002`, `T004`, `T005`, `T006`, `T010` |
| `RULE-14` | `T002`, `T004`, `T005`, `T006`, `T009`, `T010` |

## Notes

- The current worktree has `spec.md` and `.specs/project/*` deleted, so this task plan is aligned to the feature `design.md` in the worktree and the last committed `spec.md` from `HEAD` without restoring deleted files.
- The tasks intentionally keep the MVP single-rule and `SharedPreferences`-backed, matching the current design scope.
- Execution update on 2026-03-29:
  - `T001` through `T009` have been implemented in the worktree.
  - `T010` is partially complete: focused unit tests were added under `app/src/test/java/com/larissa/socialcontrol/` and `.\gradlew.bat testDebugUnitTest` passed.
  - The remaining open item is the manual device validation pass from `T010` covering end-to-end interception, unlock expiry, rule replacement, and app uninstall invalidation.
