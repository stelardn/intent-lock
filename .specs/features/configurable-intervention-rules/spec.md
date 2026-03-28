# Configurable Intervention Rules Specification

## Problem Statement

The validated spike proves the core interception flow works, but it is still tied to hardcoded packages and fixed durations in `AppConfig`. To turn Intent Lock into a usable MVP, the user must be able to configure a real intervention rule without editing source code while preserving the validated behavior on a physical Android device.

## Goals

- [ ] Let the user configure one blocked app, one control app, a challenge duration, and an unlock window from the app UI.
- [ ] Persist the selected rule locally so it survives process death and device restarts.
- [ ] Reuse the saved rule across interception, challenge tracking, and unlock decisions without relying on hardcoded app packages.

## Out of Scope

Explicitly excluded. Documented to prevent scope creep.

| Feature | Reason |
| --- | --- |
| Multiple blocked/control rules | Better handled after the single-rule MVP is stable |
| Challenge history or streaks | Not required to replace the spike hardcodes |
| Cloud sync or account management | Project remains local-first in v1 |
| Anti-circumvention hardening | The MVP only needs coarse challenge verification |
| Notification-only fallback flow | Not part of the primary post-spike product path |

---

## User Stories

### P1: Configure A Single Intervention Rule ⭐ MVP

**User Story**: As a user, I want to choose the blocked app, control app, challenge time, and unlock window so that Intent Lock matches my own habit instead of the spike defaults.

**Why P1**: Without this, the app is still a developer-only spike that requires source changes to be usable.

**Acceptance Criteria**:

1. WHEN the user opens the configuration screen THEN the system SHALL show the currently saved blocked app, control app, challenge duration, and unlock window.
2. WHEN the user selects a blocked app and a control app from installed apps THEN the system SHALL allow saving the rule only if the apps are different.
3. WHEN the user enters a valid challenge duration and unlock window THEN the system SHALL persist the values locally and reload them after app restart.
4. WHEN no valid rule is saved THEN the system SHALL clearly indicate that interception is not yet fully configured.

**Independent Test**: Configure a rule, close the app completely, reopen it, and confirm the same selections and durations are still shown.

---

### P1: Enforce The Saved Rule End-To-End ⭐ MVP

**User Story**: As a user, I want the saved rule to control interception and unlocking so that the product behavior matches the configuration I created.

**Why P1**: The app only becomes a product when the stored configuration drives the real flow instead of `AppConfig`.

**Acceptance Criteria**:

1. WHEN the blocked app from the saved rule is opened and no valid unlock exists THEN the system SHALL launch the lock flow using the saved rule values.
2. WHEN the user starts the challenge THEN the system SHALL create a session linked to the saved blocked app, control app, and required duration.
3. WHEN tracked foreground time in the saved control app reaches the configured threshold THEN the system SHALL mark the session completed and create an unlock grant for the saved blocked app.
4. WHEN the unlock grant is still valid THEN the system SHALL not intercept the blocked app again until the configured unlock window expires.

**Independent Test**: Save a rule, open the chosen blocked app, complete the configured challenge time in the chosen control app, and confirm the blocked app opens normally during the unlock window.

---

### P2: Explain Readiness And Current State

**User Story**: As a user, I want to understand whether the app is ready to intercept and what state my current rule is in so that setup problems are obvious.

**Why P2**: The technical model depends on permissions and persisted state that can otherwise feel opaque.

**Acceptance Criteria**:

1. WHEN Accessibility permission is missing THEN the system SHALL show that interception is unavailable and provide a path back to settings.
2. WHEN Usage Access is missing THEN the system SHALL show that challenge verification is unavailable and explain the limitation.
3. WHEN a challenge session or unlock grant is active THEN the system SHALL surface the current progress or unlock expiry for the saved rule.

**Independent Test**: Toggle permissions and run a challenge cycle, then verify the app shows the correct readiness and state on the main screen.

---

### P3: Recover From Invalid Or Stale Saved State

**User Story**: As a user, I want the app to recover gracefully if the saved rule becomes invalid so that I do not get stuck in a broken configuration.

**Why P3**: Installed apps and permissions can change outside the app after the rule is saved.

**Acceptance Criteria**:

1. WHEN the saved blocked or control app is no longer installed THEN the system SHALL mark the rule as invalid and prompt the user to reconfigure it.
2. WHEN the user clears or replaces the saved rule THEN the system SHALL stop using the previous rule for new interceptions.
3. WHEN stale session or unlock data no longer matches the saved rule THEN the system SHALL ignore or clear the stale data safely.

**Independent Test**: Save a rule, simulate an invalid state by clearing or replacing the rule, and confirm the app does not keep enforcing the outdated configuration.

---

## Edge Cases

- WHEN the user selects the same app as both blocked and control THEN the system SHALL reject the configuration with a clear validation message.
- WHEN the user attempts to save a duration or unlock window outside the supported range THEN the system SHALL block saving and show the expected limits.
- WHEN permissions are missing during an otherwise valid configuration THEN the system SHALL preserve the rule but mark the product as not ready.
- WHEN the app restarts during an active challenge THEN the system SHALL restore or safely recover session state instead of silently losing context.
- WHEN the user changes the saved rule while a session or unlock is active THEN the system SHALL prevent stale session or unlock data from being applied to the new rule.

---

## Requirement Traceability

Each requirement gets a unique ID for tracking across design, tasks, and validation.

| Requirement ID | Story | Phase | Status |
| --- | --- | --- | --- |
| RULE-01 | P1: Configure A Single Intervention Rule | Design | Pending |
| RULE-02 | P1: Configure A Single Intervention Rule | Design | Pending |
| RULE-03 | P1: Configure A Single Intervention Rule | Design | Pending |
| RULE-04 | P1: Configure A Single Intervention Rule | Design | Pending |
| RULE-05 | P1: Enforce The Saved Rule End-To-End | Design | Pending |
| RULE-06 | P1: Enforce The Saved Rule End-To-End | Design | Pending |
| RULE-07 | P1: Enforce The Saved Rule End-To-End | Design | Pending |
| RULE-08 | P1: Enforce The Saved Rule End-To-End | Design | Pending |
| RULE-09 | P2: Explain Readiness And Current State | Design | Pending |
| RULE-10 | P2: Explain Readiness And Current State | Design | Pending |
| RULE-11 | P2: Explain Readiness And Current State | Design | Pending |
| RULE-12 | P3: Recover From Invalid Or Stale Saved State | Design | Pending |
| RULE-13 | P3: Recover From Invalid Or Stale Saved State | Design | Pending |
| RULE-14 | P3: Recover From Invalid Or Stale Saved State | Design | Pending |

**ID format:** `[CATEGORY]-[NUMBER]`

**Coverage:** 14 total, 0 mapped to tasks, 14 unmapped pending the first design/task pass.

---

## Success Criteria

How we know the feature is successful:

- [ ] A user can configure the rule entirely from the app UI without editing source code.
- [ ] The saved rule is still applied after app restart and drives interception, challenge completion, and unlock behavior.
- [ ] The main screen makes missing permissions and current rule state understandable enough for day-to-day use on the target device.
