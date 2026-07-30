# ADR-0001: Precompiled Convention Plugins for Build Logic

## Status
Accepted (2026-07-10)

## Context
Emme Nails is a Spring Modulith monolith with 13 business modules, 2 applications, a platform BOM, and shared test fixtures. Every module needs consistent Java compilation, testing, formatting, and dependency rules. Without centralized conventions, each `build.gradle.kts` duplicates 20-30 lines of configuration.

## Decision
Use Gradle precompiled script plugins (`.gradle.kts` in `build-logic/src/main/kotlin/`) for repetitive convention, and binary Kotlin plugins for complex behavior requiring extensions, tasks, or build services.

### Convention plugins (precompiled)
- `emme.java-base` — Java 25, Spotless, Checkstyle, dependency locking
- `emme.java-library` — java-library + testing
- `emme.spring-module` — Spring context + Modulith events
- `emme.spring-application` — Spring Boot + bootJar
- `emme.persistence` — JPA + Liquibase + Testcontainers
- `emme.spring-web` — Web MVC + validation
- `emme.messaging` — Kafka + Testcontainers
- `emme.modulith` — Modulith API + verification
- `emme.testing` — JUnit 5, AssertJ, Mockito
- `emme.test-fixtures` — testFixtures source set configuration
- `emme.integration-testing` — Testcontainers PostgreSQL
- `emme.quality` — Spotless, JaCoCo, OWASP, dependency analysis
- `emme.container` — Docker/Podman image build
- `emme.publishing` — SBOM, signing, version verification
- `emme.deployment` — Compose/k3d/K8s deployment strategy dispatch

### Binary plugins
- `EmmeRootPlugin` — root CI aggregation tasks
- `EmmeContainerPlugin` — container image build via Docker/Podman
- `EmmePublishingPlugin` — SBOM, signing, version verification
- `EmmeDeploymentPlugin` — strategy pattern for compose/k3d/kubernetes targets

## Consequences
- **Positive**: Single source of truth per concern. Module build files average 6-10 lines.
- **Positive**: New modules inherit all conventions with 1 plugin declaration.
- **Positive**: IDE autocomplete via `EmmeDependencies` typed catalog wrapper.
- **Negative**: Precompiled scripts cannot use `libs.xxx` type-safe accessor; use `VersionCatalogsExtension` or `EmmeDependencies` instead.
- **Negative**: Plugin changes require `build-logic` recompilation.
