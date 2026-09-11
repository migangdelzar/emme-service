# Whole-Repository Lombok Simplification Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Reduce repetitive Java boilerplate across all owned source sets with behavior-preserving Lombok adoption while keeping domain, persistence, tenant, configuration, provider, and workflow invariants explicit.

**Architecture:** Keep Lombok opt-in and compile-time only. Extend the repository source policy, then migrate independent constructor, logging, immutable-value, resource, and test-fixture slices only when generated behavior matches the existing contract. Shared policy, public contracts, tenant boundaries, migrations, composition roots, and overlapping files are handled sequentially.

**Tech Stack:** Java 25, Gradle Kotlin DSL, Spring Boot 4.1.0, Lombok 1.18.48, JUnit 5, AssertJ, Spotless, Checkstyle.

## Global Constraints

- Lombok remains compile-time only through `compileOnly`, `annotationProcessor`, `testCompileOnly`, and `testAnnotationProcessor`.
- Audit all owned Java files under `modules/**/src/**`, `libraries/**/src/**`, `applications/**/src/**`, and relevant `build-logic/**/src/**`; exclude generated `build/` output from edits and policy inventory decisions.
- Do not introduce `@Data`, `@Delegate`, `@SneakyThrows`, `@Synchronized`, `@ExtensionMethod`, or generated exception annotations.
- Do not use broad generated `toString()` or setters as a default simplification strategy.
- Keep domain aggregates, JPA entities, composite persistence identities, tenant context, security state, workflow state, and provider clients explicit unless a separate design proves generated behavior safe.
- Keep Java records for immutable API, configuration, and contract carriers when they already express the type clearly.
- Every approved builder must declare `setterPrefix = "with"`.
- Do not change public constructor, property, JSON, command-line, environment, tenant, provider, or serialized names.
- Preserve Spring bean wiring, JPA identity/lifecycle behavior, transaction boundaries, authorization, and tenant-schema routing.
- Follow Red → Green → Refactor for every behavior-affecting task.
- Commit and push each logical slice with a conventional commit before starting the next slice.
- Use focused checks per slice; run full repository checks only at phase checkpoints.

---

## 1. File map

### Shared policy and documentation

- Modify: `modules/shared/src/test/java/com/emme/shared/architecture/LombokUsagePolicyTest.java` — repository-wide annotation allowlist, forbidden usages, builder prefix checks, and generated-source exclusion.
- Modify: `docs/superpowers/plans/2026-09-05-selective-lombok-adoption.md` — record completion of the expanded adoption and link the whole-repository plan.
- Modify: `tasks/todo.md` — track each completed Lombok slice and final audit results.
- Modify: `tasks/lessons.md` — record any discovered Lombok or source-inventory failure mode.

### Build opt-in files

- Existing convention: `build-logic/src/main/kotlin/emme.lombok.gradle.kts`.
- Existing dependency wiring: `gradle/libs.versions.toml` and `build-logic/src/main/kotlin/com/emme/buildlogic/core/dependency/Dependencies.kt`.
- Opt-in changes are limited to modules containing an accepted Lombok candidate; do not apply the plugin globally.

### Constructor candidates

The following files came from the one-constructor/direct-assignment inventory. Each must pass the constructor-specific policy before conversion; files with qualifiers, custom validation, `super(...)`, overloads, optional-dependency selection, or lifecycle behavior remain explicit.

