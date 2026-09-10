# Task 2 Report — Safe Lombok Constructor Reductions

| Field | Value |
|---|---|
| Task | Task 2 — Apply safe constructor reduction slices |
| Date | 2026-09-10 |
| Branch | `feat/ai-platform-foundation` |
| Brief | [`task-2-brief.md`](task-2-brief.md) |
| Plan | [`2026-09-10-lombok-whole-repository-refactor.md`](../../docs/superpowers/plans/2026-09-10-lombok-whole-repository-refactor.md) |
| Result | Complete; all Task 2 changes pushed |

## Scope and decision rule

The Section 1 constructor inventory was reviewed before editing. Only a class
with one public constructor whose body directly assigned every constructor
parameter to a corresponding `final` field was eligible. The generated public
`@RequiredArgsConstructor` had to preserve field declaration order and Spring
construction behavior exactly.

No qualifiers, validation, defaulting, overload delegation, `super(...)`,
optional-provider selection, custom side effects, lifecycle behavior, tenant
boundaries, JPA/persistence invariants, or configuration binding were changed.
Logger, value, and fixture candidates were not touched.

The inventory contained 56 constructor candidates: 48 adopted and 8 rejected.
The adopted work was performed in the plan's sequential module order. Salon and
Services were inspected but had no safe constructor candidate.

## Adopted candidates

Every file below received `import lombok.RequiredArgsConstructor`,
`@RequiredArgsConstructor`, and removal of its equivalent explicit constructor.

### Appointments — `82716cd1`

- `modules/appointments/src/main/java/com/emme/appointments/adapter/in/web/controller/DashboardController.java`
- `modules/appointments/src/main/java/com/emme/appointments/adapter/out/messaging/publisher/SpringAppointmentEventPublisher.java`
- `modules/appointments/src/main/java/com/emme/appointments/adapter/out/persistence/adapter/AppointmentCollisionAdapter.java`

### Assistant — `7c9a3788`

- `modules/assistant/src/main/java/com/emme/assistant/adapter/in/webhook/WhatsAppWebhookController.java`
- `modules/assistant/src/main/java/com/emme/assistant/adapter/in/webhook/WhatsAppWebhookMapper.java`
- `modules/assistant/src/main/java/com/emme/assistant/adapter/out/persistence/adapter/WhatsAppWebhookEventPersistenceAdapter.java`
- `modules/assistant/src/main/java/com/emme/assistant/adapter/out/persistence/mapper/ConversationEventPersistenceMapper.java`
- `modules/assistant/src/main/java/com/emme/assistant/adapter/out/tenant/ConfiguredWhatsAppTenantResolver.java`
- `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/in/messaging/SemanticCacheInvalidationListener.java`
- `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/storage/CatalogDesignImageReader.java`

### Calendar — `3fe36166`

- `modules/calendar/src/main/java/com/emme/calendar/adapter/in/web/controller/CalendarController.java`
- `modules/calendar/src/main/java/com/emme/calendar/adapter/in/web/controller/ClientCalendarController.java`
- `modules/calendar/src/main/java/com/emme/calendar/adapter/in/web/controller/GoogleOAuthController.java`
- `modules/calendar/src/main/java/com/emme/calendar/adapter/in/web/controller/SheetsController.java`
- `modules/calendar/src/main/java/com/emme/calendar/adapter/out/google/adapter/GoogleSheetsAdapter.java`
- `modules/calendar/src/main/java/com/emme/calendar/adapter/out/google/oauth/OAuthTokenSource.java`
- `modules/calendar/src/main/java/com/emme/calendar/adapter/out/persistence/adapter/CalendarPersistenceAdapter.java`
- `modules/calendar/src/main/java/com/emme/calendar/adapter/out/persistence/adapter/GoogleSpreadsheetLinkQueryAdapter.java`

### Catalog — `3dc6d2a6`

- `modules/catalog/src/main/java/com/emme/catalog/adapter/in/web/controller/CatalogController.java`

### Clients — `81dd42c3`

- `modules/clients/src/main/java/com/emme/clients/adapter/in/web/controller/CustomerController.java`

### Documents — `531175c0`

- `modules/documents/src/main/java/com/emme/documents/adapter/in/web/controller/DocumentController.java`
- `modules/documents/src/main/java/com/emme/documents/adapter/out/persistence/adapter/DocumentPersistenceAdapter.java`
- `modules/documents/src/main/java/com/emme/documents/adapter/out/search/HybridDocumentSearchAdapter.java`

