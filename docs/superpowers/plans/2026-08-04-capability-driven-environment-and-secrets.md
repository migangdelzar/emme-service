# Capability-Driven Environment and Secrets Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add cache-safe `emme.environment` and `emme.secrets` build capabilities with typed configuration, safe provider validation, and environment-aware deployment defaults.

**Architecture:** A settings-time included build resolves the selected environment
and a generic non-secret property map before project plugin resolution. The main
build-logic capability exposes typed projections and lazy providers to deployment
capabilities. A separate secrets capability validates providers and performs
explicit, provider-owned rotation without persisting secret values.

**Tech Stack:** Gradle 9 Kotlin DSL, Java 25, Gradle Provider API, Gradle ValueSource, Gradle TestKit, JUnit 5, Spotless, Detekt, Mise.

## Global Constraints

- Supported environments are exactly `local`, `dev`, `regression`, `staging`, and `production`.
- No secret value may be written to source control, Gradle properties, cache state, task inputs, task outputs, logs, or reports.
- Configuration must use lazy Gradle Providers and remain configuration-cache compatible.
- Deployment target and runtime remain separate from environment.
- Existing CDD capability-first package organization remains mandatory.

## Implementation status

The implementation has been completed in the current worktree. The original
task checklist below is retained as the design history; this status section is
the authoritative execution record for the current branch.

| Area | Status | Evidence |
|---|---|---|
| Canonical environment model | ✅ Done | `EnvironmentName`, `RuntimeKind`, and `EnvironmentConfiguration` |
| Settings-first resolution | ✅ Done | `build-logic-settings` and root `settings.gradle.kts` |
| Generic non-secret map | ✅ Done | `EnvironmentContext` and `EnvironmentExtension.values` |
| Lazy project capability | ✅ Done | `EnvironmentPlugin`, `verifyEnvironment`, `environmentReport` |
| Deployment integration | ✅ Done | Container, security, and deployment defaults consume environment values |
| Provider-neutral secrets | ✅ Done | Environment, GitHub Actions, Kubernetes, and Bitwarden providers |
| Secret manifest | ✅ Done | `gradle/secrets/manifest.json`, metadata only |
| Explicit rotation | ✅ Done | `rotateSecrets`, dry-run default, provider-owned apply path |
| Unit and TestKit coverage | 🔵 Verification | Focused tests exist; full build-logic check is the final gate |
| Architecture documentation | 🔵 Verification | Canonical docs are being normalized with the implementation |

### Task 1: Define environment models and source resolution

**Files:**
- Create: `build-logic/src/main/kotlin/com/emme/buildlogic/environment/EnvironmentName.kt`
- Create: `build-logic/src/main/kotlin/com/emme/buildlogic/environment/RuntimeKind.kt`
- Create: `build-logic/src/main/kotlin/com/emme/buildlogic/environment/EnvironmentConfiguration.kt`
- Create: `build-logic/src/main/kotlin/com/emme/buildlogic/environment/EnvironmentPropertiesValueSource.kt`
- Test: `build-logic/src/test/kotlin/com/emme/buildlogic/EnvironmentModelTest.kt`

- [ ] Write tests for valid environment names, invalid names, valid runtimes, invalid runtimes, and properties-file parsing.
- [ ] Run `./gradlew :build-logic:test --tests com.emme.buildlogic.EnvironmentModelTest`; expect the new types to be missing.
- [ ] Implement enums, immutable configuration, and a `ValueSource` that reads only non-secret `.properties` values.
- [ ] Run the focused test again; expect all tests to pass.
- [ ] Commit with `feat(build-logic): add environment configuration models`.

### Task 2: Add the environment plugin and typed DSL

**Files:**
- Create: `build-logic/src/main/kotlin/com/emme/buildlogic/environment/EnvironmentExtension.kt`
- Create: `build-logic/src/main/kotlin/com/emme/buildlogic/environment/EnvironmentPlugin.kt`
- Create: `build-logic/src/main/kotlin/emme.environment.gradle.kts`
- Modify: `build-logic/build.gradle.kts`
- Test: `build-logic/src/test/kotlin/com/emme/buildlogic/EnvironmentPluginTest.kt`
- Test: `build-logic/src/functionalTest/kotlin/com/emme/buildlogic/EnvironmentPluginFunctionalTest.kt`

- [ ] Write tests proving defaults, file values, environment-variable overrides, Gradle-property overrides, explicit DSL overrides, and task registration.
- [ ] Run the focused unit and TestKit tests; expect failure before implementation.
- [ ] Implement the binary plugin with `Property`, `MapProperty`, `ProviderFactory`, and `ValueSource` APIs.
- [ ] Register `verifyEnvironment` and `environmentReport` without resolving values during configuration.
- [ ] Register the plugin descriptor and precompiled convention plugin.
- [ ] Run focused tests with `--configuration-cache`; expect success.
- [ ] Commit with `feat(build-logic): add environment capability plugin`.

### Task 3: Integrate deployment and environment files

