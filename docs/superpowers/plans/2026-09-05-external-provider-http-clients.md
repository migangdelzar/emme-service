# External Provider HTTP Clients Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:test-driven-development` for every implementation task. Read the repository `AGENTS.md` instructions before editing code.

**Goal:** Replace ordinary synchronous provider calls that currently use zero-value OkHttp wrappers with capability-scoped Spring `RestClient` beans, while preserving provider-specific request, authentication, signing, error, retry, idempotency, and payment semantics.

**Architecture:** Keep application ports provider-neutral. Configure one named `RestClient` per external capability from the Spring-managed `RestClient.Builder`; inject that client into outbound adapters. Use `MockRestServiceServer` for provider contract tests and retain a small `MockWebServer` suite for real transport behavior. Keep OkHttp for black-box E2E sessions and transport-only tests. Do not introduce a universal HTTP abstraction.

**Tech Stack:** Java 25 preview, Spring Boot 4.1, Spring Framework 7 `RestClient`, JUnit 5, AssertJ, Mockito, Spring Test `MockRestServiceServer`, OkHttp 4.12 `MockWebServer` for selected transport tests, Gradle.

## 1. Scope and non-goals

This plan is intentionally executable in a separate session. It covers the production outbound REST clients in Identity, Calendar, Notification, Payment, and the already-completed WhatsApp pattern in Assistant.

In scope:

- Keycloak authentication and administration calls.
- Google OAuth, Calendar, and Sheets calls.
- Notification providers: Twilio, MessageBird, Vonage, FCM, SendGrid, SES, and APNs.
- Payment providers: Conekta, Mercado Pago, Stripe, and PayPal.
- Removal of `GoogleHttpClient`, `NotificationHttpClient`, and `PaymentHttpClient` only after all callers and production dependencies are gone.
- Migration of provider contract tests from `MockWebServer` to `MockRestServiceServer`, plus a deliberately small real-transport test matrix.
- Build/dependency cleanup with explicit retention of OkHttp where it remains test-only or is needed by an independent E2E client.

Out of scope:

- No replacement of Spring AI `ChatClient`, model transports, or Spring AI streaming.
- No migration of inbound dashboard SSE (`DashboardController`/`DashboardBroadcaster`).
- No production outbound WebSocket or SSE client; none was found in the current inventory.
- No WebFlux/WebClient dependency or reactive rewrite. Introduce WebClient only in a separate plan when a real reactive streaming requirement exists.
- No gRPC implementation. Use grpc-java with its appropriate transport in a separate plan; neither `RestClient` nor OkHttp is the gRPC client abstraction.
- No provider SDK adoption during this migration. A future SDK replacement requires a separate protocol/auth/error comparison and contract-preserving plan.
- No OkHttp major-version upgrade; the repository currently pins OkHttp 4.12.0.

## 2. Baseline inventory

### Production files expected to change

| Capability | Current configuration/wrapper | Current outbound implementations |
|---|---|---|
| Identity | `modules/identity/src/main/java/com/emme/identity/configuration/IdentityClientConfiguration.java` | `modules/identity/src/main/java/com/emme/identity/adapter/out/client/keycloak/KeycloakAdminClient.java`; `modules/identity/src/main/java/com/emme/identity/adapter/out/client/keycloak/KeycloakUserAuthenticationAdapter.java` |
| Calendar | `modules/calendar/src/main/java/com/emme/calendar/configuration/GoogleClientConfiguration.java`; `modules/calendar/src/main/java/com/emme/calendar/configuration/GoogleHttpClient.java` | `modules/calendar/src/main/java/com/emme/calendar/adapter/out/google/client/GoogleCalendarClient.java`; `modules/calendar/src/main/java/com/emme/calendar/adapter/out/google/client/GoogleSheetsClient.java`; `modules/calendar/src/main/java/com/emme/calendar/adapter/out/google/adapter/ClientCalendarSyncAdapter.java`; `modules/calendar/src/main/java/com/emme/calendar/adapter/out/google/adapter/GoogleOAuthAdapter.java`; `modules/calendar/src/main/java/com/emme/calendar/adapter/out/google/adapter/StaffCalendarSyncAdapter.java` |
| Notification | `modules/notification/src/main/java/com/emme/notification/configuration/NotificationClientConfiguration.java`; `modules/notification/src/main/java/com/emme/notification/configuration/NotificationHttpClient.java` | `modules/notification/src/main/java/com/emme/notification/adapter/out/provider/email/SendGridProvider.java`; `modules/notification/src/main/java/com/emme/notification/adapter/out/provider/email/SesEmailProvider.java`; `modules/notification/src/main/java/com/emme/notification/adapter/out/provider/push/ApnsPushProvider.java`; `modules/notification/src/main/java/com/emme/notification/adapter/out/provider/push/FcmPushProvider.java`; `modules/notification/src/main/java/com/emme/notification/adapter/out/provider/sms/MessageBirdProvider.java`; `modules/notification/src/main/java/com/emme/notification/adapter/out/provider/sms/TwilioSmsProvider.java`; `modules/notification/src/main/java/com/emme/notification/adapter/out/provider/sms/VonageProvider.java` |
| Payment | `modules/payment/src/main/java/com/emme/payment/configuration/PaymentClientConfiguration.java`; `modules/payment/src/main/java/com/emme/payment/configuration/PaymentHttpClient.java` | `modules/payment/src/main/java/com/emme/payment/adapter/out/provider/conekta/ConektaProvider.java`; `modules/payment/src/main/java/com/emme/payment/adapter/out/provider/mercadopago/MercadoPagoProvider.java`; `modules/payment/src/main/java/com/emme/payment/adapter/out/provider/stripe/StripeProvider.java`; `modules/payment/src/main/java/com/emme/payment/adapter/out/provider/paypal/PayPalProvider.java` |