- `modules/appointments/src/main/java/com/emme/appointments/adapter/in/web/controller/DashboardController.java`
- `modules/appointments/src/main/java/com/emme/appointments/adapter/out/messaging/publisher/SpringAppointmentEventPublisher.java`
- `modules/appointments/src/main/java/com/emme/appointments/adapter/out/persistence/adapter/AppointmentCollisionAdapter.java`
- `modules/appointments/src/main/java/com/emme/appointments/adapter/out/persistence/adapter/AppointmentPersistenceAdapter.java`
- `modules/assistant/src/main/java/com/emme/assistant/adapter/in/webhook/WhatsAppWebhookController.java`
- `modules/assistant/src/main/java/com/emme/assistant/adapter/in/webhook/WhatsAppWebhookMapper.java`
- `modules/assistant/src/main/java/com/emme/assistant/adapter/out/persistence/adapter/WhatsAppWebhookEventPersistenceAdapter.java`
- `modules/assistant/src/main/java/com/emme/assistant/adapter/out/persistence/mapper/ConversationEventPersistenceMapper.java`
- `modules/assistant/src/main/java/com/emme/assistant/adapter/out/tenant/ConfiguredWhatsAppTenantResolver.java`
- `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/in/messaging/SemanticCacheInvalidationListener.java`
- `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/provider/springai/advisor/PromptVersionAdvisor.java`
- `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/storage/CatalogDesignImageReader.java`
- `modules/calendar/src/main/java/com/emme/calendar/adapter/in/web/controller/CalendarController.java`
- `modules/calendar/src/main/java/com/emme/calendar/adapter/in/web/controller/ClientCalendarController.java`
- `modules/calendar/src/main/java/com/emme/calendar/adapter/in/web/controller/GoogleOAuthController.java`
- `modules/calendar/src/main/java/com/emme/calendar/adapter/in/web/controller/SheetsController.java`
- `modules/calendar/src/main/java/com/emme/calendar/adapter/out/google/adapter/GoogleSheetsAdapter.java`
- `modules/calendar/src/main/java/com/emme/calendar/adapter/out/google/oauth/OAuthTokenSource.java`
- `modules/calendar/src/main/java/com/emme/calendar/adapter/out/persistence/adapter/CalendarPersistenceAdapter.java`
- `modules/calendar/src/main/java/com/emme/calendar/adapter/out/persistence/adapter/GoogleSpreadsheetLinkQueryAdapter.java`
- `modules/catalog/src/main/java/com/emme/catalog/adapter/in/web/controller/CatalogController.java`
- `modules/clients/src/main/java/com/emme/clients/adapter/in/web/controller/CustomerController.java`
- `modules/clients/src/main/java/com/emme/clients/adapter/out/persistence/adapter/CustomerPersistenceAdapter.java`
- `modules/documents/src/main/java/com/emme/documents/adapter/in/web/controller/DocumentController.java`
- `modules/documents/src/main/java/com/emme/documents/adapter/out/persistence/adapter/DocumentPersistenceAdapter.java`
- `modules/documents/src/main/java/com/emme/documents/adapter/out/search/HybridDocumentSearchAdapter.java`
- `modules/identity/src/main/java/com/emme/identity/adapter/in/messaging/consumer/AppointmentCreatedConsumer.java`
- `modules/identity/src/main/java/com/emme/identity/adapter/in/web/controller/AuthController.java`
- `modules/identity/src/main/java/com/emme/identity/adapter/in/web/controller/CurrentUserController.java`
- `modules/identity/src/main/java/com/emme/identity/adapter/in/web/controller/FeatureFlagController.java`
- `modules/identity/src/main/java/com/emme/identity/adapter/in/web/controller/IdentityController.java`
- `modules/identity/src/main/java/com/emme/identity/adapter/in/web/controller/TenantFeatureFlagController.java`
- `modules/identity/src/main/java/com/emme/identity/adapter/in/web/security/IdentityJwtAuthoritiesConverter.java`
- `modules/identity/src/main/java/com/emme/identity/adapter/in/web/security/IdentityUserAuthoritiesMapper.java`
- `modules/identity/src/main/java/com/emme/identity/adapter/out/client/keycloak/CustomerTokenDecoderAdapter.java`
- `modules/identity/src/main/java/com/emme/identity/adapter/out/client/subscription/SubscriptionPlanAdapter.java`
- `modules/identity/src/main/java/com/emme/identity/adapter/out/module/tenancy/TenantIdentityRealmAdapter.java`
- `modules/identity/src/main/java/com/emme/identity/adapter/out/persistence/adapter/CustomerMembershipPersistenceAdapter.java`
- `modules/identity/src/main/java/com/emme/identity/adapter/out/ratelimit/RedisLoginAttemptRateLimiter.java`
- `modules/notification/src/main/java/com/emme/notification/adapter/out/event/SpringNotificationEventPublisher.java`
- `modules/payment/src/main/java/com/emme/payment/adapter/out/messaging/publisher/SpringPaymentWorkflowEventPublisher.java`
- `modules/payment/src/main/java/com/emme/payment/adapter/out/persistence/adapter/PaymentWebhookEventPersistenceAdapter.java`
- `modules/salon/src/main/java/com/emme/salon/adapter/out/persistence/adapter/BookingPolicyPersistenceAdapter.java`
- `modules/salon/src/main/java/com/emme/salon/adapter/out/persistence/adapter/BusinessProfilePersistenceAdapter.java`
- `modules/salon/src/main/java/com/emme/salon/adapter/out/persistence/adapter/OperatingHoursPersistenceAdapter.java`
- `modules/services/src/main/java/com/emme/services/adapter/out/persistence/adapter/ArtistPersistenceAdapter.java`
- `modules/services/src/main/java/com/emme/services/adapter/out/persistence/adapter/ServicePersistenceAdapter.java`
- `modules/shared/src/main/java/com/emme/shared/persistence/jdbc/BootstrapConnectionExecutor.java`
- `modules/shared/src/main/java/com/emme/shared/search/postgres/PostgresHybridSearch.java`
- `modules/shared/src/main/java/com/emme/shared/web/advice/GlobalExceptionHandler.java`
- `modules/subscriptions/src/main/java/com/emme/subscriptions/adapter/out/persistence/adapter/SubscriptionPersistenceAdapter.java`
- `modules/tenancy/src/main/java/com/emme/tenancy/adapter/out/client/database/LiquibaseTenantSchemaMigrationAdapter.java`
- `modules/tenancy/src/main/java/com/emme/tenancy/adapter/out/messaging/publisher/SpringTenantEventPublisher.java`
- `modules/tenancy/src/main/java/com/emme/tenancy/adapter/out/persistence/adapter/AuditEventPersistenceAdapter.java`
- `modules/tenancy/src/main/java/com/emme/tenancy/adapter/out/persistence/adapter/TenantProvisioningPersistenceAdapter.java`
- `modules/tenancy/src/main/java/com/emme/tenancy/application/audit/AuditEventRecorder.java`

