# Browser Website Blocking Design

## Overview

This design extends the post-multi-rule IntentLock architecture with a second protected-target type: supported websites inside supported browsers.

The starting assumption is that the multi-rule navigation work described in `docs/multi-rule-navigation-wireframe.md` has already landed far enough to provide:

- a multi-rule list in `Regras`
- a create/edit rule flow
- rule summaries on the dashboard
- per-rule runtime resolution instead of a single saved rule

Website blocking should build on that foundation rather than creating a parallel configuration model.

## Scope

This design covers:

- website-based intervention rules inside supported browsers
- create/edit/list UI for website rules within the shared multi-rule experience
- runtime host matching from browser accessibility data
- rule-specific challenge sessions and unlock grants for website targets
- invalid-state handling for unsupported or missing browsers

This design does not cover:

- all browsers on Android
- path-level or query-level URL filtering
- browser extension integration
- hardened circumvention prevention
- remote rule sync

## Product Decisions

### Supported browser boundary

Recommended MVP boundary:

- support `Google Chrome` first
- design the runtime around browser adapters so other browsers can be added later

### Matching granularity

Use normalized host-level matching in the MVP.

Examples that should resolve to the same protected target:

- `instagram.com`
- `www.instagram.com`
- `m.instagram.com`

Not included in the MVP:

- path-specific matching such as `/reels`
- query-string filtering
- wildcards broader than the normalized host family

### Confidence-first interception

Website interception should trigger only when the runtime can resolve the active host confidently enough from accessibility data.

Implication:

- false positives are more damaging than false negatives here
- if the browser UI does not expose a trustworthy host, IntentLock should not guess

### Unified rule model

Website rules should live in the same rule engine as app rules.

### Website input model

Recommended create/edit fields for a website rule:

- target kind: `App` or `Site`
- browser: supported browser picker
- site host: manual text entry with normalization and validation
- control app
- challenge duration
- unlock window
- rule enabled state

## Architecture

### Rule model evolution

The post-multi-rule domain should evolve from app-only rule targeting into a target union.

Recommended shape:

```kotlin
data class InterventionRule(
    val ruleId: String,
    val target: ProtectedTarget,
    val controlPackage: String,
    val controlAppName: String,
    val requiredSeconds: Int,
    val unlockWindowMinutes: Int,
    val isEnabled: Boolean,
    val savedAtEpochMs: Long,
)

sealed interface ProtectedTarget {
    data class App(
        val packageName: String,
        val appName: String,
    ) : ProtectedTarget

    data class Website(
        val browserPackage: String,
        val browserAppName: String,
        val normalizedHost: String,
        val displayHost: String,
    ) : ProtectedTarget
}
```

### Session and unlock scope

Challenge sessions and unlock grants should remain rule-scoped.

Recommended interpretation:

- a session belongs to one `ruleId`
- an unlock grant belongs to one `ruleId`
- runtime match resolution decides whether the current browser event belongs to that rule

### Browser support registry

Add a browser support registry responsible for:

- listing supported browsers for the rule form
- checking whether a saved browser package is still installed
- resolving a browser-specific accessibility matcher

### Browser host matcher

Add a runtime matcher abstraction so Chrome-specific accessibility parsing does not leak through the rest of the app.

Recommended interface:

```kotlin
interface BrowserHostMatcher {
    fun resolveActiveHost(eventContext: BrowserEventContext): HostMatchResult
}

data class BrowserEventContext(
    val eventPackageName: String,
    val rootNode: AccessibilityNodeInfo?,
    val event: AccessibilityEvent?,
)

sealed interface HostMatchResult {
    data class Matched(val normalizedHost: String) : HostMatchResult
    data object NoHostFound : HostMatchResult
    data object Ambiguous : HostMatchResult
}
```

### Validation model

The rule validator should extend its checks with website-specific rules:

- target kind is required
- website target requires supported browser
- website target requires valid host syntax after normalization
- website rule cannot duplicate another rule with the same normalized host + browser package
- control app remains required
- challenge duration and unlock window still respect product limits

### Persistence

The multi-rule store should persist website target metadata alongside app targets.

Recommended persisted fields for website targets:

- `target_kind = WEBSITE`
- `browser_package`
- `browser_app_name`
- `normalized_host`
- `display_host`

## UI Design

### Rule list

Website rules should appear beside app rules in `Regras`.

Recommended card summary:

- title: display host, for example `instagram.com`
- subtitle: `No Chrome`
- metadata line: `Controle: Duolingo`
- metrics line: challenge duration + unlock window
- status chip: `Ativa`, `Inativa`, `Invalida`, or `Permissoes incompletas`

### Create/edit rule flow

The shared rule form should add a target-type choice near the top.

Recommended fields:

1. target type segmented control: `App` | `Site`
2. if `App`
   - blocked app picker
3. if `Site`
   - browser picker
   - site host field
4. control app picker
5. challenge duration
6. unlock window
7. rule enabled toggle

### Dashboard summaries

Website rules should contribute to dashboard surfaces without masquerading as apps.

Recommended treatment:

- `Apps protegidos` evolves into `Protecoes ativas` or `Alvos protegidos`
- app targets show app names
- website targets show host chips like `instagram.com`