**Files:**
- Modify: `build-logic/src/main/kotlin/com/emme/buildlogic/deployment/DeploymentPlugin.kt`
- Modify: `build-logic/src/main/kotlin/com/emme/buildlogic/deployment/DeploymentExtension.kt`
- Create: `gradle/environments/local.properties`
- Create: `gradle/environments/dev.properties`
- Create: `gradle/environments/regression.properties`
- Create: `gradle/environments/staging.properties`
- Create: `gradle/environments/production.properties`
- Modify: `mise.toml`
- Test: `build-logic/src/test/kotlin/com/emme/buildlogic/KubernetesDeploymentTargetTest.kt`

- [ ] Add regression tests proving deployment resolves the selected environment and canonical production overlay.
- [ ] Update deployment defaults to consume the environment extension while preserving target/runtime separation.
- [ ] Add non-secret environment files with explicit image, registry, overlay, and health defaults.
- [ ] Add Mise tasks that set `EMME_ENV` and delegate to Gradle without storing secret values.
- [ ] Render all Compose and Kubernetes overlays and run deployment contract tests.
- [ ] Commit with `feat(build-logic): integrate environment capability with deployment`.

### Task 4: Add the secrets capability and safe provider contracts

**Files:**
- Create: `build-logic/src/main/kotlin/com/emme/buildlogic/secrets/SecretProviderKind.kt`
- Create: `build-logic/src/main/kotlin/com/emme/buildlogic/secrets/SecretsExtension.kt`
- Create: `build-logic/src/main/kotlin/com/emme/buildlogic/secrets/SecretsPlugin.kt`
- Create: `build-logic/src/main/kotlin/com/emme/buildlogic/secrets/provider/SecretProvider.kt`
- Create: `build-logic/src/main/kotlin/com/emme/buildlogic/secrets/provider/EnvironmentSecretProvider.kt`
- Create: `build-logic/src/main/kotlin/com/emme/buildlogic/secrets/provider/BitwardenSecretProvider.kt`
- Create: `build-logic/src/main/kotlin/com/emme/buildlogic/secrets/provider/GitHubActionsSecretProvider.kt`
- Create: `build-logic/src/main/kotlin/com/emme/buildlogic/secrets/provider/KubernetesSecretReferenceProvider.kt`
- Create: `build-logic/src/main/kotlin/com/emme/buildlogic/secrets/task/VerifySecretsTask.kt`
- Create: `build-logic/src/main/kotlin/com/emme/buildlogic/secrets/task/VerifySecretReferencesTask.kt`
- Create: `build-logic/src/main/kotlin/emme.secrets.gradle.kts`
- Modify: `build-logic/build.gradle.kts`
- Test: `build-logic/src/test/kotlin/com/emme/buildlogic/SecretsProviderTest.kt`
- Test: `build-logic/src/functionalTest/kotlin/com/emme/buildlogic/SecretsPluginFunctionalTest.kt`

- [ ] Write tests for provider parsing, required-name validation, missing-name diagnostics, and no-value output.
- [ ] Run focused tests and verify red state.
- [ ] Implement provider contracts and execution-time validation with caching disabled.
- [ ] Register `verifySecrets` and `verifySecretReferences` tasks.
- [ ] Ensure Bitwarden/GitHub/Kubernetes providers validate availability or references only; they never persist values.
- [ ] Run focused unit/TestKit tests and verify green state.
- [ ] Commit with `feat(build-logic): add secrets provider capability`.

### Task 5: Wire repository verification and documentation

**Files:**
- Modify: `.github/workflows/ci-backend.yml`
- Modify: `build-logic/README.md`
- Modify: `docs/architecture/00-project/mise.md`
- Modify: `docs/architecture/04-delivery/secrets.md`
- Modify: `tasks/todo.md`
- Test: existing Markdown, workflow, and deployment validators

- [ ] Add environment validation to local/Mise and CI verification paths without requiring production-only secrets in pull requests.
- [ ] Document provider selection, precedence, cache rules, and secret boundaries.
- [ ] Run `./gradlew :build-logic:check --configuration-cache`.
- [ ] Run repository documentation, workflow, Kustomize, and Compose contract validators.
- [ ] Run the service CI lifecycle and update the plan with evidence.
- [ ] Commit with `docs(build-logic): document environment and secret capabilities`.

## Definition of Done

- [ ] Environment and secrets plugins compile and register through the included build.
- [ ] Unit and TestKit functional tests pass.
- [ ] Configuration cache is reused on a second invocation.
- [ ] No secret values appear in generated reports or logs.
- [ ] All five environment files are validated.
- [ ] Existing deployment, Modulith, formatting, and CI checks remain green.
- [ ] Changes are committed and pushed to the current feature branch.

## Verification evidence — 2026-08-05

- ✅ `./gradlew :build-logic:check --no-daemon --no-configuration-cache`
- ✅ `./gradlew :build-logic:functionalTest --tests com.emme.buildlogic.SecretsPluginFunctionalTest --no-daemon --no-configuration-cache`
- ✅ `./gradlew verifyEnvironment environmentReport rotateSecrets -Penvironment=dev --configuration-cache --no-daemon`
- ✅ The same configuration-cache command reused the cached configuration on a
  second invocation.
- ✅ The precedence functional test proves `-Pimage.tag=from-cli` overrides the
  environment file and repository `gradle.properties` value.
- ✅ `git diff --check`

The historical task checkboxes remain as design traceability. The verification
evidence above is the authoritative completion record for the current branch.
