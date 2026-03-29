# Testing Context

## Validation strategy

Prefer validating changes at three levels:

1. unit tests for validation, persistence, and stale-state handling
2. local build verification with Gradle
3. manual device validation for interception and unlock behavior

## Good regression checks

- same blocked and control app should be rejected
- required seconds and unlock minutes should stay within allowed ranges
- invalid or uninstalled saved apps should make rules invalid
- stale sessions and unlock credits should be cleared or ignored
- interception should be skipped during an active unlock window
- interception should resume after unlock expiry

## Open validation work

- the focused unit tests for rule persistence and validation are already in place
- the full manual device validation pass is still the main remaining verification gap