The Assistant baseline is already complete and is the reference implementation:

- `modules/assistant/src/main/java/com/emme/assistant/configuration/WhatsAppClientConfiguration.java`
- `modules/assistant/src/main/java/com/emme/assistant/adapter/out/client/whatsapp/WhatsAppReplyAdapter.java`
- `modules/assistant/src/test/java/com/emme/assistant/adapter/out/client/whatsapp/WhatsAppReplyAdapterTest.java`

### Files deliberately retained

- `applications/emme-platform/src/e2eTest/java/com/emme/client/UserSession.java`: independent per-user OkHttp E2E session.
- `applications/emme-platform/build.gradle.kts`: E2E OkHttp dependencies.
- `modules/ai-platform/src/test/**`: MockWebServer coverage for Spring AI provider wiring, unless a test is proven to exercise this migration.
- `modules/appointments/src/main/java/com/emme/appointments/adapter/in/web/controller/DashboardController.java` and `modules/appointments/src/main/java/com/emme/appointments/adapter/in/web/sse/DashboardBroadcaster.java`: inbound MVC SSE.
- `applications/emme-platform/src/main/java/com/emme/ContainerHealthCheck.java`: JDK `HttpClient` health probe, unrelated to provider gateways.
- `libraries/ai-contracts/**`: framework/provider-free architecture boundary.

## 3. Design decisions to hold during implementation

1. **Default synchronous REST client:** named `RestClient` bean per capability, built from injected `RestClient.Builder`. Do not inject `OkHttpClient` into production provider adapters after their migration.
2. **Provider boundary:** provider classes remain outbound adapters and may use `RestClient` directly. `RestClient` types must not cross application ports or enter domain packages.
3. **No universal gateway:** do not create `ExternalHttpClient`, `ProviderHttpClient`, or a wrapper that only delegates `newCall`. Keep provider-specific methods and error semantics visible.
4. **Base URLs and hosts:** use a client base URL only when it represents one stable provider host. For providers with multiple hosts or token/resource hosts, build the request URI explicitly while reusing the capability-scoped client.
5. **Timeouts:** configure connect/read/request timeouts once in the named client. A per-call timeout is permitted only when a provider operation has a documented materially different SLA and has a focused test.
6. **Authentication:** put static provider headers in the client only when they are safe and immutable. Put tenant/user/token/JWT/idempotency headers at the operation boundary.
7. **Status handling:** use `retrieve().onStatus(...)` for ordinary failure mapping. Use `exchange(...)` when a status is itself part of business behavior, such as Google delete returning 410 as an accepted result, or when response headers/body must be inspected together.
8. **Forms and bytes:** use `MultiValueMap<String, String>` for form-encoded OAuth/token requests. For signed SES requests, sign the exact UTF-8 bytes that are passed to `RestClient.body(byte[])`; never serialize twice.
9. **Virtual threads:** keep calls synchronous and blocking at the adapter boundary. Spring Boot virtual threads are the concurrency policy; they do not require OkHttp and do not make WebClient mandatory. Do not add a reactive rewrite solely for virtual threads.
10. **HTTP/2:** the provider migration must not promise HTTP/2 as an application feature. Verify the selected request factory and provider compatibility in transport tests where useful; protocol negotiation remains an infrastructure concern.

## 4. Testing contract

### Primary provider tests

Provider contract tests bind a `RestClient.Builder` to `MockRestServiceServer`:

