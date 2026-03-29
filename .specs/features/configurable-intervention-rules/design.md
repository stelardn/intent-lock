# Configurable Intervention Rules MVP Design

## Overview

This design turns the validated spike into a single-rule MVP with a real configuration UI.

The implementation keeps the proven Android flow intact:

- `AccessibilityService` remains the interception entry point
- `UsageStatsManager` remains the challenge verification source
- local persistence remains lightweight and synchronous for low-risk device behavior

The main change is replacing `AppConfig` as the source of truth for blocked/control apps and timing with one persisted user-defined rule.

## Scope

This design covers:

- one configurable intervention rule
- one main configuration/status screen
- one app picker flow reused for blocked and control app selection
- persistence of rule, challenge session, and unlock grant
- readiness and active-state visibility in the UI
- stale-state protection when the rule changes or becomes invalid

This design does not cover:

- multiple simultaneous rules
- challenge history
- cloud sync
- anti-circumvention hardening

## Product Decisions

### Single source of truth

The saved rule becomes the only runtime source for:

- which package should be intercepted
- which package should be launched as the control app
- how long the challenge lasts
- how long the unlock window lasts

### MVP persistence choice

Use `SharedPreferences` for the rule store in the MVP.

Reasoning:

- the current spike already uses synchronous preference-backed stores successfully
- `AccessibilityService` and `LockActivity` benefit from immediate synchronous reads
- this keeps the first product pass low-risk on device

`DataStore` remains a future refactor option after the MVP is stable.

### Supported value ranges

Proposed MVP limits:

- challenge duration: `10` to `300` seconds
- unlock window: `1` to `60` minutes

Reasoning:

- wide enough for real use
- narrow enough to validate and explain simply
- keeps input UX straightforward

## Architecture

### New domain model

```kotlin
data class InterventionRule(
    val ruleId: String,
    val blockedPackage: String,
    val blockedAppName: String,
    val controlPackage: String,
    val controlAppName: String,
    val requiredSeconds: Int,
    val unlockWindowMinutes: Int,
    val savedAtEpochMs: Long,
)
```

`ruleId` changes on every successful save. It is used to invalidate stale session and unlock data safely.

### Existing models to extend

`ChallengeSession` should add:

- `ruleId: String`

`UnlockGrant` should add:

- `ruleId: String`

This lets runtime code reject leftover session/unlock state that belongs to an older rule.

### New components

#### `InterventionRuleStore`

Responsibilities:

- persist the single saved rule
- load the current rule
- clear the current rule
- validate structural completeness before returning a usable rule

Storage format:

- one `SharedPreferences` file
- one key per field for easy debugging and low migration complexity

#### `InstalledAppRepository`

Responsibilities:

- return launchable installed apps for the picker
- expose app label + package name
- verify whether a saved package is still installed
- fetch a launch intent for the saved control app

App discovery should use launcher-visible apps instead of hardcoded package queries.

Preferred query:

- `Intent.ACTION_MAIN`
- `Intent.CATEGORY_LAUNCHER`

This avoids `QUERY_ALL_PACKAGES` and removes the spike-era dependency on Instagram/Duolingo-specific manifest entries.

#### `PermissionStatusRepository`

Responsibilities:

- check whether Accessibility permission is enabled for `SocialAccessibilityService`
- check whether Dados de uso is granted

#### `RuleRuntimeValidator`

Responsibilities:

- reject rules where blocked/control apps are the same
- reject values outside supported ranges
- mark a stored rule invalid if either saved package is no longer installed
- clear or ignore stale session/unlock state when `ruleId` no longer matches

#### `MainViewModel`

Responsibilities:

- load rule, permissions, installed apps, session, unlock grant, and progress into one UI state
- manage edit state separately from the last saved state
- save or clear the rule
- refresh state on resume
- expose validation errors for the UI

This is the first place where adding lifecycle dependencies is worth it because the screen is now stateful enough to justify a proper state holder.

## UI Design

### Main screen structure

`MainActivity` becomes the MVP home and configuration screen.

Sections:

