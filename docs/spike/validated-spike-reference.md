# Validated Spike Reference

## Purpose

This document is the compact technical handoff for future sessions.

It captures:

- what was validated on a physical Android device
- the implementation choices already proven to work
- the code areas that matter most
- the known constraints and caveats
- the recommended next steps for productizing the spike

Use this document as the primary re-entry point before making further changes.

## Current Validation Status

The full technical spike was validated on a physical Android device.

Validated behaviors:

1. The app detects Instagram opening via `AccessibilityService`.
2. The app redirects into its own lock screen.
3. The lock screen launches Duolingo successfully.
4. The app measures approximate foreground time spent in Duolingo with `UsageStatsManager`.
5. The app marks the challenge as completed when the required time is reached.
6. The app grants a temporary unlock window for Instagram.
7. During the unlock window, Instagram is no longer intercepted.

This means the core technical premise of the MVP is feasible on the validated device.

## Validated Product Flow

Current hardcoded flow:

1. User opens Instagram.
2. `SocialAccessibilityService` receives a foreground/window event.
3. If Instagram is not currently unlocked, the app launches `LockActivity`.
4. User taps `Start challenge`.
5. The app stores a `ChallengeSession`.
6. The app launches Duolingo.
7. User spends time in Duolingo.
8. User returns to Social Control.
9. `UsageStatsChallengeTracker` estimates tracked Duolingo foreground time.
10. If the tracked time reaches the threshold, the app marks the session completed.
11. The app creates an `UnlockGrant` for Instagram.
12. While the unlock is still valid, `SocialAccessibilityService` skips interception.

## Hardcoded Values

These values are currently fixed in code in `AppConfig`:

- blocked app: `com.instagram.android`
- control app: `com.duolingo`
- required challenge duration: `15` seconds
- unlock window: `10` minutes
- lock debounce: `1500` ms

These are acceptable for the spike, but should be replaced by user-configurable storage in the next product phase.

## Core Technical Decisions

### Android-only, native-first

The spike confirmed that the critical value is in Android system integration, not cross-platform UI.

Chosen stack:

- Kotlin
- Jetpack Compose
- `AccessibilityService`
- `UsageStatsManager`
- local persistence via `SharedPreferences` for the spike

### Accessibility is the interception backbone

Foreground app interception is driven by `AccessibilityService`, not by usage stats.

Reason:

- `UsageStatsManager` is useful for measurement and reporting
- it is not the right backbone for real-time interruption

### Usage stats are good enough for coarse challenge verification

The current implementation uses usage events to estimate foreground time in the control app.

This is sufficient for:

- validating the concept
- unlocking after approximate task completion

This is not yet hardened against all cheating or edge cases.

### Unlock logic belongs at the interception point

The unlock decision is enforced inside the accessibility service itself.

That was the right place to put it, because it prevents immediate re-blocking loops at the source of interception.

## Important Code Locations

### Interception

- [SocialAccessibilityService.kt](C:\Users\Larissa\Documents\IntentLock\app\src\main\java\com\larissa\socialcontrol\SocialAccessibilityService.kt)

Responsibilities:

- receive accessibility events
- filter to relevant window events
- ignore our own app
- debounce repeated interceptions
- skip interception when an unlock grant is active
- launch the lock screen when the blocked app is opened

### Challenge launch

- [LockActivity.kt](C:\Users\Larissa\Documents\IntentLock\app\src\main\java\com\larissa\socialcontrol\LockActivity.kt)

Responsibilities:

- show the interruption UI
- start the challenge flow
- save the current challenge session
- launch the control app

### Challenge session persistence

- [ChallengeSessionStore.kt](C:\Users\Larissa\Documents\IntentLock\app\src\main\java\com\larissa\socialcontrol\ChallengeSessionStore.kt)

Responsibilities:

- persist active challenge data
- persist completion timestamp
- clear active challenge state

### Usage measurement

- [UsageStatsChallengeTracker.kt](C:\Users\Larissa\Documents\IntentLock\app\src\main\java\com\larissa\socialcontrol\UsageStatsChallengeTracker.kt)

Responsibilities:

- verify whether Usage Access is granted
- query usage events
- aggregate foreground time for the control app
- determine whether the challenge is complete

### Unlock grant persistence

- [UnlockGrantStore.kt](C:\Users\Larissa\Documents\IntentLock\app\src\main\java\com\larissa\socialcontrol\UnlockGrantStore.kt)

Responsibilities:

- persist unlock expiry
- answer whether a blocked app is currently unlocked
- clear expired or manual-reset grants

### Main product state surface

