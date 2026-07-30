# Emme Build Logic

Centralized Gradle build conventions for the Emme Nails Studio platform.

The canonical architecture is documented in
[`docs/architecture/00-project/build-logic.md`](../docs/architecture/00-project/build-logic.md).
This README is a short repository-local orientation and must not define a second
build-logic structure.

## Architectural style

`build-logic` uses Capability-Driven Design. It is intentionally different from
the DDD + Hexagonal package structure used by backend business modules.

```text
Business module                         Gradle build-logic
DDD bounded capability                  Build capability
domain / application / adapters         convention / plugin / task / provider
business invariant                      Gradle lifecycle behavior
```

The organizing unit is the build capability, not a global Kotlin type. Files that
change together live together under `container/`, `deployment/`, `publishing/`,
`security/`, `quality/`, or another owned capability.

## Target structure

```text
build-logic/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── config/
└── src/
    ├── main/kotlin/
    │   ├── emme.*.gradle.kts       # precompiled convention plugins
    │   └── com/emme/buildlogic/
    │       ├── core/               # shared build primitives
    │       ├── model/              # genuinely global build concepts
    │       ├── root/               # repository-wide coordination
    │       ├── container/          # plugin, DSL, tasks, providers, results
    │       ├── deployment/         # plugin, DSL, tasks, providers, results
    │       ├── publishing/         # plugin, DSL, tasks, providers, results
    │       ├── registry/           # registry boundary and results
    │       ├── security/           # scanner plugin, tasks, providers
    │       ├── quality/            # quality capability implementation
    │       └── git/                # Git ValueSources
    ├── test/kotlin/
    └── functionalTest/kotlin/
```

`internal/`, global `plugin/`, `extension/`, `task/`, `provider/`, and `value/`
packages are transitional implementation buckets. They are migrated by capability
ownership, not by a blind global rename. `internal/` becomes `core/`; capability
specific files move into their owning capability.

## Convention plugins

| Plugin | Purpose | Applies to |
|---|---|---|
| `emme.java-base` | Java toolchain, compiler, formatting, locking | All Java projects |
| `emme.java-library` | `java-library` and shared library defaults | Libraries |
| `emme.spring-module` | Spring module and Modulith baseline | Business modules |
| `emme.spring-application` | Spring Boot application packaging | Applications |
| `emme.spring-web` | MVC and validation capability | Web modules |
| `emme.persistence` | JPA, Liquibase, database testing | Persistent modules |
| `emme.messaging` | Messaging and broker testing | Messaging modules |
| `emme.modulith` | Modulith API and verification | Event-enabled modules |
| `emme.testing` | Unit-test conventions | Test source sets |
| `emme.test-fixtures` | Reusable test-fixture publication | Libraries/modules |
| `emme.integration-testing` | Real infrastructure test source sets | Integration-enabled projects |
| `emme.quality` | Formatting, analysis, coverage, dependency checks | Quality gates |
| `emme.api-compat` | Public API compatibility checks | Published libraries |
| `emme.container` | Container image lifecycle | Containerized applications |
| `emme.publishing` | SBOM, signing, metadata, releases | Published artifacts |
| `emme.deployment` | Compose/k3d/Kubernetes strategy dispatch | Deployable applications |

Convention plugins remain declarative, composable, small, opinionated, and reusable.
Complex behavior belongs in a capability-owned binary plugin.

## Module types plus capabilities

Module types answer **what the project is**. Capabilities answer **what the project
can do**.

```kotlin
plugins {
    id("emme.spring-module")
    id("emme.persistence")
    id("emme.messaging")
}
```

Do not create an `emme.everything` plugin. Explicit composition keeps individual
module build files declarative and makes build capability ownership visible.

## Capability execution flow

```text
module build.gradle.kts
    → precompiled convention plugin
    → binary capability plugin
    → typed extension and lazy tasks
    → provider port
    → technology adapter
    → external tool
```

Use typed extensions for configuration, custom tasks for execution, provider ports
for external tools, result models for stable outcomes, and `Provider`/`ValueSource`
for lazy external state.

## Deployment targets

```bash
# Docker Compose (default)
./gradlew composeUp composeDown composeLogs

# k3d local cluster
./gradlew k3dCreate k3dImport k3dDelete -Pemme.deployment.target=k3d

# Kubernetes
./gradlew k8sApply k8sDiff k8sRollout -Pemme.deployment.target=kubernetes
```

Override the target with `-Pemme.deployment.target=k3d` or
`EMME_DEPLOYMENT_TARGET=k3d`.

## Example: business module

```kotlin
// modules/booking/build.gradle.kts
plugins {
    id("emme.spring-module")
    id("emme.spring-web")
    id("emme.persistence")
    id("emme.modulith")
    id("emme.integration-testing")
}

dependencies {
    implementation(project(":modules:customer"))
    implementation(project(":modules:catalog"))
}
```

The module declares intent. `build-logic` owns how the declared capabilities are
implemented and verified.