The inventory also identified custom-construction classes that are not
automatic targets: `CancelAppointmentService`, `CompleteAppointmentService`,
`ConfirmAppointmentService`, `CreateAppointmentService`,
`GetAppointmentService`, `ListAppointmentsByDateService`,
`MarkAppointmentNoShowService`, `RescheduleAppointmentService`,
`StartAppointmentService`, `CreateCatalogItemService`,
`DeleteCatalogItemService`, `SetTenantFeatureFlagOverrideService`,
`RagQueryService`, `ChatService`, `ProcessWhatsAppMessageService`, and
configuration/property records. Their custom behavior remains explicit.

### Logging candidates

The ordinary logger inventory includes these classes:

- `modules/appointments/src/main/java/com/emme/appointments/adapter/in/web/controller/DashboardController.java`
- `modules/appointments/src/main/java/com/emme/appointments/adapter/in/web/sse/DashboardBroadcaster.java`
- `modules/assistant/src/main/java/com/emme/assistant/adapter/in/webhook/WhatsAppWebhookController.java`
- `modules/assistant/src/main/java/com/emme/assistant/adapter/in/webhook/WhatsAppWebhookMapper.java`
- `modules/assistant/src/main/java/com/emme/assistant/adapter/out/client/whatsapp/WhatsAppReplyAdapter.java`
- `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/provider/springai/SpringAiNailDesignExtractor.java`
- `modules/assistant/src/main/java/com/emme/assistant/ai/application/provider/TracingAiChatCompletion.java`
- `modules/assistant/src/main/java/com/emme/assistant/ai/application/provider/TracingEmbeddingService.java`
- `modules/assistant/src/main/java/com/emme/assistant/ai/application/semantic/SemanticCacheInvalidationService.java`
- `modules/assistant/src/main/java/com/emme/assistant/ai/application/semantic/SemanticCacheResolver.java`
- `modules/assistant/src/main/java/com/emme/assistant/ai/application/semantic/SemanticChatCache.java`
- `modules/assistant/src/main/java/com/emme/assistant/ai/application/semantic/SemanticIntentClassifier.java`
- `modules/assistant/src/main/java/com/emme/assistant/ai/application/semantic/SemanticToolSelector.java`
- `modules/assistant/src/main/java/com/emme/assistant/ai/application/tool/AuthorizedAiToolGateway.java`
- `modules/calendar/src/main/java/com/emme/calendar/adapter/in/web/controller/GoogleOAuthController.java`
- `modules/calendar/src/main/java/com/emme/calendar/adapter/out/google/adapter/ClientCalendarSyncAdapter.java`
- `modules/calendar/src/main/java/com/emme/calendar/adapter/out/google/adapter/GoogleOAuthAdapter.java`
- `modules/calendar/src/main/java/com/emme/calendar/adapter/out/google/adapter/GoogleSheetsAdapter.java`
- `modules/calendar/src/main/java/com/emme/calendar/adapter/out/google/adapter/StaffCalendarSyncAdapter.java`
- `modules/calendar/src/main/java/com/emme/calendar/adapter/out/google/client/GoogleCalendarClient.java`
- `modules/calendar/src/main/java/com/emme/calendar/adapter/out/google/client/GoogleSheetsClient.java`
- `modules/calendar/src/main/java/com/emme/calendar/adapter/out/google/oauth/OAuthTokenSource.java`
- `modules/calendar/src/main/java/com/emme/calendar/application/service/GetBusyTimesService.java`
- `modules/calendar/src/main/java/com/emme/calendar/application/service/SyncCalendarEventsService.java`
- `modules/documents/src/main/java/com/emme/documents/application/service/FailDocumentService.java`
- `modules/identity/src/main/java/com/emme/identity/adapter/in/messaging/consumer/TenantRealmProvisioningListener.java`
- `modules/identity/src/main/java/com/emme/identity/adapter/in/web/controller/AuthController.java`
- `modules/identity/src/main/java/com/emme/identity/adapter/out/observability/SecurityAuditLogger.java`
- `modules/identity/src/main/java/com/emme/identity/application/service/AuthenticateCustomerService.java`
- `modules/identity/src/main/java/com/emme/identity/application/service/EnsureCustomerMembershipService.java`
- `modules/notification/src/main/java/com/emme/notification/adapter/out/provider/email/MockEmailProvider.java`
- `modules/notification/src/main/java/com/emme/notification/adapter/out/provider/email/SendGridProvider.java`
- `modules/notification/src/main/java/com/emme/notification/adapter/out/provider/email/SesEmailProvider.java`
- `modules/notification/src/main/java/com/emme/notification/adapter/out/provider/email/SmtpEmailProvider.java`
- `modules/notification/src/main/java/com/emme/notification/adapter/out/provider/push/ApnsPushProvider.java`
- `modules/notification/src/main/java/com/emme/notification/adapter/out/provider/push/FcmPushProvider.java`
- `modules/notification/src/main/java/com/emme/notification/adapter/out/provider/push/MockPushProvider.java`
- `modules/notification/src/main/java/com/emme/notification/adapter/out/provider/sms/MessageBirdProvider.java`
- `modules/notification/src/main/java/com/emme/notification/adapter/out/provider/sms/MockSmsProvider.java`
- `modules/notification/src/main/java/com/emme/notification/adapter/out/provider/sms/TwilioSmsProvider.java`
- `modules/notification/src/main/java/com/emme/notification/adapter/out/provider/sms/VonageSmsProvider.java`
- `modules/notification/src/main/java/com/emme/notification/application/service/DeliverNotificationService.java`
- `modules/payment/src/main/java/com/emme/payment/adapter/in/webhook/MercadoPagoWebhookController.java`
- `modules/tenancy/src/main/java/com/emme/tenancy/adapter/in/messaging/consumer/TenantActivationListener.java`
- `modules/tenancy/src/main/java/com/emme/tenancy/adapter/in/messaging/consumer/TenantSchemaProvisioningListener.java`
- `modules/tenancy/src/main/java/com/emme/tenancy/adapter/in/web/filter/TenantContextFilter.java`
- `modules/tenancy/src/main/java/com/emme/tenancy/adapter/in/web/filter/TenantRateLimitInterceptor.java`
- `modules/tenancy/src/main/java/com/emme/tenancy/adapter/out/client/database/SchemaMultiTenantConnectionProvider.java`
- `modules/tenancy/src/main/java/com/emme/tenancy/adapter/out/client/database/TenantDatabasePoolProvider.java`
- `modules/tenancy/src/main/java/com/emme/tenancy/adapter/out/client/database/TenantIdentifierResolver.java`

