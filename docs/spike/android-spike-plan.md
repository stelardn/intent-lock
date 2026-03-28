# Android Spike Plan

## Goal

Prove that a personal Android app can interrupt impulsive social media opens by requiring time in a user-chosen control app before allowing access to a blocked app.

This spike is not about polished UX or full architecture yet. It is about answering the feasibility questions that would make or break the MVP.

## Product Flow To Validate

1. User taps a blocked app such as Instagram.
2. Our app detects that the blocked app came to the foreground.
3. Our app redirects the user into a challenge flow.
4. The user spends the configured amount of time in a chosen control app such as Duolingo.
5. Our app verifies enough time was spent in the control app.
6. Our app temporarily unlocks the blocked app.

## Recommended Stack For The Spike

- Android native
- Kotlin
- Jetpack Compose
- AccessibilityService for foreground app detection and intervention
- UsageStatsManager for usage verification and analytics
- DataStore for lightweight local configuration

## Main Unknowns

### 1. Foreground app detection

Question: Can we reliably detect that a blocked app was opened quickly enough to interrupt the behavior?

Likely answer: Yes, with `AccessibilityService`, but timing and event quality can vary by Android version and OEM.

### 2. Redirect behavior

Question: Can we move the user into our own gating flow or the selected control app when a blocked app is opened?

Likely answer: Partially yes, but this is one of the riskiest behaviors because Android is restrictive about background launches and task switching.

### 3. Time verification

Question: Can we verify that the user spent enough time in the control app and did not simply switch away?

Likely answer: Yes, with `UsageStatsManager` and session tracking, though exact timing logic will need testing.

### 4. Unlock window

Question: Can we allow access to the blocked app for a short session without immediately re-blocking it?

Likely answer: Yes, if we maintain an in-memory and persisted unlock state with expiry.

## Spike Success Criteria

The spike is successful if all of the following are true on a real Android device:

- We can detect a target app entering the foreground.
- We can bring up our own interruption UI within a practical delay.
- We can launch the chosen control app from that flow.
- We can measure at least a coarse but reliable amount of time spent in the control app.
- We can mark the blocked app as temporarily unlocked and avoid a redirect loop.

## Spike Scope

### In scope

- One blocked app
- One control app
- One required duration value
- Manual permission setup
- Local-only storage
- Real-device validation

### Out of scope

- Play Store readiness
- Accounts or cloud sync
- Subscription or payments
- Sophisticated anti-cheat logic
- Device admin features
- iOS support

## Proposed Spike Order

## Spike 1: Detect blocked app opens

### Goal

Detect when a specific app package enters the foreground.

### Implementation

- Create a minimal Android app with a setup screen.
- Ask the user to enable Accessibility permissions.
- Implement an `AccessibilityService`.
- Log package names from relevant accessibility events.
- Filter for one blocked package such as `com.instagram.android`.

### Success criteria

- We can consistently see the blocked package when it is opened.
- Detection happens fast enough to feel immediate.

### Notes

- This should be tested on a physical device, not only an emulator.
- We should record behavior on your exact phone model and Android version.

## Spike 2: Interrupt with our own gating screen

### Goal

When the blocked app opens, bring the user into our app instead.

### Implementation

- From the accessibility service, trigger an intent into a simple "Access Locked" activity.
- Show the blocked app name, chosen control app, and required time.
- Add loop protection so reopening our own app does not retrigger the block.

### Success criteria

- Opening the blocked app results in our lock screen appearing most of the time.
- We do not get stuck in an app-switching loop.

### Risk

- This is the highest-risk spike because Android may behave differently across devices.

## Spike 3: Launch the control app and track time

### Goal

Start the control app and measure enough active time spent there.

### Implementation

- Add a "Start challenge" button on the lock screen.
- Launch the selected control app via package manager intent.
- Start a challenge session with:
  - blocked app package
  - control app package
  - required duration
  - session start timestamp
- Poll or query usage events to estimate time spent in the control app.

### Success criteria

- We can tell whether the user spent the required time in the control app.
- Brief app switches or screen-off events do not incorrectly count as success.

## Spike 4: Temporary unlock flow

### Goal

Let the user open the blocked app without immediate re-blocking after challenge completion.

### Implementation

- Store an unlock token with expiry, such as 10 minutes.
- When the blocked app is detected, skip interception if the unlock is still valid.
- Surface a small status view in the app showing whether a package is currently unlocked.

### Success criteria

- After completing the challenge, the blocked app can be opened normally.
- Once the unlock expires, blocking resumes.

## Minimal Technical Design For The Spike

### App modules

- `app`: Compose UI, settings, session state
- `service`: accessibility service and interception logic
- `usage`: usage events querying and time aggregation

### Local data model

- `BlockedAppConfig`
  - blocked package name
  - control package name
  - required duration seconds
- `ChallengeSession`
  - session id
  - blocked package name
  - control package name
  - started at
  - accumulated eligible seconds
  - completed at
- `UnlockGrant`
  - blocked package name
  - expires at

### Guardrails

- Ignore events from our own package.
- Ignore events during a currently valid unlock.
- Debounce repeated accessibility events for the same package.
- Store enough logs during the spike to understand failures.

## Practical Testing Plan

Test on your own phone with:

- one low-stakes blocked app first
- one control app that is easy to recognize
- short required durations such as 15 to 30 seconds

For each spike run, capture:

- device model
- Android version
- target app package
- detection delay
- whether redirect worked
- whether time tracking matched reality

## Recommendation After The Spike

If Spikes 1 through 4 succeed on your device, proceed with an Android-only MVP.

If Spike 2 proves too brittle, fall back to a softer intervention model:

- detect the blocked app
- show a full-screen overlay or notification reminder
- require manual tap into the control app instead of forced redirection

That fallback is weaker, but still useful for reducing impulsive opens.

## First Build Target

The first coded milestone after this document should be:

"A basic Android app that detects Instagram opening and shows a lock screen instead."

That gives us the earliest possible answer to the hardest technical question.