```java
RestClient.Builder builder = RestClient.builder();
MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
RestClient client = builder.baseUrl("https://provider.test").build();
```

Each migrated provider test must assert the externally visible contract, not private implementation details:

- method, resolved URL, query/form/body encoding, and content type;
- authentication and operation-specific headers;
- successful response mapping;
- malformed/empty response behavior;
- relevant 4xx, 401/403, 409, 429, and 5xx mapping;
- timeout/transport exception mapping where the existing provider has a defined failure contract;
- idempotency, signing, token refresh, or chained-call behavior where applicable.

The test must be written and run red before the production migration, then made green with the smallest implementation, then refactored without changing behavior.

### Secondary transport tests

Use OkHttp 4.12 `MockWebServer` only for a small number of tests that exercise the actual HTTP stack:

- one Identity test for a real socket and timeout/disconnect behavior;
- one Calendar test for OAuth/resource-host URI handling;
- one Notification test for a provider header/body plus a delayed response;
- one Payment test for TLS/connection behavior and an idempotency header.

These tests must not duplicate every provider contract case. They are integration tests for request-factory wiring, pooling, timeout, and wire behavior. If HTTP/2 is explicitly enabled by the chosen request factory, add one protocol-negotiation assertion only where the test environment supports it deterministically.

### E2E tests

Keep `UserSession` on OkHttp. It is an independent black-box client and is not evidence that production provider adapters should use OkHttp.

## 5. Ordered implementation tasks

Each task is a vertical slice. The implementer must follow Red → Green → Refactor, run the focused command after each stage, and commit only that slice. Do not delete a wrapper until the task that migrates its final caller is green.

### HTTP-01 — Freeze the transport policy and migration ledger

**Dependencies:** none.

**Files:**

- Modify: `docs/superpowers/migrations/framework-first-migration-ledger.md`
- Modify: `tasks/todo.md`
- Create: `applications/emme-platform/src/test/java/com/emme/ExternalProviderHttpBoundaryArchitectureTest.java`

**Test-first work:** add an architecture/inventory test that scans stable `src/main` and Gradle source files only. It must document and verify the currently retained OkHttp locations (`UserSession`, test fixtures, and transport tests), while keeping the provider production imports in an explicit migration allowlist. Do not walk `build/` output. HTTP-02 through HTTP-12 remove one provider family from that allowlist; HTTP-13 changes the assertion to reject all ordinary production provider imports.

**Implementation:** record the four capability clients, the retained OkHttp exceptions, the test split (`MockRestServiceServer` versus `MockWebServer`), and the explicit non-goals in the migration ledger. Add the Phase F focused-plan link to `tasks/todo.md`.

**Verification:**

```bash
./gradlew :applications:emme-platform:test --tests '*ExternalProviderHttpBoundaryArchitectureTest' --no-parallel --no-configuration-cache
git diff --check
```

The architecture test is expected to remain red until all production provider waves are complete; each provider task must also have its own green focused tests.

**Commit:** `docs(integrations): define provider HTTP migration policy`

### HTTP-02 — Migrate Identity and Keycloak calls

**Dependencies:** HTTP-01.

**Files:**

- Modify: `modules/identity/src/main/java/com/emme/identity/configuration/IdentityClientConfiguration.java`
- Modify: `modules/identity/src/main/java/com/emme/identity/adapter/out/client/keycloak/KeycloakAdminClient.java`
- Modify: `modules/identity/src/main/java/com/emme/identity/adapter/out/client/keycloak/KeycloakUserAuthenticationAdapter.java`
- Create or modify: `modules/identity/src/test/java/com/emme/identity/configuration/IdentityClientConfigurationTest.java`
- Create or modify: `modules/identity/src/test/java/com/emme/identity/adapter/out/client/keycloak/KeycloakProviderContractTest.java`

**Test-first cases:** token form encoding; userinfo GET; admin user creation chain; role and password operations; token/error response mapping; malformed JSON; 401/403; timeout. Assert that the adapter uses one injected named client and does not construct OkHttp.

**Implementation:** expose one `identityRestClient` bean from the Spring-managed builder with the existing timeout policy. Inject `RestClient` into both adapters. Preserve ObjectMapper usage only for provider-specific response parsing that cannot be represented safely by existing DTOs, and preserve all current Keycloak URLs, realms, client IDs, and chained-call ordering.

**Verification:**

```bash
./gradlew :modules:identity:test :modules:identity:compileJava --no-parallel --no-configuration-cache
```

**Commit:** `refactor(identity): migrate Keycloak adapters to RestClient`

