# Signed Release Pipeline Tasks

## Task Format

Each task follows the `tlc-spec-driven` phase-3 structure:

- What: exact deliverable
- Where: primary files or modules to change
- Depends on: prerequisites that must land first
- Reuses: existing code or patterns to leverage
- Done when: verifiable completion criteria
- Covers: requirement IDs from `spec.md`

## Execution Plan

```text
T001 -> T002 -> T003 -> T004
   \---------------> T005 ---^
```

Parallel opportunity after `T001`:

- `T002` and `T005` can proceed in parallel if documentation is drafted while implementation work starts

## Tasks

### T001 - Define CI signing inputs and fail-safe rules

- Status: Pending
- What: Finalize the required GitHub Actions secrets, CI environment variable names, and the expected failure behavior when signing inputs are missing or invalid.
- Where:
  - `.specs/features/signed-release-pipeline/`
  - implementation notes in workflow comments if the team wants them
- Depends on:
  - none
- Reuses:
  - the current `build-apk.yml` workflow trigger and release artifact goal
- Done when:
  - the required secrets are fixed and documented consistently
  - the fail-fast behavior is explicit enough to implement without ambiguity
  - signed and unsigned artifact naming expectations are decided
- Covers: `SIGN-04`, `SIGN-08`, `SIGN-09`, `SIGN-10`, `SIGN-12`

### T002 - Add conditional release signing support in Gradle

- Status: Pending
- What: Update the Android Gradle configuration so release signing can be enabled from environment variables without breaking local unsigned `assembleRelease`.
- Where:
  - `app/build.gradle.kts`
- Depends on: `T001`
- Reuses:
  - the existing `release` build type
  - the current local unsigned release path documented in `README.md`
- Done when:
  - Gradle reads keystore path, alias, and passwords from environment variables
  - the release signing config is attached only when all required values are present
  - local builds without signing inputs still sync and assemble an unsigned release APK
- Covers: `SIGN-02`, `SIGN-06`, `SIGN-11`

### T003 - Extend GitHub Actions to reconstruct the keystore and build the signed APK

- Status: Pending
- What: Update the existing APK workflow so it validates secrets, reconstructs the keystore from Base64, invokes the signed release build, and uploads the signed artifact.
- Where:
  - `.github/workflows/build-apk.yml`
- Depends on: `T001`, `T002`
- Reuses:
  - the current Java, Gradle, Android SDK, and `main` branch trigger setup
- Done when:
  - the workflow validates all required signing secrets before the build
  - the keystore is decoded into runner temp storage
  - `assembleRelease` runs with signing inputs
  - the uploaded artifact clearly represents the signed release output
- Covers: `SIGN-01`, `SIGN-02`, `SIGN-03`, `SIGN-04`, `SIGN-05`, `SIGN-07`, `SIGN-08`, `SIGN-09`, `SIGN-10`

### T004 - Verify success and failure paths in CI

- Status: Pending
- What: Run and document verification for both the happy path and the missing-secret failure path so the pipeline behavior is proven rather than assumed.
- Where:
  - GitHub Actions run history
  - optional verification notes in the active work log if the project keeps one
- Depends on: `T003`
- Reuses:
  - the repository’s existing local and CI verification practices
- Done when:
  - a configured run on `main` uploads the signed APK artifact
  - a test run with a missing secret fails before artifact upload
  - the resulting behavior matches the spec’s fail-safe expectations
- Covers: `SIGN-01`, `SIGN-03`, `SIGN-08`, `SIGN-09`, `SIGN-10`

### T005 - Document maintainer setup for signing secrets and local expectations

- Status: Pending
- What: Add maintainer-facing documentation for generating or obtaining the keystore, encoding it for GitHub Secrets, configuring required secrets, and understanding CI-vs-local release behavior.
- Where:
  - `README.md`
  - optional dedicated release-engineering doc under `docs/`
- Depends on: `T001`
- Reuses:
  - the current README build instructions
  - the repository’s existing engineering context docs
- Done when:
  - the required secrets and their purpose are documented
  - the expected Base64 keystore flow is documented
  - the distinction between signed CI releases and local unsigned release builds is documented clearly
- Covers: `SIGN-12`, `SIGN-13`, `SIGN-14`

## Requirement Coverage Summary

| Requirement | Covered by |
| --- | --- |
| `SIGN-01` | `T003`, `T004` |
| `SIGN-02` | `T002`, `T003` |
| `SIGN-03` | `T003`, `T004` |
| `SIGN-04` | `T001`, `T003` |
| `SIGN-05` | `T003` |
| `SIGN-06` | `T002` |
| `SIGN-07` | `T003` |
| `SIGN-08` | `T001`, `T003`, `T004` |
| `SIGN-09` | `T001`, `T003`, `T004` |
| `SIGN-10` | `T001`, `T003`, `T004` |
| `SIGN-11` | `T002` |
| `SIGN-12` | `T001`, `T005` |
| `SIGN-13` | `T005` |
| `SIGN-14` | `T005` |

## Notes

- This feature builds directly on the current unsigned APK workflow already committed in `.github/workflows/build-apk.yml`.
- The implementation should favor a safe migration path where local release builds remain usable even before CI signing is fully configured.
- If the team later adds AAB generation or Play publishing, that work should layer on top of this signed artifact foundation rather than replacing it.
