# Browser Website Blocking Tasks

## Task Format

Each task follows the `tlc-spec-driven` phase-3 structure:

- What: exact deliverable
- Where: primary files or modules to change
- Depends on: prerequisites that must land first
- Reuses: existing code or patterns to leverage
- Done when: verifiable completion criteria
- Covers: requirement IDs from `spec.md`

## Execution Notes

This feature should begin only after the ongoing multi-rule navigation work is stable enough that:

- rules are stored and rendered as a collection
- the shared create/edit flow exists
- dashboard and rules tabs can display mixed target summaries

## Execution Plan

```text
T001 -> T002 -> T004 -> T005 -> T006 -> T007 -> T009
   \-> T003 -----^      \-------> T008 -----^
```

Parallel opportunities after dependencies clear:

- `T002` and `T003` can run in parallel after `T001`
- `T007` and `T008` can run in parallel after `T006`

## Tasks

### T001 - Extend the multi-rule model for website targets

- Status: Pending
- What: Expand the post-multi-rule rule model, persistence schema, and serialization logic to support `App` and `Website` protected targets in one shared rule system.
- Where:
  - multi-rule domain model files under `app/src/main/java/com/larissa/socialcontrol/`
  - the rule persistence store introduced by the multi-rule work
  - any rule mappers or DTOs used by the rules list and editor
- Depends on:
  - completion of the multi-rule navigation and rule-management foundation
- Reuses:
  - the existing rule lifecycle with `ruleId`, `isEnabled`, challenge duration, and unlock window
- Done when:
  - a rule can represent either an app target or a website target
  - website targets persist browser package, browser name, normalized host, and display host
  - stored website rules round-trip without losing information
- Covers: `WEB-01`, `WEB-04`

### T002 - Add browser support registry and host normalization

- Status: Pending
- What: Introduce a support registry for explicitly supported browsers plus host normalization and validation helpers for website rules.
- Where:
  - new browser support module(s) under `app/src/main/java/com/larissa/socialcontrol/`
  - rule validation helpers near the existing runtime validator
- Depends on: `T001`
- Reuses:
  - installed-app lookup patterns already used for control apps
  - current validation style and machine-readable validation issues
- Done when:
  - the app can list supported browsers for the rule form
  - host input such as `https://www.instagram.com` normalizes into the persisted host format
  - invalid or blank host input is rejected consistently
  - missing or unsupported browsers can be identified for saved rules
- Covers: `WEB-02`, `WEB-03`, `WEB-09`, `WEB-11`

### T003 - Add browser-aware rule validation and duplicate prevention

- Status: Pending
- What: Extend rule validation to support website targets, prevent duplicate browser+host rules, and preserve deterministic runtime rule resolution.
- Where:
  - `RuleRuntimeValidator` or its post-multi-rule equivalent
  - rule-form validation state models
- Depends on: `T001`
- Reuses:
  - existing validation result patterns
  - current duplicate-prevention behavior for app targets, if introduced by the multi-rule work
- Done when:
  - website rules cannot save without a supported browser
  - website rules cannot save without a valid normalized host
  - two website rules cannot target the same normalized host in the same browser
  - overlapping invalid configurations are caught before runtime
- Covers: `WEB-02`, `WEB-03`, `WEB-15`

### T004 - Build the Chrome host matcher abstraction

- Status: Pending
- What: Add a browser host matcher abstraction and implement the first Chrome-specific matcher that resolves the active host from accessibility data.
- Where:
  - new matcher abstractions under `app/src/main/java/com/larissa/socialcontrol/`
  - `SocialAccessibilityService.kt`
- Depends on: `T002`, `T003`
- Reuses:
  - the existing accessibility-event filtering entry point
  - current service-level logging and debounce structure where appropriate
- Done when:
  - the service can request a matcher for Chrome
  - the matcher returns `Matched`, `NoHostFound`, or `Ambiguous`
  - runtime matching relies on the normalized host instead of raw text guesses
- Covers: `WEB-05`, `WEB-10`

### T005 - Extend runtime interception, debounce, and unlock resolution

- Status: Pending
- What: Update runtime rule resolution so website rules can trigger the shared lock flow, with rule-based debounce and website-rule unlock matching.
- Where:
  - `app/src/main/java/com/larissa/socialcontrol/SocialAccessibilityService.kt`
  - challenge session and unlock grant coordination code
- Depends on: `T001`, `T004`
- Reuses:
  - the existing lock-launch flow
  - current session/unlock lifecycle tied to `ruleId`
- Done when:
  - foreground Chrome events can resolve to one website rule by normalized host
  - a valid unlock grant for the same website rule bypasses interception
  - repeated browser events for the same rule do not create a tight lock loop
  - ambiguous or unresolved host state does not trigger interception
