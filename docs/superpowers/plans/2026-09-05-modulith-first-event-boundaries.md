# Modulith-First Event Boundaries with Kafka Deferred Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` (recommended) or `superpowers:executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make Spring Modulith the only active asynchronous event provider for the initial Emme Nails runtime and defer Kafka provider/container activation until a real external consumer exists.

**Architecture:** Keep the modular monolith and the PostgreSQL-backed Spring Modulith event publication registry. Current event records remain internal facts and are consumed with `@ApplicationModuleListener`; Kafka build/test artifacts remain dormant and are enabled only through an explicit deferred-test or external-consumer decision.

**Tech Stack:** Java 25, Gradle, Spring Boot 4.1.x, Spring Modulith 2.1.x, PostgreSQL, Liquibase, JUnit 5, AssertJ, Testcontainers, Docker Compose, Kubernetes/Kustomize, GitHub Actions.

## Global Constraints

- Internal events do not use `@Externalized` and do not require Kafka.
- Kafka may be enabled only for an external consumer, independently deployed worker, replayable stream, or explicitly approved delivery boundary.
- Keep `spring-modulith-starter-core`, `spring-modulith-starter-jdbc`, Liquibase ownership of `event_publication`, and outstanding-publication republication.
- Do not import Kafka types into domain or application orchestration packages.
- Do not delete the reusable Kafka build/test capability; keep it dormant and explicitly gated.
- Kafka Compose, Kubernetes broker settings, and Testcontainers creation must not run in the default local, test, production, or CI path.
- Every behavior change follows Red → Green → Refactor, with the failing test written before the implementation change.
- Preserve unrelated working-tree changes; stage only files belonging to this event-boundary change.
- Use the existing repository Java/Gradle conventions and run commands with `--no-parallel --no-configuration-cache` when diagnosing failures.

## File Map and Ownership

### Event contracts and application tests

- Modify: `modules/tenancy/src/main/java/com/emme/tenancy/api/event/TenantCreated.java`
- Modify: `modules/tenancy/src/main/java/com/emme/tenancy/api/event/TenantActivated.java`
- Modify: `modules/appointments/src/main/java/com/emme/appointments/api/event/AppointmentCreated.java`
- Modify: `modules/appointments/src/main/java/com/emme/appointments/api/event/AppointmentCancelled.java`
- Modify: `modules/appointments/src/main/java/com/emme/appointments/api/event/AppointmentRescheduled.java`
- Modify: `modules/assistant/src/main/java/com/emme/assistant/api/event/LearningCandidateEvaluationRequested.java`
- Rename/modify: `applications/emme-platform/src/test/java/com/emme/KafkaEventContractTest.java` → `applications/emme-platform/src/test/java/com/emme/EventContractTest.java`
- Modify: `applications/emme-platform/src/test/java/com/emme/PlatformApplicationParityTest.java`
- Modify: `applications/emme-platform/src/integrationTest/java/com/emme/KafkaEventStreamingIntegrationTest.java`

### Active application configuration

- Modify: `applications/emme-platform/src/main/resources/application.yml`
- Modify: `applications/emme-platform/src/main/resources/application-production.yml`
- Modify: `applications/emme-platform/src/main/resources/application-test.yml`
- Modify: `applications/emme-platform/build.gradle.kts`
- Delete: `applications/emme-platform/src/main/java/com/emme/configuration/KafkaEventStreamingProperties.java`
- Delete: `applications/emme-platform/src/test/java/com/emme/KafkaEventStreamingPropertiesTest.java`

The deferred profile remains available and is not part of the default runtime:

- Preserve/gate: `applications/emme-platform/src/integrationTest/resources/application-kafka-test.yml`

### Deferred container, deployment, and CI artifacts

- Modify/comment as deferred: `deployment/compose/compose.environment-kafka.yaml`
- Modify: `infra/kubernetes/overlays/k3s-production-jvm/kustomization.yaml`
- Modify: `infra/kubernetes/overlays/k3s-production-native/kustomization.yaml`
- Modify: `.github/workflows/ci-backend.yml`
- Preserve but do not run by default: `deployment/compose/compose.kafka.contract.test.mjs`
- Preserve but do not activate by default: `libraries/test-containers/src/main/java/com/emme/testing/integration/container/KafkaContainerConfiguration.java`
- Preserve but do not activate by default: `libraries/test-containers/src/main/java/com/emme/testing/integration/annotation/KafkaIntegrationTest.java`
- Preserve dormant capability: `build-logic/src/main/kotlin/emme.messaging.gradle.kts`