### HTTP-03 — Establish the Notification RestClient boundary

**Dependencies:** HTTP-01.

**Files:**

- Modify: `modules/notification/src/main/java/com/emme/notification/configuration/NotificationClientConfiguration.java`
- Modify: `modules/notification/src/test/java/com/emme/notification/NotificationPackageConventionTest.java`
- Create: `modules/notification/src/test/java/com/emme/notification/configuration/NotificationClientConfigurationTest.java`

**Test-first cases:** named singleton client exists; configured timeout is applied; builder/base URL behavior is deterministic; provider packages do not instantiate clients. Do not migrate provider behavior in this task.

**Implementation:** add the named `notificationRestClient` bean using the managed builder. Keep `NotificationHttpClient` temporarily so the subsequent provider slices can migrate one caller group at a time. Do not add a generic provider interface.

**Verification:**

```bash
./gradlew :modules:notification:test --tests '*ClientConfigurationTest' --tests '*NotificationPackageConventionTest' --no-parallel --no-configuration-cache
```

**Commit:** `refactor(notification): establish named RestClient boundary`

### HTTP-04 — Migrate Twilio SMS

**Dependencies:** HTTP-03.

**Files:**

- Modify: `modules/notification/src/main/java/com/emme/notification/adapter/out/provider/sms/TwilioSmsProvider.java`
- Modify: `modules/notification/src/test/java/com/emme/notification/adapter/out/provider/TwilioSmsProviderContractTest.java`

**Test-first cases:** form body and authorization; successful message mapping; 4xx/429/5xx mapping; timeout mapping; malformed response. Replace the current `MockWebServer` contract setup with `MockRestServiceServer`.

**Implementation:** inject `@Qualifier("notificationRestClient") RestClient`, submit the same form fields and headers, and preserve the existing application port and exception semantics.

**Verification:**

```bash
./gradlew :modules:notification:test --tests '*TwilioSmsProviderContractTest' --no-parallel --no-configuration-cache
```

**Commit:** `refactor(notification): migrate Twilio sender to RestClient`

### HTTP-05 — Migrate MessageBird and Vonage SMS

**Dependencies:** HTTP-04.

**Files:**

- Modify: `modules/notification/src/main/java/com/emme/notification/adapter/out/provider/sms/MessageBirdProvider.java`
- Modify: `modules/notification/src/main/java/com/emme/notification/adapter/out/provider/sms/VonageProvider.java`
- Modify: `modules/notification/src/test/java/com/emme/notification/adapter/out/provider/MessageBirdProviderContractTest.java`
- Modify: `modules/notification/src/test/java/com/emme/notification/adapter/out/provider/VonageProviderContractTest.java`

**Test-first cases:** provider-specific form/JSON shape, API-key/auth headers, success mapping, provider error mapping, and timeout. Use separate expectations for each provider; do not normalize their request models into one DTO.

**Implementation:** inject the named Notification client, preserve each provider’s endpoint and encoding, and remove only the direct OkHttp usage from these two classes.

**Verification:**

```bash
./gradlew :modules:notification:test --tests '*MessageBirdProviderContractTest' --tests '*VonageProviderContractTest' --no-parallel --no-configuration-cache
```

**Commit:** `refactor(notification): migrate MessageBird and Vonage senders`

### HTTP-06 — Migrate FCM and SendGrid

**Dependencies:** HTTP-03.

**Files:**

- Modify: `modules/notification/src/main/java/com/emme/notification/adapter/out/provider/push/FcmPushProvider.java`
- Modify: `modules/notification/src/main/java/com/emme/notification/adapter/out/provider/email/SendGridProvider.java`
- Create or modify: `modules/notification/src/test/java/com/emme/notification/adapter/out/provider/FcmPushProviderContractTest.java`
- Create or modify: `modules/notification/src/test/java/com/emme/notification/adapter/out/provider/SendGridProviderContractTest.java`

**Test-first cases:** FCM OAuth token form request followed by send request; token failure short-circuit; SendGrid JSON body and `X-Message-Id`; 401/403/429/5xx; malformed response; no send after token failure.

**Implementation:** use the same injected client for both FCM hosts by supplying explicit URIs where required. Preserve the two-step ordering and SendGrid response-header extraction. Keep token caching or current lifecycle behavior unchanged unless a focused regression test proves a defect.

**Verification:**

```bash
./gradlew :modules:notification:test --tests '*FcmPushProviderContractTest' --tests '*SendGridProviderContractTest' --no-parallel --no-configuration-cache
```

**Commit:** `refactor(notification): migrate FCM and SendGrid providers`