- Covers: `WEB-05`, `WEB-07`, `WEB-08`, `WEB-10`, `WEB-16`

### T006 - Update create/edit rule UX for mixed target types

- Status: Pending
- What: Extend the shared rule editor so users can create or edit app and website rules from one flow, including browser selection and host input.
- Where:
  - the rule create/edit screen introduced by the multi-rule navigation work
  - shared UI-state models and validation messaging
  - `app/src/main/res/values/strings.xml`
- Depends on: `T002`, `T003`
- Reuses:
  - the existing app picker for control apps
  - the shared multi-rule create/edit route
- Done when:
  - the form lets the user choose `App` or `Site`
  - website mode shows supported browser selection and host input
  - validation feedback is clear and blocks invalid save attempts
  - saved website rules reopen correctly in edit mode
- Covers: `WEB-01`, `WEB-02`, `WEB-03`, `WEB-12`

### T007 - Update rules list, dashboard summaries, and lock copy

- Status: Pending
- What: Adapt mixed-target presentation across the rules list, dashboard summaries, and lock screen so website rules read naturally.
- Where:
  - rules-tab composables introduced by the multi-rule navigation work
  - dashboard/home composables
  - `LockActivity.kt`
  - `app/src/main/res/values/strings.xml`
- Depends on: `T001`, `T006`
- Reuses:
  - existing status chips and rule card layout patterns
  - the shared lock screen flow
- Done when:
  - website rules show host and browser metadata in the rules list
  - dashboard summaries represent both apps and websites clearly
  - the lock screen copy adapts to website targets without a separate activity
- Covers: `WEB-04`, `WEB-06`, `WEB-12`, `WEB-13`

### T008 - Invalidate stale or unsupported website rules safely

- Status: Pending
- What: Add invalid-state handling for removed browsers, unsupported browser packages, stale host data, and other unusable website-rule states.
- Where:
  - rule loading and validation code
  - rules list and dashboard state derivation
- Depends on: `T002`, `T003`
- Reuses:
  - the existing invalid-rule UX patterns
  - current stale state cleanup tied to `ruleId`
- Done when:
  - saved website rules become invalid when their selected browser is missing or unsupported
  - stale or non-normalizable host data is surfaced safely
  - invalid website rules do not participate in runtime interception
- Covers: `WEB-11`, `WEB-14`

### T009 - Add tests and run physical-device validation for Chrome

- Status: Pending
- What: Add focused tests for website-rule logic and execute a real-device validation pass covering Chrome detection, unlock bypass, and ambiguous-state handling.
- Where:
  - `app/src/test/java/com/larissa/socialcontrol/`
  - validation notes captured in the active work log if the project keeps one
- Depends on: `T004`, `T005`, `T006`, `T007`, `T008`
- Reuses:
  - the current spec-driven validation style from earlier features
  - existing physical-device manual checklist approach
- Done when:
  - unit tests cover host normalization, duplicate prevention, and invalid-browser handling
  - runtime-oriented tests cover website-rule unlock matching and ambiguous host no-op behavior where feasible
  - manual validation confirms Chrome can intercept `instagram.com`, launch the control app, grant unlock, skip re-interception during the unlock window, and avoid false positives on other sites
- Covers: `WEB-05`, `WEB-06`, `WEB-07`, `WEB-08`, `WEB-09`, `WEB-10`, `WEB-11`, `WEB-13`, `WEB-14`, `WEB-15`, `WEB-16`

## Requirement Coverage Summary

| Requirement | Covered by |
| --- | --- |
| `WEB-01` | `T001`, `T006` |
| `WEB-02` | `T002`, `T003`, `T006` |
| `WEB-03` | `T002`, `T003`, `T006` |
| `WEB-04` | `T001`, `T007` |
| `WEB-05` | `T004`, `T005`, `T009` |
| `WEB-06` | `T007`, `T009` |
| `WEB-07` | `T005`, `T009` |
| `WEB-08` | `T005`, `T009` |
| `WEB-09` | `T002`, `T009` |
| `WEB-10` | `T004`, `T005`, `T009` |
| `WEB-11` | `T002`, `T008`, `T009` |
| `WEB-12` | `T006`, `T007` |
| `WEB-13` | `T007`, `T009` |
| `WEB-14` | `T008`, `T009` |
| `WEB-15` | `T003`, `T009` |
| `WEB-16` | `T005`, `T009` |

## Notes

- This task plan assumes the current work on the multi-rule navigation shell and rule-management flow lands first.
- The MVP boundary for this feature is intentionally `Chrome first`, even though the architecture is prepared for additional browsers later.
- Browser host resolution should be validated on a physical device before the Chrome matcher implementation is treated as stable.