`SesEmailProvider.java` and the nine AI semantic/provider classes using the
uppercase `LOGGER` field require deliberate `LOGGER` to `log` reference
renaming if converted to `@Slf4j`. Security audit logging and classes with
custom logger factories remain explicit.

## 2. Task dependency graph

```text
Task 1 policy/inventory
  ├── Task 2 constructor slices
  ├── Task 3 logging slices
  ├── Task 4 values/builders/immutable updates
  ├── Task 5 resources/field reductions
  └── Task 6 test and E2E fixtures
Task 2 + Task 3 + Task 4 + Task 5 + Task 6
  └── Task 7 boundary audit and closure
```

Task 1 is sequential because it changes the shared architecture guard. Tasks
2 through 6 are independent by file set and may be dispatched to separate
agents only after Task 1 is committed; overlapping files such as controllers
with logger fields must remain in one sequential slice. Task 7 is sequential
because it updates the shared completion ledger and final policy.

## 3. Tasks

### Task 1: Extend the repository Lombok policy and source inventory

**Files:**

- Modify: `modules/shared/src/test/java/com/emme/shared/architecture/LombokUsagePolicyTest.java`
- Test: `modules/shared/src/test/java/com/emme/shared/architecture/LombokUsagePolicyTest.java`

**Interfaces:**

- Produces a source inventory over `modules`, `libraries`, `applications`, and
  `build-logic` owned `src` trees.
- Excludes any path containing `/build/` and the policy test itself from
  annotation-usage assertions while preserving forbidden-name detection.
- Keeps the approved production allowlist and adds a separate approved map for
  test/E2E Lombok files.

- [x] **Step 1: Write the failing policy test.** Add assertions that the inventory includes `applications` and all source sets, excludes generated `build/` paths, rejects the six excluded annotation families, and requires `setterPrefix = "with"` for every approved builder.

```java
@Test
void scansAllOwnedSourceSetsWithoutGeneratedBuildOutput() throws IOException {
  Path root = sourcePath();

  assertThat(ownedJavaSources(root))
      .allMatch(path -> path.toString().replace('\\', '/').contains("/src/"))
      .noneMatch(path -> path.toString().replace('\\', '/').contains("/build/"));
}
```

- [x] **Step 2: Run the focused test to verify it fails.**

Run:

```bash
./gradlew :modules:shared:test --tests com.emme.shared.architecture.LombokUsagePolicyTest --no-parallel --no-configuration-cache
```

Expected: FAIL because the current inventory only walks `modules` and
`libraries` production Java sources and does not represent all source sets.

- [x] **Step 3: Implement the smallest policy change.** Add a shared source-root list, filter to owned `src` Java files, exclude `/build/`, and keep the policy detector source out of annotation-usage matching. Use explicit regex checks for annotation usages rather than broad substring checks so policy string literals do not count as usages.

- [x] **Step 4: Run the focused policy test to verify it passes.**

Run the same command. Expected: PASS with no forbidden production/test
annotation usages and no generated build paths in the inventory.

- [x] **Step 5: Refactor and verify formatting.** Run:

```bash
./gradlew :modules:shared:spotlessApply :modules:shared:spotlessCheck --no-parallel --no-configuration-cache
git diff --check
```