### Documentation and traceability

- Modify: `docs/architecture/01-backend/events.md`
- Modify: `docs/adr/0005-spring-modulith-kafka-event-streaming.md`
- Modify: `docs/adr/0006-mvp-low-cost-runtime-boundary.md`
- Modify: `docs/ai-platform/technical-specification.md`
- Modify: `docs/ai-platform/implementation-plan.md`
- Modify: `docs/superpowers/plans/2026-09-04-repository-framework-first-refactoring.md`

Expected implementation impact: approximately 24–26 files, including the design
and test/configuration/documentation updates. The exact count can be reduced if
the deployment overlays already omit unused Kafka secret patches in the target
environment.

---

## Task 1: Classify current event records as Modulith-internal

**Files:**

- Modify: the six event records listed in the event-contract file map.
- Rename/modify: `applications/emme-platform/src/test/java/com/emme/KafkaEventContractTest.java` → `applications/emme-platform/src/test/java/com/emme/EventContractTest.java`

**Interfaces:**

- Consumes: existing immutable event records and current `@Externalized` declarations.
- Produces: an internal event contract in which all six current events have no `Externalized` annotation and remain publishable through Spring Modulith.

- [ ] **Step 1: Write the failing contract test**

Rename the test class to `EventContractTest` and replace the Kafka-routing assertions with an explicit internal-event set. Add this focused assertion before removing annotations:

```java
@Test
void currentEventsRemainInternalUntilAnExternalConsumerIsApproved() {
  assertThat(
          List.of(
              TenantCreated.class,
              TenantActivated.class,
              AppointmentCreated.class,
              AppointmentCancelled.class,
              AppointmentRescheduled.class,
              LearningCandidateEvaluationRequested.class))
      .allMatch(eventType -> !eventType.isAnnotationPresent(Externalized.class));
}
```

Keep the existing record immutability, stable identifier, framework-type exclusion, and unsupported Rabbit/AMQP assertions. Remove assertions that require business events to be externalized.

- [ ] **Step 2: Run the test to verify it fails**

Run:

```bash
./gradlew :applications:emme-platform:test --tests com.emme.EventContractTest --no-parallel --no-configuration-cache
```

Expected: FAIL because the six current event records still have `@Externalized` annotations and the old class name may still be referenced by CI.

- [ ] **Step 3: Remove external transport metadata from the six event records**

For each listed event, remove the import and annotation while preserving the record fields, validation, event IDs, tenant IDs, timestamps, and public package location. For example:

```java
// Remove:
import org.springframework.modulith.events.Externalized;

// Remove:
@Externalized("emme.studio.appointment-created::#{#this.tenantId()}")

public record AppointmentCreated(/* existing immutable fields */) {}
```

Do not replace the annotations with Kafka or Spring Kafka imports. The event publisher ports and Spring Modulith listeners remain unchanged in this task.

- [ ] **Step 4: Run the contract test to verify it passes**

Run:

```bash
./gradlew :applications:emme-platform:test --tests com.emme.EventContractTest --no-parallel --no-configuration-cache
```

Expected: PASS with zero skipped tests.

- [ ] **Step 5: Commit the event classification change**

```bash
git add modules/tenancy/src/main/java/com/emme/tenancy/api/event/TenantCreated.java \
  modules/tenancy/src/main/java/com/emme/tenancy/api/event/TenantActivated.java \
  modules/appointments/src/main/java/com/emme/appointments/api/event/AppointmentCreated.java \
  modules/appointments/src/main/java/com/emme/appointments/api/event/AppointmentCancelled.java \
  modules/appointments/src/main/java/com/emme/appointments/api/event/AppointmentRescheduled.java \
  modules/assistant/src/main/java/com/emme/assistant/api/event/LearningCandidateEvaluationRequested.java \
  applications/emme-platform/src/test/java/com/emme/KafkaEventContractTest.java \
  applications/emme-platform/src/test/java/com/emme/EventContractTest.java
git commit -m "refactor(events): classify initial facts as Modulith-only"
```

## Task 2: Remove the active Kafka provider configuration

**Files:**

