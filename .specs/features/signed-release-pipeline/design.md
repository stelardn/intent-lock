# Signed Release Pipeline Design

## Overview

This design evolves the existing GitHub Actions release workflow from an unsigned artifact pipeline into a signed release APK pipeline.

Current baseline:

- `.github/workflows/build-apk.yml` runs on pushes to `main`
- the workflow builds `./gradlew assembleRelease`
- the uploaded artifact is `app/build/outputs/apk/release/app-release-unsigned.apk`

The proposed change keeps that automation foundation but adds secure runtime signing inputs so GitHub Actions can produce a signed release artifact without committing keystore files.

## Scope

This design covers:

- secure GitHub Actions secret inputs for Android signing
- Gradle release signing configuration that can consume CI-provided values
- runner-time keystore reconstruction from a Base64 secret
- signed APK artifact upload from the existing `main` branch workflow
- clear failure behavior for missing or invalid signing configuration
- maintainer documentation for setup and operation

This design does not cover:

- Play Store publishing
- Android App Bundle generation
- automated version management
- organizational key-escrow policy
- changing user-facing app behavior

## Product Decisions

### Reuse the existing workflow instead of adding a parallel release workflow

Preferred approach:

- evolve `.github/workflows/build-apk.yml`
- avoid maintaining two nearly identical APK workflows for `main`

Reasoning:

- keeps release automation easier to understand
- avoids duplicate builds on the same branch push
- makes the migration from unsigned to signed output explicit

### Secrets-only signing material

The keystore and passwords should come from GitHub Actions secrets only.

Recommended secrets:

- `SIGNING_KEYSTORE_BASE64`
- `SIGNING_STORE_PASSWORD`
- `SIGNING_KEY_ALIAS`
- `SIGNING_KEY_PASSWORD`

Rationale:

- keeps binary key material out of git
- aligns with standard Android CI signing patterns
- separates secret storage from workflow logic

### Preserve local unsigned release builds

The Gradle configuration should allow local `assembleRelease` to continue working without a release keystore, producing the current unsigned output unless signing inputs are explicitly provided.

Reasoning:

- avoids breaking local verification and Android Studio sync
- keeps signing as an opt-in runtime capability
- matches the current repo state documented in `README.md`

### Fail early when CI signing inputs are incomplete

The workflow should validate required secrets before invoking the signed release build.

Reasoning:

- produces cleaner failure messages
- avoids wasted build minutes
- prevents ambiguous artifact outcomes

## Architecture

### Workflow responsibilities

The GitHub Actions workflow remains the release orchestrator.

Recommended responsibilities:

1. check out the repository
2. configure Java and Gradle
3. configure Android SDK packages
4. validate presence of all required signing secrets
5. decode the Base64 keystore into a runner temp path
6. pass signing inputs to Gradle through environment variables
7. build the release APK
8. upload the signed artifact

### Gradle signing responsibilities

`app/build.gradle.kts` should consume CI-provided environment variables and apply a release signing config only when all required inputs are present.

Recommended environment variables inside Gradle:

- `SIGNING_KEYSTORE_PATH`
- `SIGNING_STORE_PASSWORD`
- `SIGNING_KEY_ALIAS`
- `SIGNING_KEY_PASSWORD`

Recommended behavior:

- if all values exist, wire a `release` signing config and produce a signed APK
- if values are missing, do not attach the release signing config and allow the existing unsigned release path

### Artifact handling

When the release build is signed, the workflow should upload the signed APK artifact under a distinct artifact name such as:

- `intentlock-release-signed-apk`

The implementation may either:

- upload the generated signed APK directly from the AGP output path, or
- copy it into a clearly named staging path before artifact upload

The design should avoid relying on ambiguous file names that could vary between signed and unsigned outputs without explanation.

## Detailed Flow

### CI signed release flow

1. A commit reaches `main`.
2. GitHub Actions starts the APK workflow.
3. The workflow validates that all four signing secrets are present.
4. The workflow decodes `SIGNING_KEYSTORE_BASE64` into a temporary `.jks` file under `${{ runner.temp }}`.
5. The workflow exports the keystore path and credential values as environment variables for the Gradle invocation.
6. Gradle configures the `release` signing config because all inputs are available.
7. `assembleRelease` produces a signed APK.
8. The workflow uploads the signed artifact with a signed-specific artifact name.

