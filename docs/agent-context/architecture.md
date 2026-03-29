# Architecture Context

## Main surfaces

- `MainActivity`: main dashboard, rules list, rule editor, and system status UI
- `LockActivity`: interruption screen shown when a blocked app is opened
- `SocialAccessibilityService`: foreground app interception entry point

## Main runtime collaborators

- `MainViewModel`: aggregates UI state, validation, session progress, unlock credits, and permission readiness
- `InterventionRuleStore`: persists rules in `SharedPreferences`
- `ChallengeSessionStore`: persists active challenge sessions
- `UnlockGrantStore`: persists active unlock windows and expired credits
- `InstalledAppRepository`: lists launchable apps and resolves launch intents
- `PermissionStatusRepository`: checks accessibility and usage-access readiness
- `RuleRuntimeValidator`: validates draft and saved rules
- `UsageStatsChallengeTracker`: calculates control-app foreground progress

## Current data model

### `InterventionRule`

Represents one saved protection rule:

- blocked app package and display name
- control app package and display name
- required challenge duration in seconds
- unlock window duration in minutes
- enabled flag
- `ruleId`
- save timestamp

### Why `ruleId` matters

`ruleId` is used to invalidate stale state safely.

When a rule changes:

- old sessions should not continue driving progress
- old unlock credits should not remain valid
- runtime logic should prefer clearing stale state over trying to migrate it

This is a core safety principle in the current app.

## Persistence model

All current runtime persistence is local and synchronous through `SharedPreferences`.

This is intentional because:

- the app needs simple immediate reads in `AccessibilityService` and `LockActivity`
- the current MVP favors low-risk on-device behavior over architectural complexity

There is no database and no cloud sync.

## Permission model

The app depends on two Android capabilities:

- Accessibility access for interception
- Usage access for challenge tracking

If either permission is missing:

- saved rules remain stored
- the UI should clearly show missing readiness
- runtime behavior should remain safe and understandable

## Source-of-truth file map

Use these files first when orienting yourself:

- `app/src/main/java/com/larissa/socialcontrol/MainActivity.kt`
- `app/src/main/java/com/larissa/socialcontrol/MainViewModel.kt`
- `app/src/main/java/com/larissa/socialcontrol/SocialAccessibilityService.kt`
- `app/src/main/java/com/larissa/socialcontrol/LockActivity.kt`
- `app/src/main/java/com/larissa/socialcontrol/InterventionRule.kt`
- `app/src/main/java/com/larissa/socialcontrol/InterventionRuleStore.kt`
- `app/src/main/java/com/larissa/socialcontrol/ChallengeSessionStore.kt`
- `app/src/main/java/com/larissa/socialcontrol/UnlockGrantStore.kt`
- `app/src/main/java/com/larissa/socialcontrol/RuleRuntimeValidator.kt`
- `app/src/main/java/com/larissa/socialcontrol/InstalledAppRepository.kt`
- `app/src/main/java/com/larissa/socialcontrol/PermissionStatusRepository.kt`
- `app/src/main/java/com/larissa/socialcontrol/UsageStatsChallengeTracker.kt`
- `app/src/main/AndroidManifest.xml`