- Modify: `applications/emme-platform/src/main/resources/application.yml`
- Modify: `applications/emme-platform/src/main/resources/application-production.yml`
- Modify: `applications/emme-platform/src/main/resources/application-test.yml`
- Modify: `applications/emme-platform/build.gradle.kts`
- Delete: `applications/emme-platform/src/main/java/com/emme/configuration/KafkaEventStreamingProperties.java`
- Delete: `applications/emme-platform/src/test/java/com/emme/KafkaEventStreamingPropertiesTest.java`
- Modify: `applications/emme-platform/src/test/java/com/emme/PlatformApplicationParityTest.java`

**Interfaces:**

- Consumes: Task 1’s internal event classification and existing Modulith JDBC configuration.
- Produces: application profiles that do not bind Kafka provider properties or require broker credentials.

- [ ] **Step 1: Add a failing profile-safety test**

Extend `PlatformApplicationParityTest` with a source-level contract that reads the base, production, and test profiles and asserts that active profiles do not contain Kafka provider configuration:

```java
@Test
void mvpProfilesDoNotActivateKafkaProvider() throws IOException {
  List<String> profiles =
      List.of(
          "applications/emme-platform/src/main/resources/application.yml",
          "applications/emme-platform/src/main/resources/application-production.yml",
          "applications/emme-platform/src/main/resources/application-test.yml");

  profiles.forEach(
      profile ->
          assertThat(readSource(profile))
              .as("Kafka provider must remain deferred in %s", profile)
              .doesNotContain("EMME_KAFKA_EVENTS_ENABLED:true")
              .doesNotContain("app:\n  messaging:\n    kafka:")
              .doesNotContain("spring:\n  kafka:");
}
```

- [ ] **Step 2: Run the profile-safety test to verify it fails**

Run:

```bash
./gradlew :applications:emme-platform:test --tests com.emme.PlatformApplicationParityTest --no-parallel --no-configuration-cache
```

Expected: FAIL because `application.yml` and `application-production.yml` currently define Kafka provider settings and production defaults Kafka to enabled.

- [ ] **Step 3: Remove active Kafka profile settings**

Remove the `spring.kafka` and `app.messaging.kafka` blocks from `application.yml` and `application-production.yml`. Remove the disabled externalization block from `application-test.yml`; retain its Modulith JDBC test schema settings. Keep the explicit `application-kafka-test.yml` profile for the deferred test path.

In `applications/emme-platform/build.gradle.kts`, retain `id("emme.messaging")` and its Kafka test dependencies only because the explicitly gated deferred Kafka integration test still needs to compile. Add a default exclusion for that test:

```kotlin
val kafkaDeferred =
  providers.gradleProperty("emme.kafka-deferred").map(String::toBoolean).orElse(false)

tasks.named<Test>("integrationTest") {
  if (!kafkaDeferred.get()) {
    exclude("**/KafkaEventStreamingIntegrationTest.class")
  }
}
```

This keeps the reusable capability dormant without making Kafka a normal runtime provider.

- [ ] **Step 4: Remove unused application-only Kafka property code**

Delete `KafkaEventStreamingProperties.java` and `KafkaEventStreamingPropertiesTest.java`. Kafka provider validation is no longer part of the active application configuration; future activation will reintroduce it with an external-consumer contract and environment-specific validation.

- [ ] **Step 5: Run focused tests and compile**

Run:

```bash
./gradlew :applications:emme-platform:test \
  --tests com.emme.PlatformApplicationParityTest \
  --tests com.emme.EventContractTest \
  --no-parallel --no-configuration-cache
./gradlew :applications:emme-platform:compileJava \
  --no-parallel --no-configuration-cache
```

Expected: PASS with zero failures and no application profile requiring Kafka.

- [ ] **Step 6: Commit the provider-configuration change**

```bash
git add applications/emme-platform/build.gradle.kts \
  applications/emme-platform/src/main/resources/application.yml \
  applications/emme-platform/src/main/resources/application-production.yml \
  applications/emme-platform/src/main/resources/application-test.yml \
  applications/emme-platform/src/test/java/com/emme/PlatformApplicationParityTest.java
git add -u applications/emme-platform/src/main/java/com/emme/configuration/KafkaEventStreamingProperties.java \
  applications/emme-platform/src/test/java/com/emme/KafkaEventStreamingPropertiesTest.java
git commit -m "refactor(config): defer Kafka provider activation"
```