### Lock screen

`LockActivity` should remain shared across target kinds.

For website rules, the ready state copy should adapt from app phrasing to target phrasing.

## Runtime Flow

### Website rule save flow

1. User opens `Nova regra` from the post-multi-rule rules tab.
2. User selects `Site` as the target type.
3. User selects a supported browser.
4. User enters a host such as `instagram.com`.
5. The form normalizes and validates the host.
6. The rule store saves the website target plus shared rule fields.
7. Any stale session or unlock state for the replaced rule is cleared according to the existing rule lifecycle.

### Website interception flow

1. `SocialAccessibilityService` receives a relevant accessibility event.
2. Runtime rule resolution finds enabled website rules whose `browserPackage` matches the foreground package.
3. If no candidate website rules exist for that browser package, return.
4. The browser support registry resolves the browser matcher.
5. The matcher inspects the active accessibility tree and tries to resolve the active host.
6. If host resolution is ambiguous or missing, return without interception.
7. If the normalized host matches one saved website rule and no valid unlock exists for that `ruleId`, launch `LockActivity`.
8. Debounce repeated events for the same rule and host.

### Website unlock flow

1. User starts the challenge from `LockActivity`.
2. The app creates a challenge session for the resolved website rule.
3. The configured control app is launched.
4. Progress is tracked the same way as app rules through usage stats.
5. On completion, an unlock grant is created for the matching `ruleId`.
6. When the protected host is opened again in the same supported browser before expiry, interception is skipped.

## Invalid And Stale State Handling

### Missing browser

If the saved browser package is no longer installed:

- mark the rule invalid
- preserve the stored host for display
- stop runtime interception for that rule
- prompt the user to edit or replace it

### Unsupported browser drift

If the support registry no longer recognizes the saved browser package:

- mark the rule invalid
- explain that the browser is no longer supported by the current build

### Debounce protection

The current debounce strategy should evolve from package-based to rule-based for website interceptions.

## Android Integration Notes

### Accessibility usage

Website detection depends on accessibility data beyond `event.packageName`.

The service will likely need to inspect:

- `rootInActiveWindow`
- event source node when available
- visible text or editable text fields associated with the browser address bar

### Physical-device validation

Manual validation remains mandatory because browser accessibility trees can vary by Android version, Chrome version, OEM build, and address bar focus state.

## Implementation Plan

1. Wait for the multi-rule navigation and rule-management foundation to stabilize.
2. Extend the rule model and persistence to support website targets.
3. Add browser support registry and Chrome host matcher.
4. Extend rule validation, duplicate detection, and invalid-state handling for website rules.
5. Update create/edit/list/dashboard UI for mixed app and website targets.
6. Extend `SocialAccessibilityService` runtime rule resolution for browser host matching and rule-based debounce.
7. Validate the end-to-end flow on a physical device with Chrome.

## Testing Strategy

### Unit-level checks

- host normalization and validation
- duplicate website rule rejection for same browser + host
- missing or unsupported browser invalidation
- rule-based unlock matching for website rules
- ambiguous or missing host resolution returning no interception

### Manual device checks

1. Save a Chrome website rule for `instagram.com`.
2. Restart the app and confirm the rule still renders correctly in `Regras`.
3. Open `instagram.com` in Chrome and confirm interception occurs.
4. Start the challenge and confirm the configured control app launches.
5. Complete the required time and confirm temporary unlock is granted.
6. Reopen `instagram.com` during the unlock window and confirm interception is skipped.
7. Open another site in Chrome and confirm it is not blocked by the Instagram rule.
8. Remove Chrome or disable browser support in a test build and confirm the rule becomes invalid.
9. Validate repeated browser events do not create a lock loop.

## Requirement Traceability

| Requirement | Design coverage |
| --- | --- |
| WEB-01 | Shared create/edit flow supports `App` and `Site` target kinds |
| WEB-02 | Website rules require supported browser selection |
| WEB-03 | Website rules require normalized valid host input |
| WEB-04 | Website rules persist and round-trip through multi-rule storage |
| WEB-05 | `SocialAccessibilityService` resolves website rules by browser package + host |
| WEB-06 | `LockActivity` starts a session for the matched website rule |
| WEB-07 | Challenge completion grants unlock for the website rule |
| WEB-08 | Website unlock bypasses interception until expiry |
| WEB-09 | The rule form only exposes explicitly supported browsers |
| WEB-10 | Runtime does not claim a match when host resolution is ambiguous |
| WEB-11 | Missing or unsupported browsers invalidate saved website rules |
| WEB-12 | Website rules appear in the shared multi-rule list and create/edit flow |
| WEB-13 | Dashboard summaries represent website targets clearly |
| WEB-14 | Stale website rule state is ignored or invalidated safely |
| WEB-15 | Duplicate or overlapping website rules are prevented deterministically |
| WEB-16 | Rule-based debounce prevents repeated browser lock loops |

## Assumptions

- The multi-rule navigation feature is complete enough that rules are no longer modeled as a single saved rule.
- Chrome is the first supported browser.
- Host-level matching is sufficient for the first release of browser blocking.
- Avoiding false-positive locks is more important than capturing every possible browser navigation state.