### HTTP-07 — Migrate APNs and SES with signing safeguards

**Dependencies:** HTTP-03.

**Files:**

- Modify: `modules/notification/src/main/java/com/emme/notification/adapter/out/provider/push/ApnsPushProvider.java`
- Modify: `modules/notification/src/main/java/com/emme/notification/adapter/out/provider/email/SesEmailProvider.java`
- Create or modify: `modules/notification/src/test/java/com/emme/notification/adapter/out/provider/ApnsPushProviderContractTest.java`
- Create or modify: `modules/notification/src/test/java/com/emme/notification/adapter/out/provider/SesEmailProviderContractTest.java`
- Create: `modules/notification/src/test/java/com/emme/notification/adapter/out/provider/SesSigV4RequestTest.java`

**Test-first cases:** APNs JWT and required headers; APNs JSON body; SES canonical request, signed headers, payload hash, and authorization; successful response; signature rejection; provider 4xx/5xx. The SigV4 test must compare the exact signed request bytes and headers, not just a status code.

**Implementation:** inject `notificationRestClient`. For SES, build the final UTF-8 body once, compute the signature over that byte array, then pass the same bytes to `RestClient.body(byte[])`. Do not move signing into a generic interceptor until the existing canonical request behavior is fully covered.

**Verification:**

```bash
./gradlew :modules:notification:test --tests '*ApnsPushProviderContractTest' --tests '*SesEmailProviderContractTest' --tests '*SesSigV4RequestTest' --no-parallel --no-configuration-cache
```

**Commit:** `refactor(notification): migrate APNs and SES providers`

### HTTP-08 — Establish Payment RestClient and migrate Conekta/Mercado Pago

**Dependencies:** HTTP-01.

**Files:**

- Modify: `modules/payment/src/main/java/com/emme/payment/configuration/PaymentClientConfiguration.java`
- Modify: `modules/payment/src/main/java/com/emme/payment/adapter/out/provider/conekta/ConektaProvider.java`
- Modify: `modules/payment/src/main/java/com/emme/payment/adapter/out/provider/mercadopago/MercadoPagoProvider.java`
- Modify: `modules/payment/src/test/java/com/emme/payment/PaymentPackageConventionTest.java`
- Create or modify: `modules/payment/src/test/java/com/emme/payment/configuration/PaymentClientConfigurationTest.java`
- Create: `modules/payment/src/test/java/com/emme/payment/adapter/out/provider/ConektaProviderContractTest.java`
- Create: `modules/payment/src/test/java/com/emme/payment/adapter/out/provider/MercadoPagoProviderContractTest.java`

**Test-first cases:** request JSON and authorization; existing no-op/unsupported `authorize` and `capture` behavior; successful create/refund mapping; malformed response; provider 4xx/5xx; no accidental state transition when an operation is unsupported.

**Implementation:** add `paymentRestClient`, inject it into both providers, and preserve current application port semantics. Before changing any payment operation, document whether the provider supports it and add the corresponding regression test. Do not infer support from the HTTP endpoint alone.

**Verification:**

```bash
./gradlew :modules:payment:test :modules:payment:compileJava --no-parallel --no-configuration-cache
```

**Commit:** `refactor(payment): migrate Conekta and Mercado Pago providers`

### HTTP-09 — Migrate Stripe and PayPal

**Dependencies:** HTTP-08.

**Files:**

- Modify: `modules/payment/src/main/java/com/emme/payment/adapter/out/provider/stripe/StripeProvider.java`
- Modify: `modules/payment/src/main/java/com/emme/payment/adapter/out/provider/paypal/PayPalProvider.java`
- Modify: `modules/payment/src/test/java/com/emme/payment/adapter/out/provider/StripeProviderContractTest.java`
- Create or modify: `modules/payment/src/test/java/com/emme/payment/adapter/out/provider/PayPalProviderContractTest.java`

**Test-first cases:** Stripe form encoding, Authorization, and `Idempotency-Key`; Stripe initiate/refund/error mapping; PayPal OAuth form request followed by order/refund JSON calls; token failure short-circuit; 401/403/409/429/5xx; malformed response; timeout.

**Implementation:** use explicit URIs for PayPal token/resource hosts if needed. Preserve idempotency keys exactly and keep payment state transitions outside the HTTP client. Replace the existing OkHttp-based Stripe test with MockRestServiceServer; add a real MockWebServer transport case in HTTP-13.

**Verification:**

```bash
./gradlew :modules:payment:test --tests '*StripeProviderContractTest' --tests '*PayPalProviderContractTest' --no-parallel --no-configuration-cache
```

