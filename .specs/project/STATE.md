# State

**Last Updated:** 2026-03-28T16:02:32.2075332-03:00
**Current Work:** Configurable Intervention Rules - Specification

---

## Recent Decisions (Last 60 days)

### AD-001: Keep Intent Lock Android-only and native-first (2026-03-28)

**Decision:** Continue building Intent Lock as a native Android app rather than introducing a cross-platform layer.
**Reason:** The spike validated that the differentiating behavior depends on Android system integration.
**Trade-off:** Less portability and no immediate path to iOS.
**Impact:** Product planning should optimize for Android permissions, OEM behavior, and native architecture choices.

### AD-002: Keep `AccessibilityService` as the interception backbone (2026-03-28)

**Decision:** Use `AccessibilityService` as the primary mechanism for detecting blocked app opens and triggering the lock flow.
**Reason:** Real-device validation proved it is the correct real-time trigger, while usage stats are better suited for measurement.
**Trade-off:** The app depends on a high-friction permission and device-specific behavior.
**Impact:** Onboarding and reliability work must treat Accessibility as a first-class requirement.

### AD-003: Use usage stats for coarse challenge verification (2026-03-28)

**Decision:** Continue using `UsageStatsManager` to estimate time spent in the control app for challenge completion.
**Reason:** The spike confirmed it is good enough for coarse validation and MVP-level completion logic.
**Trade-off:** Timing is approximate and not hardened against all circumvention patterns.
**Impact:** Future product work can keep this approach for MVP, while anti-circumvention remains explicitly out of scope.

### AD-004: Productize the spike with one configurable rule before multi-rule support (2026-03-28)

**Decision:** The first product milestone will support one user-configurable blocked/control pair instead of jumping directly to many rules.
**Reason:** This removes the most important spike hardcodes while keeping scope small enough for an MVP transition.
**Trade-off:** Users still cannot manage multiple blocked apps in the first milestone.
**Impact:** The first feature spec should focus on configurable single-rule behavior, with multi-rule support deferred to a later milestone.

---

## Active Blockers

No active blockers recorded.

---

## Lessons Learned

### L-001: Package visibility can silently break control-app launch (2026-03-28)

**Context:** During the spike, Duolingo was installed but `getLaunchIntentForPackage()` still returned `null`.
**Problem:** Android 11+ package visibility rules prevented the app from resolving the launch intent.
**Solution:** Declare the relevant packages in `queries` in the manifest.
**Prevents:** Misdiagnosing app-launch failures as installation or intent issues.

### L-002: Physical-device validation is mandatory for this product (2026-03-28)

**Context:** The spike depended on real app switches, permissions, and foreground detection behavior.
**Problem:** Emulator-first validation would miss important timing and OEM-specific behavior.
**Solution:** Treat the target device as the primary validation environment.
**Prevents:** False confidence from desktop-only or emulator-only verification.

---

## Quick Tasks Completed

| # | Description | Date | Commit | Status |
| --- | --- | --- | --- | --- |

---

## Deferred Ideas

Ideas captured during work that belong in future features or phases. Prevents scope creep while preserving good ideas.

- [ ] Support multiple blocked apps with per-rule unlock settings - Captured during: post-spike roadmap
- [ ] Add challenge history or streak mechanics after the core configurable MVP is stable - Captured during: post-spike roadmap
- [ ] Investigate a softer reminder fallback if forced redirection proves fragile on more devices - Captured during: post-spike roadmap

---

## Todos

- [ ] Create `design.md` for `configurable-intervention-rules` if the spec expands beyond a straightforward refactor
- [ ] Break the first feature into atomic tasks before implementation starts
- [ ] Rename remaining spike-oriented project identifiers when milestone 1 implementation begins

---

## Preferences

**Model Guidance Shown:** never
