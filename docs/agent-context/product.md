# Product Context

## Product summary

Intent Lock is an Android app that helps users interrupt impulsive app opens.

Current MVP behavior:

- the app watches a selected blocked app through `AccessibilityService`
- when that blocked app reaches the foreground, Intent Lock opens a lock screen
- the user must spend time in a selected control app
- progress is measured with `UsageStatsManager`
- once the challenge is complete, the blocked app receives a temporary unlock window

The app is:

- Android-only
- local-first
- single-device
- currently centered on configurable app-to-app intervention rules

## Important product constraints

- Treat this repository as a consumer-facing app for Brazilian Portuguese users.
- All visible UI copy must remain in PT-BR with correct accents.
- README and internal engineering docs may be in English when useful.
- Preserve responsiveness on small screens, especially in headers, cards, chips, and rule editor layouts.
- Avoid horizontal compression that breaks important PT-BR words badly.

## Current implementation status

The configurable intervention rules MVP is mostly implemented.

Implemented:

- configurable rules persisted locally
- multiple saved rules in storage and UI
- app picker for blocked and control apps
- rule validation and invalid-state handling
- interception driven by saved rules instead of hardcoded packages
- lock screen driven by runtime rule data
- challenge progress tracking
- temporary unlock credits
- focused unit tests for rule persistence and validation

Still open or worth validating:

- full manual device validation pass
- release signing setup
- future hardening against circumvention
- browser website blocking is spec'd but not implemented in the app code yet