## Task 3: Preserve Kafka streaming verification as an explicit deferred test

**Files:**

- Modify: `applications/emme-platform/src/integrationTest/java/com/emme/KafkaEventStreamingIntegrationTest.java`
- Preserve/verify: `applications/emme-platform/src/integrationTest/resources/application-kafka-test.yml`
- Modify: `applications/emme-platform/build.gradle.kts`

**Interfaces:**

- Consumes: the deferred `emme.kafka-deferred` Gradle property and the existing `kafka-test` profile.
- Produces: a Kafka integration test that exercises only a test-local externalized event, never a production business event.

- [ ] **Step 1: Write the failing deferred-test contract**

Change the integration test so it publishes a test-only record declared inside the `com.emme` test package:

```java
@Externalized("emme.test.kafka-event::#{#this.tenantId()}")
record TestExternalizedEvent(UUID tenantId, String marker) {}
```

Replace business-event publication and topic assertions with `emme.test.kafka-event` assertions. This keeps production events internal while preserving proof that the future externalizer can publish a stable topic, tenant key, and payload.

Run the explicitly selected test before adding the Gradle exclusion:

```bash
./gradlew integrationTest \
  --tests com.emme.KafkaEventStreamingIntegrationTest \
  -Pemme.kafka-deferred=true \
  --no-parallel --no-configuration-cache
```

Expected: FAIL until the test-only event and updated topic expectations are implemented. If Docker is unavailable, preserve the container startup error as an environment prerequisite and continue with the non-container checks.

- [ ] **Step 2: Implement the deferred-only test path**

Keep `application-kafka-test.yml` explicitly enabling Modulith externalization and JDBC schema initialization. Ensure the `integrationTest` task excludes `KafkaEventStreamingIntegrationTest` unless `-Pemme.kafka-deferred=true` is supplied. Do not annotate any production event to make this test pass.

- [ ] **Step 3: Verify default integration does not create Kafka**

Run:

```bash
./gradlew integrationTest --dry-run --no-parallel --no-configuration-cache
```

Expected: the normal integration task remains available without a Kafka broker; the Kafka test is excluded by the default task configuration. Then run the explicit deferred test in a Docker-enabled environment:

```bash
./gradlew integrationTest \
  --tests com.emme.KafkaEventStreamingIntegrationTest \
  -Pemme.kafka-deferred=true \
  --no-parallel --no-configuration-cache
```

Expected: PASS against the Kafka Testcontainer, with the test-only topic and tenant key verified.

- [ ] **Step 4: Commit the deferred test boundary**

```bash
git add applications/emme-platform/build.gradle.kts \
  applications/emme-platform/src/integrationTest/java/com/emme/KafkaEventStreamingIntegrationTest.java \
  applications/emme-platform/src/integrationTest/resources/application-kafka-test.yml
git commit -m "test(events): gate Kafka verification behind explicit opt-in"
```

## Task 4: Defer Kafka container creation and deployment activation

**Files:**

- Modify/comment: `deployment/compose/compose.environment-kafka.yaml`
- Modify: `infra/kubernetes/overlays/k3s-production-jvm/kustomization.yaml`
- Modify: `infra/kubernetes/overlays/k3s-production-native/kustomization.yaml`
- Modify: `.github/workflows/ci-backend.yml`
- Preserve: `deployment/compose/compose.kafka.contract.test.mjs`
- Preserve: `libraries/test-containers/src/main/java/com/emme/testing/integration/container/KafkaContainerConfiguration.java`
- Preserve: `libraries/test-containers/src/main/java/com/emme/testing/integration/annotation/KafkaIntegrationTest.java`

**Interfaces:**

- Consumes: the provider-disabled application profiles from Task 2.
- Produces: local, production, and normal CI paths with no Kafka container creation or broker-secret dependency.

- [ ] **Step 1: Add the failing deployment/configuration checks**

Before changing the activation points, run the existing checks and record the current Kafka activation evidence:

```bash
node deployment/compose/compose.kafka.contract.test.mjs
kubectl kustomize infra/kubernetes/overlays/k3s-production-jvm >/dev/null
kubectl kustomize infra/kubernetes/overlays/k3s-production-native >/dev/null
```

Expected: the Compose contract currently proves Kafka is enabled by its optional overlay, and the production overlays currently contain Kafka secret patches.