- [x] **Step 6: Commit and push.**

```bash
git add modules/shared/src/test/java/com/emme/shared/architecture/LombokUsagePolicyTest.java
git commit -m "test(lombok): cover whole repository source policy"
git push origin feat/ai-platform-foundation
```

### Task 2: Apply safe constructor reduction slices

**Files:**

- Modify: candidate files listed in Section 1 under “Constructor candidates”.
- Modify: each candidate module's `build.gradle.kts` only when that module is
  not already opted into `emme.lombok`.
- Test: each candidate module's existing unit and integration test sources.
- Test: `modules/shared/src/test/java/com/emme/shared/architecture/LombokUsagePolicyTest.java`.

**Interfaces:**

- Consumes Task 1's approved annotation policy.
- Produces constructor-only Lombok usage with public generated constructors,
  unchanged parameter order, and unchanged Spring bean behavior.

- [x] **Step 1: Write one failing allowlist test per module slice.** Before changing a module, add its accepted paths to the policy's expected approved set without adding Lombok to the source. Run the policy test and verify it fails with the missing approved source path.

- [x] **Step 2: Run the focused red test.**

```bash
./gradlew :modules:shared:test --tests com.emme.shared.architecture.LombokUsagePolicyTest --no-parallel --no-configuration-cache
```

Expected: FAIL because the approved source path does not yet contain
`import lombok.RequiredArgsConstructor;` and `@RequiredArgsConstructor`.

- [x] **Step 3: Convert only a plain constructor.** For an accepted class such as `DashboardController`, replace the pure constructor with:

```java
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DashboardController {
  private final DashboardService dashboardService;
}
```

Do not apply this form when the constructor has a qualifier, validation,
defaulting, overload delegation, `super(...)`, optional-provider selection, or
custom side effects. For a class with a constructor qualifier, keep the
explicit constructor unless a field-level qualifier and focused wiring test
prove equivalent behavior.

- [x] **Step 4: Run the affected module tests and compilation.** For each module slice run the module test task, `compileJava`, and `compileTestJava`; include `compileIntegrationTestJava` when the module defines that source set. The minimum commands are:

```bash
for lombok_module in appointments assistant calendar catalog clients documents identity notification payment salon services shared subscriptions tenancy; do
  ./gradlew ":modules:${lombok_module}:test" ":modules:${lombok_module}:compileJava" ":modules:${lombok_module}:compileTestJava" --no-parallel --no-configuration-cache || exit 1
done
./gradlew :modules:shared:test --tests com.emme.shared.architecture.LombokUsagePolicyTest --no-parallel --no-configuration-cache
```

Expected: PASS with no changed public behavior.

- [x] **Step 5: Refactor and inspect generated-constructor compatibility.** Confirm constructor parameter order remains the field declaration order, `-parameters` remains enabled, and no tenant/provider qualifier or Spring bean ambiguity changed. Run the module's Spotless and Checkstyle tasks.

- [x] **Step 6: Commit and push each module slice.** Use the module name as the conventional commit scope:

```bash
lombok_module=appointments
git add -A -- "modules/${lombok_module}" modules/shared/src/test/java/com/emme/shared/architecture/LombokUsagePolicyTest.java
git commit -m "refactor(lombok): simplify ${lombok_module} constructors"
git push origin feat/ai-platform-foundation
```

Repeat the same explicit slice command with `lombok_module` set, one commit at
a time, to `assistant`, `calendar`, `catalog`, `clients`, `documents`,
`identity`, `notification`, `payment`, `salon`, `services`, `shared`,
`subscriptions`, and `tenancy`. The staging path is restricted to that module
and the shared policy test.

The candidate modules are processed in this sequential order when files
overlap with logging: `appointments`, `assistant`, `calendar`, `catalog`,
`clients`, `documents`, `identity`, `notification`, `payment`, `salon`,
`services`, `shared`, `subscriptions`, and `tenancy`. Do not convert the
custom-construction classes listed in Section 1 without a new approved design.

Task 2 completion: 48 safe constructor candidates were converted in the
specified order. Eight candidates were rejected because their constructors
instantiate mappers or perform validation; `salon` and `services` therefore
received no source conversion. Full adoption, rejection, TDD, verification,
and commit evidence is recorded in
[task-2-report.md](../../../.superpowers/sdd/task-2-report.md).

### Task 3: Replace ordinary logger boilerplate with `@Slf4j`

**Files:**

- Modify: the ordinary logger candidates listed in Section 1 under “Logging candidates”.
- Modify: module `build.gradle.kts` files only when the module needs to opt into
  `emme.lombok`.
- Test: affected module tests and any existing security/audit logging tests.
- Test: `modules/shared/src/test/java/com/emme/shared/architecture/LombokUsagePolicyTest.java`.

**Interfaces:**

- Produces the same SLF4J logger category and call behavior with less field and
  import boilerplate.
- Does not change security-audit logger semantics or introduce sensitive log
  fields.