### Identity — `41b4b687`

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

### Notification — `b9fa88af`

- `modules/notification/src/main/java/com/emme/notification/adapter/out/event/SpringNotificationEventPublisher.java`

### Payment — `3fe571b7`

- `modules/payment/src/main/java/com/emme/payment/adapter/out/messaging/publisher/SpringPaymentWorkflowEventPublisher.java`
- `modules/payment/src/main/java/com/emme/payment/adapter/out/persistence/adapter/PaymentWebhookEventPersistenceAdapter.java`

### Shared — `0203096c`

Shared was the only adopted module without existing Lombok convention wiring;
`id("emme.lombok")` was added to `modules/shared/build.gradle.kts`. The existing
compile-only/annotation-processor convention was reused.

- `modules/shared/src/main/java/com/emme/shared/persistence/jdbc/BootstrapConnectionExecutor.java`
- `modules/shared/src/main/java/com/emme/shared/search/postgres/PostgresHybridSearch.java`
- `modules/shared/src/main/java/com/emme/shared/web/advice/GlobalExceptionHandler.java`

### Subscriptions — `b7e7dbb4`

- `modules/subscriptions/src/main/java/com/emme/subscriptions/adapter/out/persistence/adapter/SubscriptionPersistenceAdapter.java`

### Tenancy — `30633509`

- `modules/tenancy/src/main/java/com/emme/tenancy/adapter/out/client/database/LiquibaseTenantSchemaMigrationAdapter.java`
- `modules/tenancy/src/main/java/com/emme/tenancy/adapter/out/messaging/publisher/SpringTenantEventPublisher.java`
- `modules/tenancy/src/main/java/com/emme/tenancy/adapter/out/persistence/adapter/AuditEventPersistenceAdapter.java`
- `modules/tenancy/src/main/java/com/emme/tenancy/adapter/out/persistence/adapter/TenantProvisioningPersistenceAdapter.java`
- `modules/tenancy/src/main/java/com/emme/tenancy/application/audit/AuditEventRecorder.java`

## Rejected candidates

These candidates were intentionally left explicit because generated
`@RequiredArgsConstructor` would not be exactly equivalent.

| File | Rejection reason |
|---|---|
| `modules/appointments/src/main/java/com/emme/appointments/adapter/out/persistence/adapter/AppointmentPersistenceAdapter.java` | Constructor creates `new AppointmentPersistenceMapper()`. |
| `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/provider/springai/advisor/PromptVersionAdvisor.java` | Constructor validates a required value for null/blank input. |
| `modules/clients/src/main/java/com/emme/clients/adapter/out/persistence/adapter/CustomerPersistenceAdapter.java` | Constructor creates `new CustomerPersistenceMapper()`. |
| `modules/salon/src/main/java/com/emme/salon/adapter/out/persistence/adapter/BookingPolicyPersistenceAdapter.java` | Constructor creates a mapper. |
| `modules/salon/src/main/java/com/emme/salon/adapter/out/persistence/adapter/BusinessProfilePersistenceAdapter.java` | Constructor creates a mapper. |
| `modules/salon/src/main/java/com/emme/salon/adapter/out/persistence/adapter/OperatingHoursPersistenceAdapter.java` | Constructor creates a mapper. |
| `modules/services/src/main/java/com/emme/services/adapter/out/persistence/adapter/ArtistPersistenceAdapter.java` | Constructor creates a mapper. |
| `modules/services/src/main/java/com/emme/services/adapter/out/persistence/adapter/ServicePersistenceAdapter.java` | Constructor creates a mapper. |

No candidate was rejected because of a missing dependency or unverified
behavior; the explicit constructors are the safe implementation for their
current custom behavior.

## TDD evidence

The shared `LombokUsagePolicyTest` allowlist was updated first for every
module slice. Before any source annotation was added, the focused policy run
failed because the newly allowlisted files lacked the Lombok import and
annotation. This was the required red signal. The source conversion then made
the same test pass, followed by the module checks and a refactor-quality pass.

Baseline policy before Task 2: 8 tests passed.