- [ ] **Step 2: Comment and gate optional Kafka container activation**

Keep `compose.environment-kafka.yaml` as a documented deferred overlay, but make its header state that it is not part of the Emme Nails default runtime. Keep its `depends_on` and broker healthcheck intact for explicit future validation.

In `.github/workflows/ci-backend.yml`, replace the removed `KafkaEventContractTest` selector with the new `EventContractTest` selector, comment out the removed `KafkaEventStreamingPropertiesTest` selector, and comment out the `node deployment/compose/compose.kafka.contract.test.mjs` invocation with a clear deferred-Kafka comment. Keep the internal event contract, AGE, and ordinary Compose/Kubernetes checks active.

Remove or comment the unused `KAFKA_SASL_JAAS_CONFIG` secret patch operations from both production Kustomize overlays. Do not remove the shared secret key if another deployment artifact still owns it; only remove the application pod injection that is no longer consumed.

- [ ] **Step 3: Verify default container/deployment paths**

Run:

```bash
docker compose -f deployment/compose/compose.yaml \
  -f deployment/compose/compose.runtime-jvm.yaml config --quiet
docker compose -f deployment/compose/compose.yaml \
  -f deployment/compose/compose.runtime-native.yaml config --quiet
kubectl kustomize infra/kubernetes/overlays/k3s-production-jvm >/dev/null
kubectl kustomize infra/kubernetes/overlays/k3s-production-native >/dev/null
```

Expected: default Compose and both Kustomize overlays render successfully without creating a Kafka service or injecting Kafka credentials. The optional Kafka Compose contract remains runnable only when explicitly invoked.

- [ ] **Step 4: Commit the deployment gating change**

```bash
git add deployment/compose/compose.environment-kafka.yaml \
  infra/kubernetes/overlays/k3s-production-jvm/kustomization.yaml \
  infra/kubernetes/overlays/k3s-production-native/kustomization.yaml \
  .github/workflows/ci-backend.yml
git commit -m "chore(delivery): defer Kafka containers and deployment wiring"
```

## Task 5: Align architecture and feature documentation

**Files:**

- Modify: `docs/architecture/01-backend/events.md`
- Modify: `docs/adr/0005-spring-modulith-kafka-event-streaming.md`
- Modify: `docs/adr/0006-mvp-low-cost-runtime-boundary.md`
- Modify: `docs/ai-platform/technical-specification.md`
- Modify: `docs/ai-platform/implementation-plan.md`
- Modify: `docs/superpowers/plans/2026-09-04-repository-framework-first-refactoring.md`

**Interfaces:**

- Consumes: the approved design at `docs/superpowers/specs/2026-09-05-modulith-first-event-boundaries-design.md`.
- Produces: one consistent written policy for internal Modulith events, deferred Kafka activation, and future external consumers.

- [ ] **Step 1: Write the documentation consistency checks**

Use repository searches as the failing documentation gate:

```bash
rg -n "Kafka externalization disabled|Kafka is used only|Modulith is the internal event boundary|LearningCandidateEvaluationRequested|first-stream contract|EMME_KAFKA_EVENTS_ENABLED:true" \
  docs/architecture docs/adr docs/ai-platform docs/superpowers/plans
```

Expected: the search identifies statements that currently describe Kafka as active for selected v1 events or production by default.

- [ ] **Step 2: Update the canonical event policy**

Document that all current events are internal unless an external consumer is approved. State that `@Externalized` is reserved for a real deployment boundary, while the JDBC Modulith publication registry remains the initial durable async mechanism.

Update ADR-0005 to record Kafka as a retained but deferred capability for the initial runtime. Update ADR-0006 so the MVP decision applies Kafka-disabled behavior to production as well as local/test profiles.

Update the AI technical specification and implementation plan so `LearningCandidateEvaluationRequested` is described as an internal Modulith event, not an active Kafka contract. Update the framework-first plan’s event task to point to this focused plan and its explicit Kafka gate.

- [ ] **Step 3: Verify documentation consistency**

Run:

```bash
rg -n "Kafka externalization disabled|Kafka is used only|Modulith is the internal event boundary|LearningCandidateEvaluationRequested|first-stream contract|EMME_KAFKA_EVENTS_ENABLED:true" \
  docs/architecture docs/adr docs/ai-platform docs/superpowers/plans
git diff --check
```