- [x] **Step 1: Write the failing source-policy assertion.** Add each chosen logger file to the approved Lombok map and add an assertion that approved logger migrations no longer import `LoggerFactory` or declare the removed logger field.

```java
assertThat(contents)
    .doesNotContain("LoggerFactory.getLogger")
    .contains("import lombok.extern.slf4j.Slf4j;")
    .contains("@Slf4j");
```

- [x] **Step 2: Run the logger policy test and verify red.**

```bash
./gradlew :modules:shared:test --tests com.emme.shared.architecture.LombokUsagePolicyTest --no-parallel --no-configuration-cache
```

Expected: FAIL because the approved class still declares its manual logger.

- [x] **Step 3: Apply the minimal logger change.** Add `import lombok.extern.slf4j.Slf4j;`, add `@Slf4j`, remove the `Logger`/`LoggerFactory` field and imports, and keep all logging calls unchanged when the field is already named `log`. For uppercase `LOGGER` fields, rename only logger references to `log` in the same file and preserve log messages, levels, arguments, and exception objects.

- [x] **Step 4: Run focused behavior and source checks.** Run the affected module test, compile tasks, policy test, Spotless, and Checkstyle. For `SecurityAuditLogger.java`, keep the explicit logger unless a dedicated test proves category and redaction behavior unchanged.

- [x] **Step 5: Refactor and inspect sensitive logging.** Confirm no token, credential, tenant secret, or raw AI/provider payload was added to log calls. Confirm `@Slf4j` uses the existing class category.

- [x] **Step 6: Commit and push each independent logging slice.**

```bash
lombok_logging_module=appointments
git add -A -- "modules/${lombok_logging_module}" modules/shared/src/test/java/com/emme/shared/architecture/LombokUsagePolicyTest.java
git commit -m "refactor(lombok): simplify ${lombok_logging_module} logging"
git push origin feat/ai-platform-foundation
```

Repeat only for modules with an accepted logger candidate, one commit at a
time, using these explicit values: `appointments`, `assistant`, `calendar`,
`documents`, `identity`, `notification`, `payment`, and `tenancy`.

### Task 4: Review immutable values, equality, builders, and immutable updates

**Files:**

- Review: `libraries/ai-contracts/src/main/java/com/emme/ai/contracts/graph/GraphNodeType.java`
- Review: `libraries/ai-contracts/src/main/java/com/emme/ai/contracts/graph/GraphRelationshipType.java`
- Review: `libraries/ai-contracts/src/main/java/com/emme/ai/contracts/graph/GraphTraversalKind.java`
- Review: all non-record response/value classes found by the Task 1 source inventory.
- Review: complex test fixtures in `modules/assistant/src/test/java` and
  `applications/emme-platform/src/e2eTest/java`.
- Test: focused value, equality, serialization, builder, and fixture tests for
  any accepted candidate.
- Test: `modules/shared/src/test/java/com/emme/shared/architecture/LombokUsagePolicyTest.java`.

**Interfaces:**

- Produces no new builder unless existing construction is materially clearer
  with one.
- Any accepted builder uses `@Builder(setterPrefix = "with")`.
- Any accepted `@Value`, `@With`, or `@EqualsAndHashCode` preserves the type's
  public construction, equality, immutability, and serialization contract.

- [ ] **Step 1: Write a focused failing contract test for the selected candidate.** For `@Value` or `@With`, test all fields, immutability, copy behavior, and equality. For a builder, test required fields, omitted defaults, collection handling, and `withX` method names. For `@Jacksonized`, test JSON round-trip names. If no candidate has a real builder need, add the audit decision to the plan and make no production builder change.

- [ ] **Step 2: Run the focused test to verify red.** Run only the selected test class and confirm it fails for the missing generated API or current contract mismatch.

- [ ] **Step 3: Apply the narrow annotation set.** Use `@Value` only on an immutable non-record value object, `@With` only on final fields with an established copy-update API, and `@EqualsAndHashCode` only with explicit included identity fields. Use `@Builder(setterPrefix = "with")` only on the selected carrier and add `@Singular`, `@Builder.Default`, `@ToBuilder`, or `@Jacksonized` only when the focused test requires that behavior.

- [ ] **Step 4: Run focused tests, affected compilation, and serialization checks.** Expected: PASS with unchanged public property and JSON names.

- [ ] **Step 5: Refactor for the smallest readable annotation set.** Remove annotations that duplicate record behavior or make invariants less obvious. Keep custom equality and persistence identity explicit.

- [ ] **Step 6: Commit and push each accepted value/builder slice.**

