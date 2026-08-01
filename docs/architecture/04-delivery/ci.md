# Continuous Integration

## Purpose

CI provides fast feedback first, then progressively more expensive confidence checks. It must validate the same boundaries described in this handbook.

## Pipeline stages

```text
format / compile
       ↓
unit tests
       ↓
architecture + contract tests
       ↓
integration tests
       ↓
security / dependency / image checks
       ↓
release artifact
```

```mermaid
flowchart LR
    COMMIT[Commit] --> FAST[Format + compile + unit]
    FAST --> ARCH[Architecture + contract]
    ARCH --> SECURITY[Security + dependency + image]
    SECURITY --> ARTIFACT[Traceable artifact]
```

## Repository workflows

| Workflow | Trigger | Required evidence |
|---|---|---|
| `ci-backend.yml` | Pushes and pull requests targeting `main` | Markdown validation, static Gradle quality, platform tests, module-boundary tests, both bootable JARs |
| `ci-module-boundaries.yml` | Pushes and pull requests | Spring Modulith and ArchUnit verification |
| `security-scan.yml` | Pushes, pull requests, weekly schedule | Gitleaks always; OWASP Dependency-Check when `NVD_API_KEY` is configured |
| `dependency-review.yml` | Manual dispatch until GitHub Dependency Graph is enabled | High-severity dependency changes are rejected when supported |

The backend quality job runs Gradle `ci` with application test tasks excluded so
formatting, compilation, Spotless, Checkstyle, and project checks provide fast
feedback. Dedicated jobs then run the existing platform test suite and module
boundary suite. Build-logic has its own job for unit tests, Detekt, Spotless,
plugin validation, and Gradle TestKit functional tests. Integration tests that
require external infrastructure remain explicit release or environment jobs.

## Rules

- Fail fast on formatting, compilation, and unit-test failures.
- Keep unit tests independent from Docker and external services.
- Run integration tests against real infrastructure where compatibility matters.
- Run Gradle TestKit functional tests for build-logic changes.
- Cache dependencies safely but do not cache secrets or mutable release artifacts.
- Publish test reports, coverage, architecture violations, and security findings as CI artifacts.
- Keep release jobs protected by branch, approval, and environment policy.

## Change-aware execution

CI may select focused jobs based on changed paths, but the protected branch must retain a complete verification path. Build-logic changes should exercise all affected convention and functional tests.

## CI controls

### Required gates

| Gate | Blocks merge/release when |
|---|---|
| Formatting/type/build | Source cannot be compiled or standardized |
| Unit/module tests | Business or module behavior fails |
| Architecture | Cycles, internal imports, or forbidden dependencies appear |
| Contract | API/event compatibility breaks |
| Integration | Real database/provider behavior fails |
| Frontend quality | Build, accessibility, or critical component tests fail |
| Security | Policy-defined vulnerabilities or secret leaks appear |
| Artifact | SBOM, provenance, image, or version verification fails |

### Supply-chain controls

- Pin actions/plugins/dependencies where supported and review updates.
- Use least-privilege CI tokens and separate read/build/publish permissions.
- Do not expose production secrets to pull-request jobs.
- Produce signed, traceable artifacts with commit and dependency metadata.
- Keep dependency and build caches scoped, integrity-checked, and free of secrets.
- Keep local Gradle verification metadata and OWASP dependency analysis blocking;
  the optional GitHub dependency-review workflow must be enabled by repository
  administration before it is restored as an automatic pull-request gate.
- Configure the repository `NVD_API_KEY` secret before making OWASP analysis a
  required pull-request check. Without it, the dependency job is intentionally
  skipped instead of timing out against the public NVD rate limit.

### Test execution policy

- Parallelize isolated tests but isolate databases, tenants, ports, and files.
- Retry only infrastructure-transient failures and report the original failure.
- Quarantine flaky tests with owner and expiry; do not hide them permanently.
- Publish reports, traces, coverage, architecture diagrams, SBOMs, and security findings.

### CI checklist

- [ ] Clean checkout builds without local-only state.
- [ ] All required gates run for protected branches.
- [ ] Build-logic changes run TestKit functional coverage.
- [ ] Pull-request jobs cannot access production secrets.
- [ ] Artifacts are immutable, traceable, and retained according to policy.
- [ ] Failures provide actionable logs and diagnostic artifacts.
