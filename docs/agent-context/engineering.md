# Engineering Context

## Version and build facts

Current values from `app/build.gradle.kts`:

- `versionName = "0.1.0"`
- `versionCode = 1`
- `applicationId = "com.larissa.socialcontrol"`
- `minSdk = 29`
- `targetSdk = 36`
- `compileSdk = 36`

Current build outputs:

- debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- release APK: `app/build/outputs/apk/release/app-release.apk` when release signing is configured
- unsigned fallback APK: `app/build/outputs/apk/release/app-release-unsigned.apk` when release signing is not configured

The GitHub Actions pipeline expects release signing secrets so it can publish an installable release APK.

## UI and copy guidance

When editing UI:

- keep all user-facing copy in PT-BR
- preserve the existing app framing around intention, protection, challenge, and temporary credits
- do not casually switch to English in screens, labels, buttons, or validation messages
- keep section and card headers legible on narrow layouts
- if a header action crowds the title or subtitle, stack the action below the text on small screens

## Known technical characteristics

- The package name is still `com.larissa.socialcontrol`.
- Some naming still reflects earlier project naming and may not match the final brand.
- `AppConfig.kt` now mainly holds shared limits and debounce/default values.

## Specs and forward-looking documents

Relevant planning docs:

- `.specs/features/configurable-intervention-rules/design.md`
- `.specs/features/configurable-intervention-rules/tasks.md`
- `.specs/features/browser-website-blocking/spec.md`
- `.specs/features/browser-website-blocking/design.md`
- `.specs/features/browser-website-blocking/tasks.md`

Important note:

- the configurable intervention rules work is implemented in code
- the browser website blocking feature exists as planning and spec material, not as implemented runtime behavior

## Safe change strategy

When modifying behavior, prefer this order:

1. understand whether the change affects rule validity, session lifecycle, or unlock lifecycle
2. preserve rule-driven behavior instead of reintroducing hardcoded packages
3. keep `MainViewModel` as the place where derived UI state is assembled
4. keep `SocialAccessibilityService` and `LockActivity` simple and defensive
5. avoid introducing async persistence unless there is a strong reason