**Commit:** `refactor(payment): migrate Stripe and PayPal providers`

### HTTP-10 — Establish Google RestClient and migrate OAuth

**Dependencies:** HTTP-01.

**Files:**

- Modify: `modules/calendar/src/main/java/com/emme/calendar/configuration/GoogleClientConfiguration.java`
- Modify: `modules/calendar/src/main/java/com/emme/calendar/adapter/out/google/adapter/GoogleOAuthAdapter.java`
- Modify: `modules/calendar/src/test/integrationTest/java/com/emme/calendar/GoogleCalendarClientLiveTest.java` only if its setup references the deleted wrapper; keep its live-test purpose intact
- Create: `modules/calendar/src/test/java/com/emme/calendar/configuration/GoogleClientConfigurationTest.java`
- Create or modify: `modules/calendar/src/test/java/com/emme/calendar/adapter/out/google/GoogleOAuthProviderContractTest.java`

**Test-first cases:** authorization-code exchange; refresh; revoke; status lookup; form encoding; token response parsing; provider error mapping; incomplete configuration; explicit host/URI behavior.

**Implementation:** create the named `googleRestClient` bean, inject it into the OAuth adapter, and preserve the current credential selection and tenant/user/persona keys. Keep live-test networking disabled by default and do not turn it into a unit test.

**Verification:**

```bash
./gradlew :modules:calendar:test --tests '*GoogleClientConfigurationTest' --tests '*GoogleOAuthProviderContractTest' --no-parallel --no-configuration-cache
```

**Commit:** `refactor(calendar): migrate Google OAuth to RestClient`

### HTTP-11 — Migrate Google Calendar and Sheets clients

**Dependencies:** HTTP-10.

**Files:**

- Modify: `modules/calendar/src/main/java/com/emme/calendar/adapter/out/google/client/GoogleCalendarClient.java`
- Modify: `modules/calendar/src/main/java/com/emme/calendar/adapter/out/google/client/GoogleSheetsClient.java`
- Modify: `modules/calendar/src/integrationTest/java/com/emme/calendar/GoogleCalendarClientLiveTest.java` only for constructor/client wiring, if needed
- Create or modify: `modules/calendar/src/test/java/com/emme/calendar/adapter/out/google/GoogleCalendarProviderContractTest.java`
- Create or modify: `modules/calendar/src/test/java/com/emme/calendar/adapter/out/google/GoogleSheetsProviderContractTest.java`

**Test-first cases:** Calendar create/delete/free-busy; accepted 410 delete; Sheets create/write; bearer token; JSON request/response mapping; malformed response; 401/403/404/429/5xx and timeout.

**Implementation:** inject `googleRestClient`, use `exchange(...)` for status-sensitive delete behavior, and retain provider-specific DTOs and ObjectMapper mapping where the current response shapes require it. Do not alter sync scheduling or domain ports.

**Verification:**

```bash
./gradlew :modules:calendar:test --tests '*GoogleCalendarProviderContractTest' --tests '*GoogleSheetsProviderContractTest' --no-parallel --no-configuration-cache
```

**Commit:** `refactor(calendar): migrate Google resource clients to RestClient`

### HTTP-12 — Migrate Calendar synchronization adapters

**Dependencies:** HTTP-11.

**Files:**

- Modify: `modules/calendar/src/main/java/com/emme/calendar/adapter/out/google/adapter/ClientCalendarSyncAdapter.java`
- Modify: `modules/calendar/src/main/java/com/emme/calendar/adapter/out/google/adapter/StaffCalendarSyncAdapter.java`
- Create: `modules/calendar/src/test/java/com/emme/calendar/adapter/out/google/adapter/ClientCalendarSyncAdapterTest.java`
- Create: `modules/calendar/src/test/java/com/emme/calendar/adapter/out/google/adapter/StaffCalendarSyncAdapterTest.java`

**Test-first cases:** injected Google client/gateway wiring; tenant context restoration for durable events; client/provider selection; sync status update after provider failure; no direct OkHttp construction; retryable versus terminal error propagation.

**Implementation:** remove only the wrapper dependency from these adapters. Preserve the existing application ports, tenant restoration, event handling, and status-update failure behavior. If a client abstraction is needed for test isolation, define a capability-specific interface at the adapter boundary rather than a generic HTTP interface.

**Verification:**

```bash
./gradlew :modules:calendar:test :modules:calendar:compileJava --no-parallel --no-configuration-cache
```

**Commit:** `refactor(calendar): remove Google sync wrapper dependency`

### HTTP-13 — Delete wrappers, clean dependencies, and add transport tests

