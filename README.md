# Intent Lock

This repository started as a technical spike for a personal Android app that interrupts impulsive social media opens and redirects the user into a lock flow. The spike has now been validated on a physical Android device, and the project is moving into its first product-oriented SDD phase as Intent Lock.

## Current stage

Validated spike behavior today:

- watches for `com.instagram.android`
- uses `AccessibilityService` to detect when the blocked app reaches the foreground
- opens a lock screen when Instagram is detected
- launches `com.duolingo` from the lock screen as the control app
- saves a local challenge session
- estimates control-app foreground time via `UsageStatsManager`
- grants a temporary unlock window after challenge completion

The next product step is to replace these hardcoded values with a configurable single-rule MVP.

## Planning docs

Spike and validation references:

- [docs/spike/android-spike-plan.md](docs/spike/android-spike-plan.md)
- [docs/spike/validated-spike-reference.md](docs/spike/validated-spike-reference.md)

SDD project artifacts:

- [.specs/project/PROJECT.md](.specs/project/PROJECT.md)
- [.specs/project/ROADMAP.md](.specs/project/ROADMAP.md)
- [.specs/project/STATE.md](.specs/project/STATE.md)
- [.specs/features/configurable-intervention-rules/spec.md](.specs/features/configurable-intervention-rules/spec.md)

## Open in Android Studio

1. Open this folder in Android Studio.
2. Let Android Studio create or update the Gradle wrapper and sync the project.
3. Run the app on a physical Android device.

## Manual spike validation on device

1. Open the app.
2. Tap `Open Accessibility Settings`.
3. Enable `Intent Lock Watcher`.
4. Return to the home screen and open Instagram.
5. Tap `Start challenge`.
6. Confirm whether Duolingo opens.
7. Spend roughly 15 seconds in Duolingo.
8. Return to Intent Lock and confirm the tracked challenge time updates.
9. Reopen Instagram and confirm it is no longer intercepted during the unlock window.

## Notes

- The project is intentionally local-first and Android-only.
- Usage Access is required for challenge time tracking.
- The current codebase still contains spike-era hardcodes in `AppConfig`.
- The package name is still `com.larissa.socialcontrol` for now and can be revisited separately from the branding rename.