Expected: canonical documents consistently state Modulith-first behavior; any remaining Kafka references clearly describe the deferred capability or explicit reactivation path.

- [ ] **Step 4: Commit the documentation change**

```bash
git add docs/architecture/01-backend/events.md \
  docs/adr/0005-spring-modulith-kafka-event-streaming.md \
  docs/adr/0006-mvp-low-cost-runtime-boundary.md \
  docs/ai-platform/technical-specification.md \
  docs/ai-platform/implementation-plan.md \
  docs/superpowers/plans/2026-09-04-repository-framework-first-refactoring.md
git commit -m "docs(events): record Modulith-first MVP policy"
```

## Task 6: Run the complete Modulith-first verification gate

**Files:**

- Verify all files changed by Tasks 1–5.
- Modify: `docs/superpowers/plans/2026-09-05-modulith-first-event-boundaries.md` to mark completed tasks during execution and record final verification evidence.

**Interfaces:**

- Consumes: the finished event classification, profile, deferred-test, deployment, and documentation changes.
- Produces: a repository state ready for a later external-consumer Kafka activation decision.

- [ ] **Step 1: Run focused module and application tests**

```bash
./gradlew :modules:assistant:test \
  :modules:appointments:test \
  :modules:identity:test \
  :modules:tenancy:test \
  :modules:subscriptions:test \
  :applications:emme-platform:test \
  --no-parallel --no-configuration-cache
```

Expected: PASS with zero failures and zero skipped tests in the ordinary unit suites.

- [ ] **Step 2: Run the normal integration suite without Kafka**

```bash
./gradlew integrationTest \
  --no-parallel --no-configuration-cache
```

Expected: all non-Kafka integration tests use their existing containers and the Kafka streaming test is excluded unless `-Pemme.kafka-deferred=true` is supplied.

- [ ] **Step 3: Run build, formatting, and architecture checks**

```bash
./gradlew :applications:emme-platform:compileJava \
  :applications:emme-platform:check \
  spotlessCheck \
  --no-parallel --no-configuration-cache
```

Expected: compilation, formatting, architecture tests, and Checkstyle/quality tasks pass with no Kafka provider required.

- [ ] **Step 4: Run explicit deferred Kafka verification when Docker is available**

```bash
./gradlew integrationTest \
  --tests com.emme.KafkaEventStreamingIntegrationTest \
  -Pemme.kafka-deferred=true \
  --no-parallel --no-configuration-cache
```

Expected: the test-only externalized event reaches Kafka with its stable topic, tenant key, and payload. This is an opt-in future-capability check, not part of the initial runtime gate.

- [ ] **Step 5: Verify no active production Kafka provider remains**

```bash
rg -n "@Externalized|EMME_KAFKA_EVENTS_ENABLED:true|spring.kafka:|app.messaging.kafka:|KAFKA_BOOTSTRAP_SERVERS|KAFKA_SASL_JAAS_CONFIG" \
  applications/emme-platform/src/main modules infra/kubernetes deployment/compose .github/workflows
```

Expected: no production event record is annotated with `@Externalized`; no active application profile or deployment injects Kafka settings. Remaining matches must be limited to explicitly deferred test/build/container artifacts and documentation.

- [ ] **Step 6: Update plan status and commit verification evidence**

Mark each completed task with `[x]`, add the actual command results to the plan’s execution notes, then commit only the plan-status and verification changes:

```bash
git add docs/superpowers/plans/2026-09-05-modulith-first-event-boundaries.md
git commit -m "docs(events): record Modulith-first verification"
```

## Definition of Done

- [ ] All six current business events are internal Spring Modulith events.
- [ ] The initial application has no active Kafka provider configuration.
- [ ] PostgreSQL and the Modulith JDBC publication registry remain the durable internal async boundary.
- [ ] Kafka container creation is deferred from default Compose, Kubernetes, and CI execution.
- [ ] Deferred Kafka verification remains explicit and runnable with `-Pemme.kafka-deferred=true`.
- [ ] No domain or application orchestration package imports Kafka types.
- [ ] Canonical architecture, ADR, AI, and execution-plan documents agree on the policy.
- [ ] Unit, integration, compile, formatting, and architecture checks pass with zero ordinary-test failures or skips.
- [ ] Changes are committed in logical units and pushed to `feat/ai-platform-foundation`.
