# Capability-Driven Environment and Secrets Design

## Status

Approved for implementation on `feat/enterprise-module-template-conformance`.

## Goal

Provide one typed, lazy, cache-safe build configuration boundary for the
`local`, `dev`, `regression`, `staging`, and `production` environments, plus a
separate secrets capability that validates provider availability and required
secret names without storing or exposing secret values.

## Architecture

`environment` owns non-secret build and deployment configuration. A small
settings-time included build resolves an immutable context before project
plugin resolution. The project capability exposes typed projections over the
same arbitrary map and applies the following precedence:

```text
environment defaults
    ↓
gradle/environments/<environment>.properties
    ↓
EMME_* process variables
    ↓
Gradle project properties (-P or gradle.properties)
    ↓
explicit DSL values
```

The map is intentionally generic so new build properties do not require a new
settings-plugin API. Secret-like keys are rejected before they enter the
context.

Within the Gradle sources, the effective precedence is explicit:

| Priority | Source |
|---:|---|
| 1 (highest) | command-line project properties such as `-Pimage.tag=value` |
| 2 | `gradle.properties` |
| 3 | `EMME_*` process variables |
| 4 | `gradle/environments/<environment>.properties` |
| 5 (lowest) | capability defaults |

Explicit extension DSL assignments remain higher than conventions when a build
script intentionally overrides a capability value. Secret-like command-line
properties are still available directly to the secrets capability, but are
excluded from the shared non-secret environment map.

`secrets` is a separate capability. It validates required names and provider
availability. It never writes secret values to files, Gradle properties,
configuration-cache state, build-cache outputs, logs, or reports.

## Capabilities

```text
emme.environment
    ├── typed environment name, target, runtime, image, registry, health URL
    ├── arbitrary non-secret property map and typed projections
    ├── lazy Provider/ValueSource resolution
    ├── environment report and validation tasks
    └── deployment capability integration

emme.secrets
    ├── required secret-name declarations
    ├── environment/GitHub injected secret validation
    ├── Bitwarden session/provider availability validation
    ├── Kubernetes secret-reference validation
    └── no-value secret diagnostics
```

## Source Layout

```text
gradle/environments/
├── local.properties
├── dev.properties
├── regression.properties
├── staging.properties
└── production.properties

build-logic/src/main/kotlin/com/emme/buildlogic/
├── environment/
│   ├── EnvironmentExtension.kt
│   ├── EnvironmentPlugin.kt
│   ├── EnvironmentConfiguration.kt
│   ├── EnvironmentName.kt
│   ├── EnvironmentPropertiesValueSource.kt
│   └── RuntimeKind.kt
└── secrets/
    ├── SecretsExtension.kt
    ├── SecretsPlugin.kt
    ├── SecretProviderKind.kt
    ├── provider/
    │   ├── SecretProvider.kt
    │   ├── EnvironmentSecretProvider.kt
    │   ├── BitwardenSecretProvider.kt
    │   ├── GitHubActionsSecretProvider.kt
    │   └── KubernetesSecretReferenceProvider.kt
    └── task/
        ├── VerifySecretsTask.kt
        └── VerifySecretReferencesTask.kt
```

The separate settings build contains only the early-resolution boundary:

```text
build-logic-settings/
└── src/main/kotlin/com/emme/buildlogic/settings/EnvironmentSettingsPlugin.kt
```

It intentionally uses a normalized string validated against the available
environment property filenames. Typed models such as `EnvironmentName` and
`RuntimeKind` exist only in the main `build-logic` build, so the bootstrap build
does not duplicate build-domain classes.

## Gradle Cache and Configuration-Cache Rules

- Read properties through Gradle `ProviderFactory` and `ValueSource` APIs.
- Do not call `System.getenv`, `System.getProperty`, or eager file reads during
  plugin configuration.
- Environment report tasks declare all non-secret inputs and outputs.
- Secret verification tasks are disabled for caching and read secret values
  only during task execution.
- Secret values never become task inputs or outputs.
- Secret rotation is provider-owned and explicit: `rotateSecrets` defaults to
  `dry-run`; applying a rotation requires `-Psecret.rotation.mode=apply` and a
  selected provider. The manifest stores logical names, provider references,
  and generation policy only—never secret values.
- Deployment and container tasks keep explicit inputs and outputs so Gradle
  build caching remains valid.

## Provider Boundaries

GitHub Actions does not expose secret values through its API. Its provider is
therefore the environment-variable provider after the workflow injects
`${{ secrets.NAME }}`. Bitwarden integration validates an authenticated CLI
session and is responsible for injection outside Gradle. Kubernetes validates
declared `secretKeyRef` names and leaves value retrieval to Kubernetes or an
External Secrets controller.

## DSL

```kotlin
plugins {
    id("emme.environment")
    id("emme.secrets")
}

emmeEnvironment {
    name.set(EnvironmentName.STAGING)
    target.set(DeploymentTarget.K3S)
    runtime.set(RuntimeKind.JVM)
}

emmeSecrets {
    provider.set(SecretProviderKind.AUTO)
    required("DB_PASSWORD")
    required("KAFKA_SASL_JAAS_CONFIG")
}
```

## Verification

- Unit tests cover environment parsing, precedence, allowed names, and secret
  provider selection.
- Functional TestKit tests prove the plugins register tasks without resolving
  values during configuration and remain compatible with the configuration
  cache.
- Repository validation renders every Compose and Kubernetes environment
  overlay.
- CI runs `verifyEnvironment` and `verifySecrets` only where the required
  provider contract is available.
- Provider adapters own retrieval, update, and rotation protocols. Gradle only
  orchestrates declarations and reports safe statuses.

## Non-Goals

- No custom secret vault is introduced.
- No plaintext secret synchronization task is created.
- No environment-specific business-module configuration is added.
- No JSON parser or third-party configuration framework is added.
