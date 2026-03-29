# Signed Release Pipeline Specification

## Problem Statement

Intent Lock now has a GitHub Actions workflow at `.github/workflows/build-apk.yml` that builds and uploads a release APK artifact on pushes to `main`. That workflow currently produces `app-release-unsigned.apk`, which is useful for CI verification but not suitable for normal distribution.

The next release-engineering gap is a safe, repeatable way to generate a signed Android release artifact from GitHub Actions without committing keystores or passwords to the repository.

## Goals

- [ ] Generate a signed Android release APK artifact from GitHub Actions on pushes to `main`.
- [ ] Keep signing material out of the repository by sourcing it from GitHub Actions secrets.
- [ ] Fail safely and clearly when signing material is missing, invalid, or incomplete.
- [ ] Preserve a reasonable local developer flow so unsigned local builds still remain possible when no release keystore is configured.

## Out of Scope

Explicitly excluded for this feature slice.

| Feature | Reason |
| --- | --- |
| Automatic Google Play upload | Distribution and store release management are a separate workflow |
| Automatic version bumping or changelog generation | Useful later, but independent from signed artifact generation |
| AAB publishing pipeline | The immediate need is a signed APK artifact |
| Key rotation policy and operational incident response | Important operational work, but broader than this implementation slice |
| End-user visible app changes | This feature is build and release infrastructure only |

---

## User Stories

### P1: Build A Signed APK On Main Branch ⭐ MVP

**User Story**: As a maintainer, I want GitHub Actions to produce a signed release APK when code lands on `main` so that CI artifacts are closer to a distributable Android build.

**Why P1**: The current pipeline stops at an unsigned artifact, which is not enough for release-oriented testing or future distribution work.

**Acceptance Criteria**:

1. WHEN a commit is pushed to `main` THEN the GitHub Actions workflow SHALL run automatically.
2. WHEN required signing secrets are available THEN the workflow SHALL build a signed release APK.
3. WHEN the build completes successfully THEN the workflow SHALL upload the signed APK as a GitHub Actions artifact.
4. WHEN the artifact is uploaded THEN its naming SHALL clearly distinguish it from the current unsigned release output.

**Independent Test**: Push a change to `main` in a repository configured with valid signing secrets, then confirm the workflow uploads a signed release APK artifact.

---

### P1: Keep Signing Material Out Of The Repository ⭐ MVP

**User Story**: As a maintainer, I want the release keystore and passwords to stay in GitHub secrets so that sensitive signing material is never committed to the repo.

**Why P1**: Release signing keys are sensitive and should not live in version control.

**Acceptance Criteria**:

1. WHEN the pipeline needs the keystore THEN it SHALL reconstruct it from secret-backed data at runtime.
2. WHEN Gradle signs the release build THEN keystore path, alias, and passwords SHALL come from CI environment inputs rather than committed source files.
3. WHEN the workflow finishes THEN the signing material SHALL remain limited to ephemeral runner storage.
4. WHEN maintainers inspect the repository THEN they SHALL not need to commit `.jks`, `.keystore`, or plaintext credential files to enable signing.

**Independent Test**: Inspect the implementation and confirm the repository contains no committed signing key material while the workflow still signs correctly using secrets.

---

### P1: Fail Safely When Signing Is Not Configured

**User Story**: As a maintainer, I want missing or incomplete signing configuration to fail clearly so that CI does not silently produce the wrong release artifact.

**Why P1**: A misleading fallback could make an unsigned artifact look like a signed release and create release mistakes.

**Acceptance Criteria**:

1. WHEN one or more required signing secrets are missing THEN the workflow SHALL stop with an explicit failure before attempting release signing.
2. WHEN the keystore cannot be decoded or used THEN the workflow SHALL fail with a clear signing-related step.
3. WHEN signed release generation is expected but unavailable THEN the workflow SHALL not upload a misleading artifact under the signed release name.
4. WHEN local developers run the project without CI secrets THEN the repository SHALL still support a non-distribution local release build path.

**Independent Test**: Run the workflow in a test repository with one required secret intentionally removed, then confirm the job fails before artifact upload and reports the missing configuration.

---

### P2: Document The Signing Setup For Future Maintainers

**User Story**: As a maintainer, I want repository documentation for the signing pipeline so that future setup and key handling are understandable without reverse engineering the workflow.

**Why P2**: Signing infrastructure is operationally sensitive and easy to misconfigure when undocumented.

**Acceptance Criteria**:

1. WHEN a maintainer reads the release setup documentation THEN they SHALL see which secrets are required and what each one represents.
2. WHEN a maintainer prepares a keystore for CI THEN the docs SHALL describe the expected encoding or upload format.
3. WHEN a maintainer needs to reason about local vs CI release builds THEN the docs SHALL explain that CI signs releases while local builds may remain unsigned unless configured separately.

**Independent Test**: Have a maintainer unfamiliar with the implementation read the docs and identify the required secrets and expected workflow behavior without opening the source first.

---

## Edge Cases

- WHEN the release build is triggered on `main` but the signing secrets belong to the wrong key alias or passwords THEN the workflow SHALL fail rather than upload a bad artifact.
- WHEN the repository already contains the unsigned release workflow behavior THEN the signed release changes SHALL avoid duplicate or conflicting artifact names.
- WHEN the Android Gradle configuration is evaluated on a machine without signing secrets THEN local sync and local unsigned release assembly SHALL remain possible.
- WHEN the keystore file is reconstructed on the runner THEN it SHALL be written to a temporary path rather than a committed project path.
- WHEN future work adds AAB generation or store publishing THEN this signed APK pipeline SHALL remain a cleanly separable step.

---

## Requirement Traceability

| Requirement ID | Story | Phase | Status |
| --- | --- | --- | --- |
| SIGN-01 | P1: Build A Signed APK On Main Branch | Design | Pending |
| SIGN-02 | P1: Build A Signed APK On Main Branch | Design | Pending |
| SIGN-03 | P1: Build A Signed APK On Main Branch | Design | Pending |
| SIGN-04 | P1: Build A Signed APK On Main Branch | Design | Pending |
| SIGN-05 | P1: Keep Signing Material Out Of The Repository | Design | Pending |
| SIGN-06 | P1: Keep Signing Material Out Of The Repository | Design | Pending |
| SIGN-07 | P1: Keep Signing Material Out Of The Repository | Design | Pending |
| SIGN-08 | P1: Fail Safely When Signing Is Not Configured | Design | Pending |
| SIGN-09 | P1: Fail Safely When Signing Is Not Configured | Design | Pending |
| SIGN-10 | P1: Fail Safely When Signing Is Not Configured | Design | Pending |
| SIGN-11 | P1: Fail Safely When Signing Is Not Configured | Design | Pending |
| SIGN-12 | P2: Document The Signing Setup For Future Maintainers | Design | Pending |
| SIGN-13 | P2: Document The Signing Setup For Future Maintainers | Design | Pending |
| SIGN-14 | P2: Document The Signing Setup For Future Maintainers | Design | Pending |

**ID format:** `[CATEGORY]-[NUMBER]`

**Coverage:** 14 total, 0 mapped to tasks before the first task pass.

---

## Success Criteria

- [ ] A push to `main` can produce a signed APK artifact in GitHub Actions when signing secrets are configured.
- [ ] The repository continues to avoid committed keystores and plaintext signing credentials.
- [ ] Missing or invalid signing configuration fails loudly enough to avoid confusing signed and unsigned release artifacts.
- [ ] Future maintainers can configure the pipeline from repository docs without needing ad hoc tribal knowledge.