```bash
git add -A -- \
  libraries/ai-contracts/src/main/java/com/emme/ai/contracts/graph/GraphNodeType.java \
  libraries/ai-contracts/src/main/java/com/emme/ai/contracts/graph/GraphRelationshipType.java \
  libraries/ai-contracts/src/main/java/com/emme/ai/contracts/graph/GraphTraversalKind.java \
  modules/assistant/src/test/java/com/emme/assistant/ai/adapter/out/provider/springai/SpringAiNailDesignExtractorTest.java \
  modules/assistant/src/test/java/com/emme/assistant/ai/application/service/ProcessDesignQuoteServiceTest.java \
  modules/assistant/src/test/java/com/emme/assistant/ai/application/service/ReviewQuoteServiceTest.java \
  modules/assistant/src/test/java/com/emme/assistant/ai/application/service/SemanticRoutingServiceTest.java \
  applications/emme-platform/src/e2eTest/java/com/emme/client/UserSessionHelper.java \
  applications/emme-platform/src/e2eTest/java/com/emme/client/UserSession.java \
  applications/emme-platform/src/e2eTest/java/com/emme/client/SetupHelper.java \
  modules/shared/src/test/java/com/emme/shared/architecture/LombokUsagePolicyTest.java
git commit -m "refactor(lombok): simplify immutable value construction"
git push origin feat/ai-platform-foundation
```

### Task 5: Review resource and field reductions

**Files:**

- Review: `libraries/kernel/src/main/java/com/emme/kernel/context/TenantContextHolder.java`
- Review: `libraries/kernel/src/main/java/com/emme/kernel/tracing/CorrelationContextHolder.java`
- Review: all source files from the Task 1 inventory containing manual resource
  cleanup, duplicated field-name constants, or mechanical field modifiers.
- Test: focused resource, exception, context-restoration, and field-constant
  tests for any accepted candidate.

**Interfaces:**

- `@Cleanup` may only replace ordinary resource close code with identical
  success/failure behavior.
- `@FieldNameConstants` may only replace already duplicated stable internal
  field-name constants.
- `@FieldDefaults` and `@NonNull` may only be used when visibility, null
  timing, exception type/message, and framework binding remain identical.

- [ ] **Step 1: Write the focused failing test.** For a resource candidate, test close on normal completion, close on failure, and exception propagation. For `@NonNull`, test null timing and message. For field-name constants, test every consumer's value.

- [ ] **Step 2: Run the focused test and verify red.** Use the affected module's test task with the single test class.

- [ ] **Step 3: Apply the smallest annotation change.** Do not use `@Cleanup` in tenant context restoration, transaction cleanup, connection checkout/release, or exception translation code. Do not add field defaults that hide security or lifecycle state.

- [ ] **Step 4: Run focused tests, compilation, Spotless, and Checkstyle.** Expected: PASS with identical cleanup and exception semantics.

- [ ] **Step 5: Commit and push only accepted reductions.** If no candidate preserves behavior and clarity, record the rejection in the plan and make no code change.

### Task 6: Simplify test and E2E fixtures

**Files:**

- Review: `applications/emme-platform/src/e2eTest/java/com/emme/client/UserSessionHelper.java`
- Review: `applications/emme-platform/src/e2eTest/java/com/emme/client/UserSession.java`
- Review: `applications/emme-platform/src/e2eTest/java/com/emme/client/SetupHelper.java`
- Review: `applications/emme-platform/src/e2eTest/java/com/emme/client/crud/*.java`
- Review: `modules/assistant/src/test/java/com/emme/assistant/ai/adapter/out/provider/springai/SpringAiNailDesignExtractorTest.java`
- Review: `modules/assistant/src/test/java/com/emme/assistant/ai/application/service/ProcessDesignQuoteServiceTest.java`
- Review: `modules/assistant/src/test/java/com/emme/assistant/ai/application/service/ReviewQuoteServiceTest.java`
- Review: `modules/assistant/src/test/java/com/emme/assistant/ai/application/service/SemanticRoutingServiceTest.java`
- Modify: `applications/emme-platform/build.gradle.kts` only if an accepted E2E source candidate needs Lombok.
- Test: affected E2E/test-fixture source compilation and focused fixture tests.

**Interfaces:**

- Produces shorter fixtures without hiding assertions, roles, tenant setup,
  cleanup, HTTP behavior, or test lifecycle.
- Does not add builders or mutable accessors merely to reduce fixture lines.

- [ ] **Step 1: Write the failing fixture source-policy test.** Add an approved test/E2E path only when its intended generated constructor/accessor/builder is specified by a focused test.

- [ ] **Step 2: Run the focused source or fixture test and verify red.**

```bash
./gradlew :applications:emme-platform:compileE2eTestJava :modules:assistant:test \
  --tests '*SpringAiNailDesignExtractorTest' \
  --tests '*ProcessDesignQuoteServiceTest' \
  --tests '*ReviewQuoteServiceTest' \
  --tests '*SemanticRoutingServiceTest' \
  --no-parallel --no-configuration-cache
```

- [ ] **Step 3: Apply the smallest safe annotation.** `UserSessionHelper` may use `@RequiredArgsConstructor` only for its final `UserSession` dependency; mutable default tenant/artist state remains explicit. Do not apply `@Data` or generated equality to test helpers that contain session state.

- [ ] **Step 4: Run the affected source compilation and tests.** Expected: PASS with unchanged test users, roles, tenant setup, and HTTP requests.

- [ ] **Step 5: Commit and push each fixture slice.**