**Dependencies:** HTTP-02, HTTP-05, HTTP-06, HTTP-07, HTTP-09, HTTP-12.

**Files:**

- Delete only after search proves no references: the old OkHttp bean in `modules/identity/src/main/java/com/emme/identity/configuration/IdentityClientConfiguration.java`; `modules/calendar/src/main/java/com/emme/calendar/configuration/GoogleHttpClient.java`; `modules/notification/src/main/java/com/emme/notification/configuration/NotificationHttpClient.java`; `modules/payment/src/main/java/com/emme/payment/configuration/PaymentHttpClient.java`
- Modify: `modules/assistant/build.gradle.kts` if its direct OkHttp implementation dependency is now unused
- Modify: `modules/payment/build.gradle.kts` to remove production OkHttp only when no main-source import remains
- Modify: `modules/notification/build.gradle.kts`, `modules/calendar/build.gradle.kts`, and `modules/identity/build.gradle.kts` only where dependency analysis proves a production OkHttp declaration is obsolete
- Retain: `applications/emme-platform/build.gradle.kts` E2E OkHttp dependencies and `gradle/libs.versions.toml` MockWebServer aliases when still used
- Create: `modules/identity/src/integrationTest/java/com/emme/identity/adapter/out/client/keycloak/KeycloakRestClientTransportTest.java`
- Create: `modules/calendar/src/integrationTest/java/com/emme/calendar/adapter/out/google/GoogleRestClientTransportTest.java`
- Create: `modules/notification/src/integrationTest/java/com/emme/notification/adapter/out/provider/NotificationRestClientTransportTest.java`
- Create: `modules/payment/src/integrationTest/java/com/emme/payment/adapter/out/provider/PaymentRestClientTransportTest.java`

**Test-first cases:** real socket request; delayed response/timeout; disconnect; one capability-specific header/body; optional deterministic HTTP/2 negotiation if the configured request factory enables it. Use `MockWebServer`, not a second copy of every provider contract.

**Implementation:** run `rg` over `src/main`, source tests, and build scripts before each deletion. Remove old beans/wrappers only when no production source, test fixture, architecture test, or build declaration references them. Keep E2E and test-only OkHttp dependencies with comments/tests explaining their purpose if dependency analysis would otherwise suggest deletion.

**Verification:**

```bash
rg -n 'okhttp3|OkHttpClient|GoogleHttpClient|NotificationHttpClient|PaymentHttpClient' modules applications --glob 'src/main/**' --glob '*.gradle.kts'
./gradlew :modules:identity:test :modules:calendar:test :modules:notification:test :modules:payment:test :applications:emme-platform:test --no-parallel --no-configuration-cache
./gradlew test compileJava --no-daemon --no-parallel --no-configuration-cache
```

The `rg` result may include deliberately retained E2E/test-only locations; it must contain no ordinary production provider adapter imports or deleted wrapper references.

**Commit:** `refactor(integrations): remove obsolete provider HTTP wrappers`

## 6. Dependency and execution graph

```text
HTTP-01
├── HTTP-02 Identity
├── HTTP-03 Notification foundation ──┬── HTTP-04 Twilio ── HTTP-05 MessageBird/Vonage
│                                    ├── HTTP-06 FCM/SendGrid
│                                    └── HTTP-07 APNs/SES
├── HTTP-08 Payment foundation/Conekta/Mercado Pago ── HTTP-09 Stripe/PayPal
└── HTTP-10 Google foundation/OAuth ── HTTP-11 Calendar/Sheets ── HTTP-12 sync adapters

HTTP-02 + HTTP-05 + HTTP-06 + HTTP-07 + HTTP-09 + HTTP-12
└── HTTP-13 cleanup and transport matrix
```

HTTP-04, HTTP-06, HTTP-07, and HTTP-08 can be developed in parallel after their respective foundations, but merge them sequentially if they modify the same configuration or build file. HTTP-13 is strictly last.

## 7. Checkpoints

### Checkpoint F1 — after HTTP-02, HTTP-03, and HTTP-04

- Identity uses the named Spring client.
- Notification has a named client and Twilio is migrated.
- Keycloak and Twilio contract tests pass with MockRestServiceServer.
- No new universal HTTP abstraction exists.

```bash
./gradlew :modules:identity:test :modules:notification:test --no-parallel --no-configuration-cache
```

### Checkpoint F2 — after HTTP-07 and HTTP-09

- All Notification and Payment provider contract tests pass.
- SES signatures are byte-for-byte covered.
- Stripe idempotency and PayPal token chaining are covered.
- Payment authorize/capture behavior is unchanged and explicitly tested.