For each adopted module, the green command included the module's `test`,
`compileJava`, and `compileTestJava`; `compileIntegrationTestJava` was included
where that source set exists, plus the focused shared policy test. All green
runs completed with `BUILD SUCCESSFUL`:

| Module | Green evidence | Quality evidence |
|---|---|---|
| appointments | test, Java/test compile, shared policy | Spotless apply/check, Checkstyle main/test |
| assistant | test, Java/test/integration compile, shared policy | Spotless apply/check, Checkstyle main/test |
| calendar | test, Java/test/integration compile, shared policy | Spotless apply/check, Checkstyle main/test |
| catalog | test, Java/test/integration compile, shared policy | Spotless apply/check, Checkstyle main/test |
| clients | test, Java/test/integration compile, shared policy | Spotless apply/check, Checkstyle main/test |
| documents | test, Java/test/integration compile, shared policy | Spotless apply/check, Checkstyle main/test |
| identity | test, Java/test/integration compile, shared policy | Spotless apply/check, Checkstyle main/test |
| notification | test, Java/test/integration compile, shared policy | Spotless apply/check, Checkstyle main/test |
| payment | test, Java/test/integration compile, shared policy | Spotless apply/check, Checkstyle main/test |
| shared | policy test, Java/test/integration compile | Spotless apply/check, Checkstyle main/test |
| subscriptions | test, Java/test/integration compile, shared policy | Spotless apply/check, Checkstyle main/test |
| tenancy | test, Java/test/integration compile, shared policy | Spotless apply/check, Checkstyle main/test |

The focused policy command used for red and green phases was:

```shell
./gradlew :modules:shared:test \
  --tests com.emme.shared.architecture.LombokUsagePolicyTest \
  --no-parallel --no-configuration-cache
```

The quality command for each module was the module's `spotlessApply`,
`spotlessCheck`, `checkstyleMain`, and `checkstyleTest` tasks. All commands
used `--no-parallel --no-configuration-cache`.

## Commit and push evidence

Each module slice was committed and pushed before the next slice:

| Order | Commit | Scope |
|---:|---|---|
| 1 | `82716cd1` | appointments |
| 2 | `7c9a3788` | assistant |
| 3 | `3fe36166` | calendar |
| 4 | `3dc6d2a6` | catalog |
| 5 | `81dd42c3` | clients |
| 6 | `531175c0` | documents |
| 7 | `41b4b687` | identity |
| 8 | `b9fa88af` | notification |
| 9 | `3fe571b7` | payment |
| 10 | `0203096c` | shared |
| 11 | `b7e7dbb4` | subscriptions |
| 12 | `30633509` | tenancy |

All use the conventional message form
`refactor(lombok): simplify <module> constructors`.

## Concerns and limitations

- The shared module's first combined verification command incorrectly passed
  `--tests` to Checkstyle and failed with `Unknown command-line option
  '--tests'`. The policy test and quality tasks were immediately rerun as
  separate valid commands and passed; no source issue resulted.
- No separate live Docker/Testcontainers integration run was introduced for
  this compile-time-only constructor refactor. Existing module test tasks and
  integration-test compilation passed. No live-test failure blocked Task 2.
- One transient Git index-lock condition occurred while preparing the
  Notification slice. No active Git process held the lock; the conventional
  commit and push completed successfully.
- The pre-existing `tgrep/` directory remains untracked, unstaged, and
  untouched.

## Final audit

The final focused policy test was rerun after the last source slice. The final
working tree audit is required to show only the pre-existing untracked
`tgrep/` directory, and this report plus the plan/task notes are being pushed
with the final Task 2 documentation commit.

## Report correction addendum — 2026-09-10

This addendum corrects F-01 from `task-2-review.md`. The adopted-candidate
inventory above now uses the exact 48 production source paths from the Task 2
range `2406a826..db23c045` and the current `LombokUsagePolicyTest` allowlist.
The corrected entries preserve the original module and commit grouping; no
production source, policy assertion, or policy behavior was changed.

Verification performed after the correction:

- The adopted list contains 48 paths.
- The adopted list matches the 48 production paths changed in
  `git diff --name-only 2406a826..db23c045 -- 'modules/**/src/main/java/**/*.java'`.
- Every adopted path exists in the checkout and is present in the policy
  allowlist.
- `git diff --check` reports no whitespace errors for the corrected report.
- The pre-existing untracked `tgrep/` directory remains untouched.
