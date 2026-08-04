# Configuration Properties Normalization Plan

| Field | Detail |
|---|---|
| **Scope** | Spring Boot `@ConfigurationProperties` in `emme-service` |
| **Architecture rule** | Record-first, constraint-driven startup validation |
| **Status** | Planned final governance slice |
| **Depends on** | Module migrations, provider boundaries, and deployment profiles |
| **Out of scope** | Replacing Spring profiles, introducing a fluent-validation library, or validating disabled provider credentials |

## Decision

Stable constructor-bound settings should use immutable Java records. Mutable
properties classes remain only when a framework/binder integration or an
explicit migration dependency still requires setters. This is a migration
policy, not a rule to mechanically convert every class.

Validation belongs on the configuration boundary when the invariant is known at
startup. Use `@NotBlank`, `@NotNull`, numeric bounds, URI/format constraints,
and conditional validators only when they describe a real requirement for the
active mode. Optional provider credentials remain optional while that provider
is disabled; an enabled provider must fail startup or fail its provider contract
explicitly according to the module policy.

## Inventory and target state

| Property type | Current shape | Target | Validation focus | Reason for current exception, if any |
|---|---|---|---|---|
| `KafkaEventStreamingProperties` | Mutable class | Record after binder compatibility check | Bootstrap, group, retries, encrypted transport when enabled | Cross-field production safety rule already exists |
| `CalendarProperties` | Record | Keep record | Non-blank calendar identifier when external sync is enabled | Stable local default is valid |
| `GoogleCalendarProperties` | Record | Keep record | Endpoint URI and credential presence when Google sync is enabled | Credential is optional for mock/local mode |
| `GoogleOAuthProperties` | Record | Keep record | Client/redirect/encryption requirements when OAuth is enabled | All values are not required in every profile |
| `CatalogImageStorageProperties` | Record | Keep record | Non-blank bounded storage path | Local default is valid |
| `AiProperties` | Record | Keep record | Provider selection and nested model/base URL when non-mock | Provider credentials are conditional |
| `WhatsAppProperties` | Record | Keep record | UUID tenant and required webhook secret/token when enabled | Existing UUID parsing is a domain-neutral configuration check |
| `NotificationProperties` | Nested records | Keep record; add conditional provider validation | Active channel credentials and endpoint values | Provider blocks are intentionally optional |
| `PaymentProperties` | Nested records | Keep record; add conditional provider validation | Active provider credentials and webhook secrets | Mock provider and unused provider blocks remain optional |
| `IdentityRealmProvisioningProperties` | Mutable class | Record after constructor-binding migration | Role lists, redirect URIs, attempts, retry delay, password when enabled | Existing setter-based tests and process wiring must migrate together |
| `IdentityKeycloakProperties` | Mutable class | Record after binding migration | URI/client/realm values and admin secret when administration is enabled | Secret and customer realm are profile-dependent |
| `IdentitySecurityProperties` | Mutable class | Record after security configuration migration | Origins, methods, headers, max age, CSP source | Collections need defensive immutable copies |
| `IdentityRateLimitProperties` | Mutable class | Record | Positive attempts/window and valid proxy entries | Current setters encode invariants; tests must move to construction |
| `TenantDatabaseConnectionProperties` | Mutable class | Record only if Spring datasource fallback binding remains compatible | URL, driver, and credentials when fallback is active | Composition-root datasource lifecycle is sensitive |
| `TenantPoolingProperties` | Mutable class | Record after pool/provider constructor migration | Positive global/cache/pool limits and min ≤ max | Pool lifecycle code currently consumes bean getters |
| `RateLimitProperties` | Record | Keep record | Positive requests and non-zero duration | Defaults are safe for local/test profiles |

## Implementation phases

### Phase A — Binding and validation contracts

- [ ] Add failing properties tests for missing, blank, zero, negative, and
  conditional values for each active provider/configuration group.
- [ ] Convert one module at a time to constructor-bound records.
- [ ] Preserve profile-specific defaults and never use a production secret as a
  record default.
- [ ] Add `@Valid` for nested records where Spring Boot cascades binding
  validation.

### Phase B — Consumer migration

- [ ] Replace JavaBean getter calls with record accessors at composition roots,
  adapters, and application services.
- [ ] Remove setters only after all construction and test fixtures use the
  canonical record constructor.
- [ ] Preserve dependency injection boundaries; properties remain configuration
  inputs and never become provider/application services.

### Phase C — Conditional provider safety

- [ ] Define active-provider predicates for AI, Notification, Payment, Google,
  WhatsApp, Kafka, and Keycloak.
- [ ] Require credentials and secure transport only when a provider is enabled.
- [ ] Keep generic test fixtures on mock providers and disable asynchronous
  external listeners unless a provider-integration test explicitly opts in.
- [ ] Add startup-context tests for enabled, disabled, and missing flags.

### Phase D — Verification

- [ ] Run module properties tests and application context tests.
- [ ] Run service-wide formatting, Checkstyle, unit, integration, Modulith, and
  boot-JAR verification.
- [ ] Verify local JVM/native configuration does not bind incompatible values.
- [ ] Update this inventory and the final verification report with the exact
  remaining mutable classes and documented reasons.

## Non-goals and guardrails

- Do not put Jakarta validation annotations on domain models.
- Do not add `@NotNull` to primitive components; primitives cannot be null.
- Do not mark optional provider credentials `@NotBlank` when the provider is
  disabled.
- Do not hide cross-field provider rules in controllers or arbitrary setters.
- Do not introduce `FluentValidation` or another validation framework without a
  separate ADR proving that Jakarta Bean Validation and a focused custom
  validator cannot express the contract.
