# Browser Website Blocking Specification

## Problem Statement

The current IntentLock product path is evolving from a single-rule MVP into a multi-rule experience with dedicated navigation and rule management. After that work lands, the next gap is that distraction can still bypass app-level protection when the same service is opened in a browser, such as `instagram.com` in Chrome.

IntentLock needs a rule type that can protect supported websites inside supported browsers while preserving the existing interception model, challenge flow, and temporary unlock behavior.

## Goals

- [ ] Let the user create a website-based intervention rule after the multi-rule navigation foundation is complete.
- [ ] Intercept supported websites opened inside supported browsers with the same lock flow already used for blocked apps.
- [ ] Keep app rules and website rules in the same rule-management experience so the product still feels like one coherent system.

## Out of Scope

Explicitly excluded for this feature slice.

| Feature | Reason |
| --- | --- |
| Generic support for every Android browser | Browser UIs expose accessibility data differently and reliability needs to be validated incrementally |
| Full URL path/query matching | Host-level matching is a safer MVP boundary and easier to explain in the UI |
| Desktop browser extensions or cross-device sync | The product remains Android-only and local-first |
| Anti-circumvention hardening against every browser trick | This feature extends the current MVP behavior; it does not harden it fully |
| Content-category filtering beyond explicit user-entered hosts | Better handled after supported-site interception is stable |

---

## User Stories

### P1: Create A Website Protection Rule ⭐ MVP

**User Story**: As a user, I want to create a rule for a website inside a browser so that social media opened on the web is interrupted just like the native app.

**Why P1**: Without this, app blocking is easy to sidestep by opening the same service in the browser.

**Acceptance Criteria**:

1. WHEN the user creates or edits a rule THEN the system SHALL let them choose whether the protected target is an app or a website.
2. WHEN the user chooses website protection THEN the system SHALL require a supported browser and a valid website host.
3. WHEN the user saves a valid website rule THEN the system SHALL persist it with the same lifecycle guarantees as app rules.
4. WHEN the user views the rules list or dashboard THEN the system SHALL clearly identify website rules as browser-based protections rather than installed apps.

**Independent Test**: Create a website rule for `instagram.com` in Chrome, restart the app, and confirm the rule still appears correctly in the rules UI.

---

### P1: Intercept A Supported Website End-To-End ⭐ MVP

**User Story**: As a user, I want IntentLock to block a configured website in the browser so that opening the site triggers the same challenge flow as opening a blocked app.

**Why P1**: The feature only creates user value if the saved website rule drives the real interception path.

**Acceptance Criteria**:

1. WHEN the configured browser comes to the foreground on a page that matches a saved blocked host and no valid unlock exists THEN the system SHALL launch the lock flow for the matching rule.
2. WHEN the user starts the challenge from a website-based lock screen THEN the system SHALL create a session linked to that website rule and launch the configured control app.
3. WHEN tracked foreground time in the control app reaches the configured threshold THEN the system SHALL create a temporary unlock grant for that website rule.
4. WHEN the unlock grant is still valid THEN the system SHALL not re-intercept the same protected website until the configured unlock window expires.

**Independent Test**: Save a Chrome rule for `instagram.com`, open the site in Chrome, complete the challenge in the chosen control app, and confirm the site can be reopened during the unlock window without another interception.

---

### P2: Handle Browser Detection Limits Transparently

**User Story**: As a user, I want the product to be honest about which browsers and website detections are supported so that failures are understandable instead of mysterious.

**Why P2**: Browser accessibility surfaces are inherently less reliable than plain package-name interception.

**Acceptance Criteria**:

1. WHEN the user creates a website rule THEN the system SHALL restrict browser selection to explicitly supported browsers.
2. WHEN the active browser page cannot be resolved confidently enough to determine the current host THEN the system SHALL avoid claiming a site match it cannot justify.
3. WHEN a saved website rule becomes unsupported or unusable because its browser is no longer available THEN the system SHALL mark the rule invalid and explain why.

**Independent Test**: Save a website rule, remove the selected browser or use an unsupported browser, and confirm the app surfaces the rule state clearly without applying a false match.

---

### P2: Fit Website Rules Into Multi-Rule Navigation

**User Story**: As a user, I want website rules to appear naturally beside app rules so that rule management stays centralized after the multi-rule navigation work.

**Why P2**: A separate website-only flow would fragment the product and fight the ongoing navigation redesign.

**Acceptance Criteria**:

1. WHEN the user opens `Regras` THEN the system SHALL show app and website rules in the same list with distinguishable summaries.
2. WHEN the user creates a new rule THEN the create/edit flow SHALL support both target kinds without forcing a separate screen family.
3. WHEN the dashboard summarizes protected targets THEN website rules SHALL contribute meaningful labels without pretending they are installed apps.

**Independent Test**: Create at least one app rule and one website rule, then confirm both can be managed from the same rules tab and are summarized clearly on the dashboard.

---

### P3: Recover Safely From Ambiguous Or Stale Browser State

**User Story**: As a user, I want IntentLock to recover safely when browser-derived state is stale or ambiguous so that I do not get stuck in a noisy or broken lock loop.

**Why P3**: Website detection adds ambiguity that does not exist in pure package-based rules.

**Acceptance Criteria**:

1. WHEN a saved website rule no longer matches the current rule model or stored browser metadata THEN the system SHALL ignore or invalidate the stale rule safely.
2. WHEN multiple rules could appear to match the same browser event THEN the system SHALL resolve to a single deterministic rule or reject the invalid configuration before runtime.
3. WHEN the browser remains on screen during repeated accessibility events for the same protected host THEN the system SHALL debounce interception and avoid reopening the lock flow in a tight loop.

**Independent Test**: Configure overlapping or stale website state, then confirm runtime behavior does not produce duplicate locks or ambiguous rule application.

---

## Edge Cases

- WHEN the user enters `https://instagram.com`, `www.instagram.com`, or `m.instagram.com` THEN the system SHALL normalize the input to one canonical host-matching format.
- WHEN the user enters an invalid host, blank host, or unsupported browser THEN the system SHALL block saving and show a clear validation message.
- WHEN an app rule and a website rule both conceptually protect the same service, such as Instagram app plus Instagram web, THEN the system SHALL keep them as separate rules with independent runtime matches.
- WHEN the selected browser is foregrounded but the active page host cannot be read confidently from accessibility data THEN the system SHALL not trigger an interception based on guesswork alone.
- WHEN a browser tab switches between protected and unprotected hosts while the browser package stays foregrounded THEN the system SHALL reevaluate matching on relevant accessibility events.
- WHEN the user changes or deletes a website rule while a session or unlock grant is active THEN the system SHALL prevent stale session or unlock state from being applied to the updated rule.

---

## Requirement Traceability

| Requirement ID | Story | Phase | Status |
| --- | --- | --- | --- |
| WEB-01 | P1: Create A Website Protection Rule | Design | Pending |
| WEB-02 | P1: Create A Website Protection Rule | Design | Pending |
| WEB-03 | P1: Create A Website Protection Rule | Design | Pending |
| WEB-04 | P1: Create A Website Protection Rule | Design | Pending |
| WEB-05 | P1: Intercept A Supported Website End-To-End | Design | Pending |
| WEB-06 | P1: Intercept A Supported Website End-To-End | Design | Pending |
| WEB-07 | P1: Intercept A Supported Website End-To-End | Design | Pending |
| WEB-08 | P1: Intercept A Supported Website End-To-End | Design | Pending |
| WEB-09 | P2: Handle Browser Detection Limits Transparently | Design | Pending |
| WEB-10 | P2: Handle Browser Detection Limits Transparently | Design | Pending |
| WEB-11 | P2: Handle Browser Detection Limits Transparently | Design | Pending |
| WEB-12 | P2: Fit Website Rules Into Multi-Rule Navigation | Design | Pending |
| WEB-13 | P2: Fit Website Rules Into Multi-Rule Navigation | Design | Pending |
| WEB-14 | P3: Recover Safely From Ambiguous Or Stale Browser State | Design | Pending |
| WEB-15 | P3: Recover Safely From Ambiguous Or Stale Browser State | Design | Pending |
| WEB-16 | P3: Recover Safely From Ambiguous Or Stale Browser State | Design | Pending |

**ID format:** `[CATEGORY]-[NUMBER]`

**Coverage:** 16 total, 0 mapped to tasks, 16 unmapped pending the first design/task pass.

---

## Success Criteria

- [ ] After the multi-rule navigation work lands, a user can create and manage a browser website rule from the same rules experience used for app rules.
- [ ] A supported site such as `instagram.com` in the supported browser reliably triggers the lock flow on a physical Android device.
- [ ] The UI makes the support boundary clear enough that unsupported browsers or ambiguous detections do not feel like silent failures.