```bash
git add -A -- \
  applications/emme-platform/build.gradle.kts \
  applications/emme-platform/src/e2eTest/java/com/emme/client/UserSessionHelper.java \
  applications/emme-platform/src/e2eTest/java/com/emme/client/UserSession.java \
  applications/emme-platform/src/e2eTest/java/com/emme/client/SetupHelper.java \
  applications/emme-platform/src/e2eTest/java/com/emme/client/crud \
  modules/assistant/src/test/java/com/emme/assistant/ai/adapter/out/provider/springai/SpringAiNailDesignExtractorTest.java \
  modules/assistant/src/test/java/com/emme/assistant/ai/application/service/ProcessDesignQuoteServiceTest.java \
  modules/assistant/src/test/java/com/emme/assistant/ai/application/service/ReviewQuoteServiceTest.java \
  modules/assistant/src/test/java/com/emme/assistant/ai/application/service/SemanticRoutingServiceTest.java \
  modules/shared/src/test/java/com/emme/shared/architecture/LombokUsagePolicyTest.java
git commit -m "test(lombok): simplify fixture construction"
git push origin feat/ai-platform-foundation
```

### Task 7: Boundary audit, documentation, and phase closure

**Files:**

- Modify: `modules/shared/src/test/java/com/emme/shared/architecture/LombokUsagePolicyTest.java`
- Modify: `docs/superpowers/plans/2026-09-05-selective-lombok-adoption.md`
- Modify: `docs/superpowers/plans/2026-09-10-lombok-whole-repository-refactor.md`
- Modify: `tasks/todo.md`
- Modify: `tasks/lessons.md` only when a new failure mode was discovered.

**Interfaces:**

- Produces the final adopted/rejected/deferred inventory.
- Leaves the repository policy enforceable for future changes.

- [ ] **Step 1: Write the failing closure assertions.** Add policy assertions for the final approved set, forbidden annotations, builder prefixes, generated-source exclusion, protected domain/entity paths, and compile-only dependency scope.

- [ ] **Step 2: Run the closure policy and affected module checks.**

```bash
./gradlew :modules:shared:check :modules:assistant:check :modules:appointments:check :modules:catalog:check :modules:clients:check :modules:documents:check :modules:services:check :modules:salon:check :modules:calendar:check :modules:payment:check :modules:notification:check :modules:subscriptions:check :modules:identity:check :modules:tenancy:check --no-parallel --no-configuration-cache
```

Expected: BUILD SUCCESSFUL with zero failed or skipped Lombok policy tests.

- [ ] **Step 3: Run the source and dependency audits.**

```bash
rg -n '@(Data|Delegate|SneakyThrows|Synchronized|ExtensionMethod|StandardException)\b' modules libraries applications build-logic --glob '!**/build/**' --glob '*.java'
rg -n 'import lombok|@Builder' modules libraries applications build-logic --glob '!**/build/**' --glob '*.java'
./gradlew :modules:assistant:dependencies --configuration runtimeClasspath --no-parallel --no-configuration-cache
git diff --check
```

Expected: the first command returns no forbidden annotation usages, the second
matches only approved source-policy entries, and runtime classpaths do not
contain Lombok.

- [ ] **Step 4: Update the ledgers.** Mark adopted slices complete, list every rejected/deferred candidate with its reason, record the final source count and approved annotation count, and retain the explicit exclusions. Add any new follow-up task instead of broadening scope.

- [ ] **Step 5: Run the final phase gates.** Run full repository `check`, Spotless, Checkstyle, JaCoCo, dependency/security, architecture, startup, and smoke checks at the framework phase checkpoint. Run Docker/Testcontainers PostgreSQL, Redis, Kafka, AGE, pgvector, and concurrency gates when Docker is available.

- [ ] **Step 6: Commit and push the closure.**

```bash
git add modules/shared/src/test/java/com/emme/shared/architecture/LombokUsagePolicyTest.java docs/superpowers/plans/2026-09-05-selective-lombok-adoption.md docs/superpowers/plans/2026-09-10-lombok-whole-repository-refactor.md tasks/todo.md tasks/lessons.md
git commit -m "docs(lombok): close whole repository simplification audit"
git push origin feat/ai-platform-foundation
git log --oneline origin/feat/ai-platform-foundation -1
git status --short
```

## 4. Definition of done

- [ ] All owned Java source sets have an auditable inventory excluding generated `build/` output.
- [ ] All accepted constructor and logger candidates are migrated with focused red/green tests.
- [ ] Immutable values, equality, builders, resource cleanup, field constants, null checks, and test fixtures have explicit adopt/reject decisions.
- [ ] Risky annotations remain absent from source annotation usage.
- [ ] Domain, JPA, tenant, security, provider, workflow, and configuration boundaries retain their required explicit behavior.
- [ ] Every builder uses `setterPrefix = "with"`.
- [ ] Runtime classpaths remain free of Lombok.
- [ ] Affected checks and phase gates pass, with Docker limitations documented when applicable.
- [ ] Plan, Lombok ledger, `tasks/todo.md`, and lessons are updated.
- [ ] Every logical change is committed conventionally and pushed to `feat/ai-platform-foundation`.