- [MainActivity.kt](C:\Users\Larissa\Documents\IntentLock\app\src\main\java\com\larissa\socialcontrol\MainActivity.kt)

Responsibilities:

- surface permission entry points
- display active session state
- display tracked challenge progress
- mark session completion
- create unlock grant after completion
- show current unlock status

### Package/query configuration

- [AndroidManifest.xml](C:\Users\Larissa\Documents\IntentLock\app\src\main\AndroidManifest.xml)

Important roles:

- registers `AccessibilityService`
- declares `PACKAGE_USAGE_STATS`
- declares `queries` for Instagram and Duolingo package visibility

## Permissions And System Requirements

### Accessibility

Required for:

- detecting blocked app opens
- interrupting with the lock screen

Without it, the core interception flow does not work.

### Usage Access

Required for:

- measuring foreground time spent in the control app
- deciding when the challenge is complete

Without it, the app can still intercept, but cannot verify progress.

### Package visibility

This was an important Android 11+ gotcha during the spike.

Even when `com.duolingo` was correctly installed, `getLaunchIntentForPackage("com.duolingo")` returned `null` until the package was declared in `queries` in the manifest.

That issue is already fixed in the project.

## Important Technical Caveats

### The current timing model is approximate

The usage tracker is based on usage events, not a continuous foreground service clock.

That means:

- it is good enough for coarse validation
- it may vary slightly across Android versions or OEM builds
- it is not yet hardened for anti-circumvention

### The session is still hardcoded

There is no configurable UI yet for:

- choosing blocked apps
- choosing control apps
- choosing required time
- choosing unlock duration

### The app currently supports one active blocked/control pair

The code path is structured around a single pair for spike simplicity.

Supporting multiple pairs will require:

- a config model
- matching logic for arbitrary package names
- grant/session lookup by blocked package

### The state layer is intentionally lightweight

The spike uses `SharedPreferences`, which was the right choice for speed and low risk.

For the MVP, likely upgrades are:

- DataStore for config
- possibly Room if history/logging becomes important

## Debugging Notes Already Learned

### Gradle / Compose

With Kotlin 2.x and Compose enabled, the project must apply:

- `org.jetbrains.kotlin.plugin.compose`

Otherwise Gradle sync fails during project configuration.

### Android XML theme selection

Compose Material 3 does not automatically provide an XML theme resource for `themes.xml`.

The current project uses a framework theme for manifest-level theming and Compose handles the actual UI theme inside Kotlin.

### Device testing matters

This project should be validated on a physical device, not treated as emulator-first work.

The spike already proved that device-level behavior matters for:

- interception timing
- package visibility
- permission behavior
- switching between apps

## Recommended Next Product Phase

Now that the technical spike is validated, the next phase should stop being spike-oriented and become product-oriented.

Recommended sequence:

1. Replace hardcoded config with a persisted user configuration model.
2. Add app pickers for blocked and control apps.
3. Support configurable required duration and unlock duration.
4. Generalize session and unlock logic from one pair to many pairs.
5. Add a clearer challenge lifecycle:
   - pending
   - active
   - completed
   - unlocked
   - expired
6. Add better logs and debug surfaces for future device testing.
7. Add a basic architecture split between config, session, usage tracking, and unlock policy.

## Suggested Refactor Boundaries

When continuing implementation, prefer splitting responsibilities into these layers:

- `config`
  - blocked/control app mappings
  - required durations
  - unlock window durations
- `challenge`
  - active session state
  - completion rules
- `usage`
  - usage access checks
  - foreground time aggregation
- `unlock`
  - unlock grants
  - grant expiry
  - interception bypass rules
- `ui`
  - onboarding
  - configuration
  - lock screen
  - status/debug screens

## What Not To Re-decide Without Reason

These decisions are already supported by real-device evidence and should be treated as stable defaults unless new testing contradicts them:

- use native Android, not a cross-platform wrapper, for the core behavior
- keep `AccessibilityService` as the primary interception mechanism
- use `UsageStatsManager` as the challenge verification input
- enforce unlock logic inside the interception path
- continue testing on the physical target device during development

## Open Questions For The Next Phase

These are still product and architecture questions, not feasibility blockers:

- How should multiple blocked apps be modeled?
- Can one control app unlock several blocked apps?
- Should unlock be consumed on first open or remain time-window-based?
- How should the app explain missing permissions more gracefully?
- Should there be a challenge history or streak model?

## Related Documents

- [android-spike-plan.md](C:\Users\Larissa\Documents\IntentLock\docs\spike\android-spike-plan.md)
- [README.md](C:\Users\Larissa\Documents\IntentLock\README.md)