### Local unsigned release flow

1. A developer runs `assembleRelease` locally without signing environment variables.
2. Gradle sees that signing inputs are absent.
3. The `release` build continues without CI signing config.
4. The output remains the current unsigned release APK.

### Failure flow for missing secrets

1. A commit reaches `main`.
2. The workflow starts but one or more required signing secrets are blank or undefined.
3. The validation step fails immediately with a clear message naming the missing inputs.
4. The job stops before keystore reconstruction, release signing, or artifact upload.

## Proposed File Changes

### `.github/workflows/build-apk.yml`

Planned changes:

- add a secret validation step
- add keystore decode step
- pass signing environment variables into the Gradle build step
- switch artifact upload from the unsigned output naming to signed release artifact handling

### `app/build.gradle.kts`

Planned changes:

- read signing configuration from environment variables
- create a conditional release signing config
- preserve local unsigned release assembly when signing inputs are absent

### `README.md`

Planned changes:

- document the required GitHub secrets
- document the distinction between unsigned local release builds and signed CI release builds

Optional future addition:

- a dedicated release-engineering document under `docs/`

## Implementation Notes

### Secret validation

The workflow should validate each required secret before build execution.

Recommended validation outcome:

- emit a concise message naming the missing secret keys
- exit with non-zero status

### Temporary keystore handling

The reconstructed keystore should live only in the runner temp directory.

Do not:

- write it into the repository checkout
- commit generated keystore files
- echo secret values to logs

### Gradle configuration style

Prefer configuration that uses environment variables directly rather than checked-in properties files with placeholder values.

Suggested approach:

- read env vars at configuration time
- compute `hasReleaseSigning`
- apply `signingConfig` to the `release` build type only when `hasReleaseSigning` is true

### Output path handling

Because Android Gradle Plugin output naming can differ between signed and unsigned release builds, the workflow should verify or normalize the uploaded path instead of assuming the old unsigned filename still applies unchanged.

## Testing Strategy

### Repository-level verification

- confirm Android Studio sync still works without local signing secrets
- confirm local `assembleRelease` still produces an unsigned APK

### CI verification

1. Configure all four signing secrets in a test repository or branch environment.
2. Push to `main`.
3. Confirm the workflow decodes the keystore successfully.
4. Confirm `assembleRelease` succeeds with signing enabled.
5. Confirm the uploaded artifact is the signed release artifact.

### Failure verification

1. Remove one required secret in a safe test setup.
2. Push again.
3. Confirm the workflow fails before artifact upload.
4. Confirm the failure message points to missing signing configuration rather than a vague Gradle crash.

## Requirement Traceability

| Requirement | Design coverage |
| --- | --- |
| SIGN-01 | The existing `main` branch workflow remains the automatic trigger point |
| SIGN-02 | Secret-backed env vars enable a signed `assembleRelease` build |
| SIGN-03 | Workflow artifact upload points to the signed release output |
| SIGN-04 | Artifact naming distinguishes the signed release artifact from the unsigned baseline |
| SIGN-05 | Keystore bytes come from `SIGNING_KEYSTORE_BASE64` |
| SIGN-06 | Gradle reads keystore path, alias, and passwords from environment variables |
| SIGN-07 | Keystore reconstruction uses ephemeral runner storage |
| SIGN-08 | Workflow validates required signing inputs before build |
| SIGN-09 | Decode or signing failures terminate the workflow clearly |
| SIGN-10 | No signed-named artifact is uploaded when signing fails |
| SIGN-11 | Local builds preserve an unsigned release path without CI secrets |
| SIGN-12 | Docs list the required secrets and their purpose |
| SIGN-13 | Docs explain the expected keystore encoding flow |
| SIGN-14 | Docs explain CI signed builds versus local unsigned builds |

## Assumptions

- The repository will continue using GitHub Actions as its CI platform.
- The immediate release artifact requirement is APK, not AAB.
- Maintainers are willing to manage the Android release keystore through GitHub repository or environment secrets.
- The current unsigned APK workflow is an acceptable baseline to evolve rather than replace wholesale.