1. Hero/status card
2. Readiness card
3. Rule configuration card
4. Current activity card

#### 1. Hero/status card

Shows one primary status:

- `Not configured`
- `Configured, missing permissions`
- `Ready`
- `Challenge in progress`
- `Unlocked until <time>`
- `Saved rule invalid`

This satisfies the requirement that the app clearly communicates whether it is ready.

#### 2. Readiness card

Rows:

- Accessibility: `Ready` or `Required`
- Dados de uso: `Ready` or `Required`

Actions:

- `Open Accessibility Settings`
- `Open Dados de uso Settings`

Behavior:

- missing permissions never delete the saved rule
- readiness is derived, not persisted

#### 3. Rule configuration card

Fields:

- blocked app selector
- control app selector
- challenge duration input
- unlock window input

Actions:

- `Save rule`
- `Clear rule`

Validation:

- both apps must be selected
- apps must differ
- duration must be within range
- unlock window must be within range

Save button behavior:

- disabled when the draft is invalid
- enabled when the draft is valid and changed

#### 4. Current activity card

When there is an active session:

- show blocked app name
- show control app name
- show tracked seconds vs required seconds
- show whether Dados de uso is preventing progress calculation

When there is an active unlock grant:

- show blocked app name
- show unlock expiry time
- show remaining minutes/seconds

This keeps the current debug-like visibility from the spike, but makes it product-oriented.

### App picker flow

Use one reusable picker component for both blocked and control app fields.

Recommended interaction:

- tap selector row
- open full-screen picker or modal bottom sheet
- show search field
- show app icon, app label, and package name
- return selected app to the form

The picker only includes launchable apps.

This keeps the rule usable for normal installed apps and prevents selecting packages that cannot be opened by the lock flow.

### Lock screen updates

`LockActivity` remains intentionally focused, but it should become rule-driven.

Content changes:

- show blocked app display name instead of hardcoded package
- show control app display name instead of hardcoded package
- show required challenge duration from the saved rule
- handle missing or invalid rule with a recovery state instead of assuming config exists

Recovery state:

- message: configuration missing or invalid
- action: `Back to setup`

### Simple wireframe

```text
Intent Lock
[ Ready ] or [ Missing setup ] or [ Unlocked until 14:32 ]

Readiness
- Accessibility: Ready / Required
- Dados de uso: Ready / Required
[ Open Accessibility Settings ]
[ Open Dados de uso Settings ]

Your rule
- Blocked app: Instagram
- Control app: Duolingo
- Challenge duration: 15 seconds
- Unlock window: 10 minutes
[ Save rule ]
[ Clear rule ]

Current state
- Challenge: 8s / 15s
- Unlock: 6m 14s remaining
```

## Runtime Flow

### Save flow

1. User selects blocked app, control app, challenge duration, and unlock window.
2. UI validates draft locally.
3. `MainViewModel` creates a new `ruleId`.
4. `InterventionRuleStore` saves the rule.
5. `ChallengeSessionStore` clears any old session.
6. `UnlockGrantStore` clears any old grant.
7. Main screen reloads derived readiness and state.

This aggressive reset keeps rule replacement safe in the MVP.

### Interception flow

1. `SocialAccessibilityService` receives a foreground/window event.
2. Service ignores own package and irrelevant event types.
3. Service loads the current saved rule.
4. If there is no valid rule, return without intercepting.
5. If event package does not match `rule.blockedPackage`, return.
6. If an unlock grant exists for the same `ruleId` and blocked package, skip interception.
7. Otherwise launch `LockActivity`.

### Challenge start flow

1. `LockActivity` loads the saved rule.
2. If the rule is missing or invalid, show recovery UI.
3. If the control app launch intent is unavailable, show error and route back to setup.
4. Save a `ChallengeSession` using:
   - `ruleId`
   - blocked package
   - control package
   - required seconds
   - started timestamp
5. Launch the control app.

### Challenge completion flow

