# Intent Lock

**Vision:** Intent Lock is a personal Android app that adds intentional friction to impulsive social media opens by requiring time in a chosen control app before temporary access is granted.
**For:** individual Android users who want a local-first tool to reduce compulsive app opens without relying on cloud services or account systems.
**Solves:** the gap between noticing an unwanted habit and interrupting it at the exact moment a blocked app is opened.

## Goals

- Ship an Android-only MVP where a user can configure one blocked app and one control app, complete a challenge, and receive a temporary unlock on their own physical device.
- Replace spike hardcodes with persisted user configuration so the product can be used without changing source code.
- Preserve the validated interception model while making setup, status, and recovery understandable enough for daily personal use.

## Tech Stack

**Core:**

- Framework: Android native + Jetpack Compose
- Language: Kotlin 2.2.20
- Platform: Android SDK 29-36, Java 17
- Persistence: `SharedPreferences` in the spike, with `DataStore` as the preferred MVP upgrade for configuration

**Key dependencies:**

- `androidx.activity:activity-compose`
- `androidx.compose.material3:material3`
- `AccessibilityService`
- `UsageStatsManager`
- Android package visibility queries

## Scope

**v1 includes:**

- one configurable blocked app and one configurable control app
- configurable challenge duration and unlock window
- local persistence for configuration, challenge session, and unlock state
- permission onboarding for Accessibility and Usage Access
- status surfaces for active challenge, progress, and current unlock state

**Explicitly out of scope:**

- iOS or cross-platform support
- accounts, sync, analytics backend, or subscriptions
- advanced anti-circumvention or cheat-proofing
- Play Store hardening and production release requirements
- social, streak, or gamification systems

## Constraints

- Timeline: no fixed external deadline; optimize for small, validated product increments
- Technical: the core behavior depends on Android system permissions and OEM-specific behavior
- Technical: interception reliability must continue to be validated on the target physical device, not only on an emulator
- Resources: personal, local-first project with a single primary device used as the source of validation
