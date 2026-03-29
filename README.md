# Intent Lock

Intent Lock is an Android app that intercepts distracting app launches and routes the user through an intentional unlock flow before access is granted.

## Current version

- App version: `0.1.0`
- Version code: `1`
- Current application ID: `com.larissa.socialcontrol`

## What the app does today

The current app can:

- monitor `com.instagram.android`
- use `AccessibilityService` to detect when the blocked app reaches the foreground
- open a lock screen when Instagram is detected
- launch `com.duolingo` from the lock screen as the control app
- store a local challenge session
- estimate control-app foreground time through `UsageStatsManager`
- grant a temporary unlock window after challenge completion

## Tech stack

- Kotlin
- Android SDK 36
- Jetpack Compose
- Gradle Kotlin DSL

## Project structure

- `app/`: Android application module
- `docs/`: supporting product and technical documents
- `.specs/`: project and feature specifications

## Open in Android Studio

1. Open this folder in Android Studio.
2. Let Android Studio sync the Gradle project.
3. Run the app on a physical Android device.

## Build APKs

### Debug APK

Generate a debug APK with:

```powershell
.\gradlew.bat :app:assembleDebug
```

Output:

- `app/build/outputs/apk/debug/app-debug.apk`

### Release APK

Generate a release APK with:

```powershell
.\gradlew.bat :app:assembleRelease
```

Output:

- `app/build/outputs/apk/release/app-release-unsigned.apk`

The current release build is generated as `unsigned`. A release keystore is required before distributing the APK outside development use.

## Device setup

For the app to work correctly on a device, enable:

- Accessibility access for `Intent Lock Watcher`
- Usage access for challenge time tracking

## Manual validation

1. Open the app.
2. Tap `Open Accessibility Settings`.
3. Enable `Intent Lock Watcher`.
4. Return to the home screen and open Instagram.
5. Tap `Start challenge`.
6. Confirm that Duolingo opens.
7. Stay in Duolingo for about 15 seconds.
8. Return to Intent Lock and confirm that tracked challenge time updates.
9. Open Instagram again and confirm it is no longer intercepted during the unlock window.

## Notes

- The project is local-first and Android-only.
- The codebase still contains fixed values in `AppConfig`.
- The package name is still `com.larissa.socialcontrol`.