```bash
./gradlew :modules:notification:test :modules:payment:test --no-parallel --no-configuration-cache
```

### Checkpoint F3 — after HTTP-12

- Google OAuth, Calendar, Sheets, and sync adapter tests pass.
- Google’s 410 delete behavior and tenant restoration remain covered.
- No Calendar production class depends on `GoogleHttpClient`.

```bash
./gradlew :modules:calendar:test :modules:calendar:compileJava --no-parallel --no-configuration-cache
```

### Checkpoint F4 — after HTTP-13

- Production provider adapters have no direct OkHttp imports.
- OkHttp remains only where intentionally retained for E2E, test fixtures, or transport tests.
- All focused modules compile and test; root test/compile passes.
- The migration ledger records deletions and retained exceptions.

## 8. Rollback boundaries and risks

Each provider task is independently revertible before the wrapper cleanup task. If a provider regression appears, restore that provider’s prior adapter and wrapper while leaving other migrated providers intact.

Highest-risk slice: SES SigV4. Roll back SES alone if canonical bytes, signed headers, or clock handling differ. Do not “fix” a signature failure by weakening verification or logging secrets.

Payment risk: preserve idempotency keys, provider operation support, and application state transitions. A successful HTTP response is not automatically a successful business operation.

Google/Keycloak risk: preserve token scope, realm, tenant, persona, and chained-call behavior. Avoid broad SDK adoption as a migration shortcut.

Transport risk: virtual threads improve blocking-call scalability but do not eliminate connection-pool limits, provider rate limits, timeout policy, or backpressure. Record any pool/factory change as a separate operational decision.

## 9. Future protocol decision table

| Requirement | Preferred technology in this codebase | Reason |
|---|---|---|
| Synchronous JSON/form REST from Spring MVC/virtual-thread code | Spring `RestClient` | Spring Boot-managed configuration, converters, observations, familiar tests, blocking semantics compatible with virtual threads |
| Reactive streaming or very high fan-out non-blocking calls | Spring `WebClient` | Use only with a real reactive execution path; do not add WebFlux for ordinary blocking REST |
| Server-side inbound SSE | Spring MVC `SseEmitter` as currently used | Existing dashboard flow is inbound and unrelated to provider migration |
| WebSocket client | A WebSocket-specific client selected for the use case; OkHttp may be appropriate for a simple client | `RestClient` is not a WebSocket API; decide reconnect/backpressure/session ownership separately |
| gRPC | grpc-java, normally Netty transport on JVM | gRPC is not REST and should use generated stubs and protocol-native status/deadline semantics |
| Black-box E2E HTTP session | OkHttp | Existing `UserSession` intentionally owns cookies/auth/session behavior independently of production wiring |
| Container health probe | JDK `HttpClient` as currently implemented | Small standalone JDK boundary; no need to force it through Spring |

## 10. Definition of done

- [ ] Every production migration task began with a failing focused test or explicit architecture/inventory test.
- [ ] All migrated provider contract tests use MockRestServiceServer for request/response semantics.
- [ ] At least one real-transport MockWebServer test exists per capability, without duplicating provider contracts.
- [ ] Keycloak, Google, Notification, and Payment request/auth/error semantics are unchanged.
- [ ] SES signing uses the exact bytes sent over the wire.
- [ ] Stripe idempotency, Google 410 delete handling, FCM token chaining, and payment operation support are explicitly covered.
- [ ] No application/domain port depends on Spring HTTP or OkHttp types.
- [ ] Deleted wrappers have no remaining production/test/build references.
- [ ] Retained OkHttp dependencies are documented by E2E or transport-test ownership.
- [ ] Module tests, compilation, Spotless, Checkstyle, and repository architecture tests pass.
- [ ] Changes are committed atomically and pushed to `feat/ai-platform-foundation`.

## 11. Authoritative references

- [Spring Framework REST clients](https://docs.spring.io/spring-framework/reference/integration/rest-clients.html)
- [Spring Boot REST client support](https://docs.spring.io/spring-boot/reference/io/rest-client.html)
- [Spring Boot virtual threads](https://docs.spring.io/spring-boot/reference/features/task-execution-and-scheduling.html)
- [Spring WebClient](https://docs.spring.io/spring-framework/reference/web/webflux-webclient.html)
- [Java 25 `HttpClient`](https://docs.oracle.com/en/java/javase/25/docs/api/java.net.http/java/net/http/HttpClient.html)
- [OkHttp official README](https://github.com/square/okhttp/blob/master/README.md)
- [grpc-java official README](https://github.com/grpc/grpc-java/blob/master/README.md)
