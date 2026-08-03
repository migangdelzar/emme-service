# Final service verification — 2026-08-03

## Scope

This is the current verification record for `feat/module-plans-normalization`.
It supersedes older partial verification notes for the module migrations,
`emme-platform` cutover, build-logic CDD normalization, Kafka/Spring Modulith,
typed configuration, endpoint versioning, and managed JDBC connection
execution.

The repository-local migration is complete for the current unreleased service.
The source of truth is `applications/emme-platform`; the removed
`applications/studio-api` project is not part of the Gradle graph.

## Architecture closure

| Area | Result | Evidence |
|---|---|---|
| Module package structure | Pass | Canonical DDD + Hexagonal packages, grouped public APIs, package metadata, and boundary tests |
| One service per use case | Pass | Application boundary tests across Identity, Catalog, Studio, Assistant, Notification, Payment, and Tenancy |
| Framework-free domain models | Pass | Domain package convention tests and module checks |
| Persistence ownership | Pass | Entity, Spring Data repository, mapper, and outbound adapter boundaries |
| External provider ownership | Pass | Provider implementations grouped by technology/channel behind application ports |
| Tenant isolation | Pass locally | Tenant predicates, current-tenant resolution, and cross-tenant regression tests |
| Endpoint versioning | Pass | Configured Spring MVC header-based `API-Version` resolver and controller mappings |
| Managed JDBC callbacks | Pass | Generic throwable connection function/consumer executor and Tenancy bootstrap integration |
| Build-logic CDD | Pass | Capability-owned Gradle plugins, extensions, tasks, providers, ValueSources, and TestKit |
| Optional native-image capability | Pass locally | `emme.native-image` convention, no-fallback configuration, dependency verification, and TestKit |
| Spring Modulith boundaries | Pass | Platform Modulith and layer tests |
| Kafka event streaming | Pass locally | Kafka Testcontainers publication/topic/key/payload test; no RabbitMQ runtime dependency |
| Application composition | Pass | `emme-platform` parity and application project graph tests |

## Verification commands

All commands completed successfully on 2026-08-03:

```text
./gradlew ci --no-daemon --no-configuration-cache --console=plain
./gradlew :modules:shared:integrationTest :modules:identity:integrationTest \
  :modules:tenancy:integrationTest :modules:catalog:integrationTest \
  :modules:studio:integrationTest :modules:assistant:integrationTest \
  :modules:notification:integrationTest :modules:payment:integrationTest \
  :modules:calendar:integrationTest :modules:booking:integrationTest \
  :applications:emme-platform:integrationTest --max-workers=1 \
  --no-daemon --no-configuration-cache --console=plain
./gradlew :applications:emme-platform:test \
  --tests com.emme.ModularityTest \
  --tests com.emme.LayerConventionTest \
  --tests com.emme.PlatformApplicationParityTest \
  :applications:emme-platform:bootJar \
  --no-daemon --no-configuration-cache --console=plain
node scripts/validate-markdown.mjs
git diff --check
gh pr checks 2 --watch=false
```

The full integration matrix completed with `BUILD SUCCESSFUL` in 5m 53s. The
platform architecture/boot gate completed in 17s. The remote checks for PR #2
all passed: build-logic, test, quality, verify, boot JAR, infrastructure,
secret scan, and dependency scan. Deployment remains skipped because PR #2 is
draft and no deployment environment was requested.

## Event-streaming closure

The isolated platform Kafka integration test completed with Spring Modulith
logging `No publications outstanding!`. Public event contracts use explicit
topic names and tenant partition keys. The application uses the JDBC
publication registry and the Kafka externalizer only in the enabled production
or dedicated integration profile.

Kafka broker-outage chaos, production credentials, and deployed consumer
recovery remain deployment-environment acceptance tests; they are not silently
represented as local proof.

## Known non-blocking test-harness diagnostic

Some separately launched PostgreSQL/Testcontainers Spring contexts log
shutdown-time `SQLSTATE 08006`, `57P01`, or EOF messages while Spring Modulith
queries its JDBC publication registry after the external PostgreSQL process has
already begun stopping. Every affected Gradle task still completed successfully
with zero failed tests. The existing test-container configuration disables
reuse and explicitly declares publication-registry ordering; the remaining
message is an external process-shutdown race, not an application assertion
failure. It is tracked as a test-harness cleanup improvement rather than hidden
or treated as production runtime evidence.

## Environment-dependent evidence not claimed as local completion

The following require secrets or an actual deployment environment and therefore
remain explicit release gates:

- credentialed Keycloak migration/recovery and live realm provisioning;
- credentialed Twilio, MessageBird, Vonage, payment-provider, and AI-provider
  calls;
- live PostgreSQL backup/restore and migration rollback;
- deployed JVM-versus-GraalVM native-image measurements;
- broker-outage and deployed Kafka consumer-recovery drills;
- live pool eviction under a real database outage.

These are operational acceptance tests, not missing package migrations. The
repository contains deterministic fakes, contract tests, integration tests,
typed configuration, and rollback documentation needed to execute them safely.

The optional `emme.native-image` capability is now available for the deployable
application spike. It is intentionally not applied to `emme-platform` by
default, so the JVM image and existing delivery behavior remain the safe
baseline until a GraalVM/Docker runner produces and measures the native image.

## Final decision

The branch is ready for code review and merge of the repository-local
architecture work. No legacy module implementation package, obsolete
`studio-api` application, RabbitMQ runtime integration, direct provider secret
lookup, or unqualified tenant persistence path was reintroduced. The remaining
environment-dependent gates must be run before production deployment, not
before merging this unreleased architecture branch.
