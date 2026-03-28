# Roadmap

**Current Milestone:** Milestone 1 - Configurable Single-Rule MVP
**Status:** Planning

---

## Milestone 1 - Configurable Single-Rule MVP

**Goal:** Turn the validated spike into a usable personal MVP with one configurable intervention rule that works end-to-end on a physical Android device.
**Target:** A user can configure one blocked app and one control app without editing code, then complete the challenge and receive a temporary unlock.

### Features

**Configurable Intervention Rule** - PLANNED

- choose one blocked app from installed apps
- choose one control app from installed apps
- configure required challenge duration and unlock window
- persist rule locally and stop depending on `AppConfig` hardcodes

**Permission And Status Onboarding** - PLANNED

- guide the user through Accessibility and Usage Access setup
- show whether permissions are missing or ready
- show the currently active rule and whether it is valid

**Reliable Challenge Lifecycle** - PLANNED

- formalize states such as pending, active, completed, unlocked, and expired
- recover cleanly after interrupted sessions or app restarts
- make current progress and unlock status visible in the app

---

## Milestone 2 - Multi-Rule Expansion

**Goal:** Support more than one blocked app and make rule handling scalable without losing the validated interception behavior.

### Features

**Rule Engine For Multiple Blocked Apps** - PLANNED

- store multiple blocked/control mappings
- resolve the correct rule by blocked package
- support per-rule duration and unlock settings

**Reusable Unlock Policy** - PLANNED

- generalize unlock grants beyond a single package pair
- define consistent expiry and cleanup behavior
- prevent stale grants and re-block loops across multiple rules

**History And Debug Surfaces** - PLANNED

- show recent challenge outcomes
- expose debug information useful during device validation
- help explain why a block, challenge, or unlock did or did not happen

---

## Milestone 3 - Hardening And Release Readiness

**Goal:** Improve resilience, clarity, and maintainability enough for longer-term personal use and future distribution decisions.

### Features

**Device Hardening** - PLANNED

- test on additional Android versions and OEM behaviors
- improve failure handling around permissions, app switches, and package visibility
- document known compatibility limits

**Product Polish** - PLANNED

- rename remaining spike-oriented identifiers
- improve copy, onboarding clarity, and empty states
- refine settings and reset flows

**Quality Baseline** - PLANNED

- add focused tests for config, challenge, and unlock policy logic
- document architecture boundaries for ongoing development
- reduce fragile coupling between UI, service, and persistence

---

## Future Considerations

- anti-circumvention heuristics
- challenge history and streaks
- optional reminders or notification fallback
- export/import of local settings
- release packaging after device compatibility is better understood