1. `MainActivity` loads the active session.
2. `UsageStatsChallengeTracker` calculates progress for `session.controlPackage`.
3. If tracked time reaches `session.requiredSeconds`, mark the session completed.
4. Load the current rule.
5. If `session.ruleId != rule.ruleId`, clear the stale session and stop.
6. Create an `UnlockGrant` with:
   - `ruleId`
   - blocked package
   - expiry = `completedAt + unlockWindowMinutes`
7. Main screen shows unlock status.

### Rule clear flow

1. User taps `Clear rule`.
2. App clears the saved rule.
3. App clears session and unlock stores.
4. `SocialAccessibilityService` stops intercepting new launches because there is no valid rule.

## Invalid And Stale State Handling

### Missing installed apps

On every main-screen refresh and lock-screen load:

- if blocked package is no longer installed, mark rule invalid
- if control package is no longer installed, mark rule invalid

Behavior:

- preserve the stored values for display
- block interception and challenge start
- show a clear prompt to reconfigure

### Rule changed during active state

Primary protection:

- clear session + grant on every successful rule save

Secondary protection:

- `ruleId` mismatch causes session/grant to be ignored and cleared on load

### Expired unlock

Existing behavior remains:

- `UnlockGrantStore.isUnlocked()` clears expired grants automatically

### App restart during active challenge

Existing behavior remains with one change:

- restored session is only used if `session.ruleId` still matches the current saved rule

## Android Manifest Changes

Replace the hardcoded package visibility entries with a generic launcher query.

Target shape:

```xml
<queries>
    <intent>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent>
</queries>
```

This supports:

- building the app picker
- resolving launch intents for chosen apps

## Implementation Plan

1. Add model/store/runtime validation for `InterventionRule`.
2. Extend session and unlock models with `ruleId`.
3. Replace `AppConfig` reads in `SocialAccessibilityService` and `LockActivity`.
4. Rebuild `MainActivity` around a configuration + status UI.
5. Add generic app picker and package validation.
6. Replace manifest hardcodes with launcher query visibility.
7. Validate the full flow on a physical device.

## Testing Strategy

### Unit-level checks

- rule validation for same-app rejection
- duration/window range validation
- rule load returning null for incomplete data
- stale `ruleId` session/grant rejection
- invalid saved packages being detected

### Manual device checks

1. Save a valid rule and confirm it survives app restart.
2. Open the selected blocked app and confirm lock interception occurs.
3. Start the challenge and confirm the selected control app launches.
4. Spend enough time in the control app and confirm unlock is granted.
5. Reopen the blocked app during the unlock window and confirm interception is skipped.
6. Let the unlock expire and confirm interception resumes.
7. Change the rule while a session or unlock is active and confirm stale state is not reused.
8. Uninstall the selected control or blocked app and confirm the UI reports an invalid rule.

## Requirement Traceability

| Requirement | Design coverage |
| --- | --- |
| RULE-01 | Main screen configuration card loads and displays the saved rule |
| RULE-02 | App picker + validator enforce different blocked/control apps |
| RULE-03 | `InterventionRuleStore` persists the rule across restart |
| RULE-04 | Hero/status + readiness card communicate missing configuration |
| RULE-05 | `SocialAccessibilityService` intercepts using the saved blocked app |
| RULE-06 | `LockActivity` creates a session from the saved rule |
| RULE-07 | Completion flow grants unlock from tracked control-app progress |
| RULE-08 | Unlock grant bypasses interception until expiry |
| RULE-09 | Readiness card surfaces missing Accessibility permission |
| RULE-10 | Readiness card surfaces missing Dados de uso permission |
| RULE-11 | Current activity card shows active challenge or unlock state |
| RULE-12 | Installed-app validation marks the saved rule invalid |
| RULE-13 | Clear/replace rule flow stops using the old rule |
| RULE-14 | `ruleId` + proactive clearing prevent stale session/unlock reuse |

## Assumptions

- The MVP remains single-device and local-first.
- Only launchable apps are valid picker candidates.
- A successful rule save is allowed even if permissions are still missing.
- Replacing the rule clears current challenge/unlock state by design in the MVP.
