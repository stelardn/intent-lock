# Runtime Context

## Rule save flow

1. User creates or edits a rule in `MainActivity`.
2. `MainViewModel` validates the draft through `RuleRuntimeValidator`.
3. `InterventionRuleStore` persists the rule.
4. Any session or unlock data for that rule is cleared.
5. The UI refreshes and rebuilds derived status.

## Interception flow

1. `SocialAccessibilityService` receives a window change event.
2. It ignores its own package and irrelevant event types.
3. It loads a usable enabled rule for the foreground package.
4. It validates the saved rule before using it.
5. If the app is currently unlocked for that rule, it skips interception.
6. Otherwise it launches `LockActivity`.

## Challenge flow

1. `LockActivity` loads the rule from the passed `ruleId`.
2. If the rule is missing, disabled, invalid, or not launchable, it shows recovery UI.
3. If valid, the user starts the challenge.
4. A `ChallengeSession` is saved.
5. The control app is launched.
6. `MainViewModel` later checks progress with `UsageStatsChallengeTracker`.
7. Once progress reaches the required seconds, an `UnlockGrant` is created and the session is cleared.

## Behavioral invariants

- Interception must remain rule-driven, not hardcoded.
- Invalid, stale, or disabled rules must fail safe.
- Unlock grants must only bypass interception for the matching rule and blocked package.
- Rule changes should clear or invalidate stale session and unlock state.
