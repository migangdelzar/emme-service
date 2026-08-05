# Schema-Per-Tenant & Realm-Per-Tenant Provisioning Design

| Field | Detail |
|---|---|
| Status | Design approved — pending implementation |
| Date | 2026-08-05 |
| Scope | `emme-service` tenancy + identity modules, E2E provisioner, compose infrastructure |
| Reference | `emme-modulith` schema-per-tenant pattern, Spring Modulith events + Kafka externalization |

## 1. Goal

Apply the full DDD + Hexagonal + Spring Modulith pattern to tenant provisioning:
- Each tenant gets its own **PostgreSQL schema** (`tenant_{slug}`) for business data isolation.
- Each tenant gets its own **Keycloak realm** (`emme-{slug}`) for authentication isolation.
- Provisioning is **event-driven** via Modulith application events (outbox pattern), replacing ShedLock polling.
- `emme_core` schema stores **tenant metadata only** (tenant, tenant_registry, membership, role, permission, audit).
- Hibernate multi-tenancy uses **dual connection pools** (core pool + tenant pool) instead of AspectJ `SET LOCAL search_path`.
- `TenantCreated` fires at creation time; `TenantActivated` fires when schema + realm are both ready.
- Kafka externalization for both events via Spring Modulith's `@Externalized`.

## 2. Architecture Overview

### 2.1 Event Chain

```
POST /api/tenants/v1.0
  │  CreateTenantService
  │  → INSERT emme_core.tenant (keycloak_realm=null, status=ACTIVE)
  │  → INSERT emme_core.tenant_registry (status=PROVISIONING)
  │  → publish TenantCreated  [@Externalized → Kafka: emme.tenancy.tenant-created]
  ▼
┌──────────────────────────────────────────────────────┐
│ Step 1: TenantSchemaProvisioningListener              │
│   @ApplicationModuleListener                          │
│   @Transactional                                      │
│   1. CREATE SCHEMA IF NOT EXISTS tenant_{slug}       │
│   2. Liquibase.update("emme-studio", "dev")           │
│   3. UPDATE tenant_registry SET status='READY'         │
│   4. publish TenantSchemaReady (internal only)        │
│                                                       │
│   Idempotent: schema EXISTS check.                    │
│   Retry: Modulith event republish on restart.         │
└──────────────────────┬───────────────────────────────┘
                       │
                       ▼
┌──────────────────────────────────────────────────────┐
│ Step 2: TenantRealmProvisioningListener               │
│   @ApplicationModuleListener                          │
│   @Transactional                                      │
│   1. Keycloak Admin API: create realm "emme-{slug}"   │
│   2. Keycloak: create client (emme-salon-app)         │
│   3. Keycloak: create roles (business_owner, etc.)    │
│   4. Keycloak: create admin user + assign role        │
│   5. UPDATE tenant SET keycloak_realm='emme-{slug}'   │
│   6. publish TenantRealmReady (internal only)         │
│                                                       │
│   Idempotent: check realm exists before create.       │
│   On retry: delete partial realm, recreate.           │
│   Retry: Modulith event republish on restart.         │
└──────────────────────┬───────────────────────────────┘
                       │
                       ▼
┌──────────────────────────────────────────────────────┐
│ Step 3: TenantActivationListener                      │
│   @ApplicationModuleListener                          │
│   @Transactional                                      │
│   1. UPDATE tenant_registry SET status='ACTIVE',      │
│      schema_version='0.1.0'                           │
│   2. publish TenantActivated                          │
│      [@Externalized → Kafka: emme.tenancy.activated]  │
│                                                       │
│   Tenant is now fully operational.                    │
│   External consumers receive the "ready" signal.      │
└──────────────────────────────────────────────────────┘
```

### 2.2 Events

| Event | Target | Partition Key | Visibility |
|---|---|---|---|
| `TenantCreated` | `emme.tenancy.tenant-created` (Kafka) | `tenantId` | External + Internal |
| `TenantSchemaReady` | Internal only (Modulith) | — | Internal |
| `TenantRealmReady` | Internal only (Modulith) | — | Internal |
| `TenantActivated` | `emme.tenancy.tenant-activated` (Kafka) | `tenantId` | External + Internal |

`TenantCreated` (existing record, unchanged fields):
```java
@Externalized("emme.tenancy.tenant-created::#{#this.tenantId()}")
public record TenantCreated(UUID eventId, UUID tenantId, String slug, String name) {}
```

`TenantActivated` (new):
```java
@Externalized("emme.tenancy.tenant-activated::#{#this.tenantId()}")
public record TenantActivated(UUID eventId, UUID tenantId, String slug, 
                               String schemaName, String keycloakRealm) {}
```

Internal events (`TenantSchemaReady`, `TenantRealmReady`) are plain records — no `@Externalized`, no Kafka topic.

### 2.3 Dual Connection Pools

```
                     ┌─────────────────────────────────┐
                     │   AbstractRoutingDataSource      │
                     │   lookupKey = "core" | "tenant"   │
                     └──────┬──────────────┬────────────┘
                            │              │
               ┌────────────▼──┐    ┌──────▼──────────────────────┐
               │  corePool     │    │  tenantPool                  │
               │  HikariCP     │    │  HikariCP                    │
               │               │    │                              │
               │  JDBC URL:    │    │  JDBC URL:                   │
               │  jdbc:pg://   │    │  jdbc:pg://host/emme         │
               │  host/emme    │    │  (no currentSchema param)    │
               │  ?currentSchema│   │                              │
               │  =emme_core   │    │  Managed by:                 │
               │               │    │  SchemaMultiTenantConn…      │
               │  Tables:       │    │  + CurrentTenantIdResolver  │
               │  emme_core.*   │    │  connection.setSchema(…)    │
               │               │    │                              │
               │  min:2 max:5  │    │  min:2 max:10               │
               └───────────────┘    └─────────────────────────────┘
```

**Routing Decision**: entries annotated `@Table(schema = "emme_core")` → core pool. All others → tenant pool. `CurrentTenantIdentifierResolver` returns `"emme_core"` when `TenantContext` is null (startup, health checks, unauthenticated requests).

**Why dual pools**: the login flow queries `emme_core` tables (membership, tenant, role) before any tenant context exists. The core pool handles these without needing search_path tricks. After authentication, the JWT carries `tenant_id`, the filter sets context, and business queries route through the tenant pool with the correct schema.

**Schema-per-tenant tables**: `@Table(name = "business_profile")` — no schema attribute. Hibernate resolves to the active schema from `TenantIdentifierResolver`. These tables live in `tenant_{slug}`.

**Global tables**: `@Table(name = "tenant", schema = "emme_core")` — explicit schema. These always route to the core pool. Tables in `emme_core`: `tenant`, `tenant_registry`, `membership`, `role`, `permission`, `role_permission`, `customer_identity`, `customer_membership`, `feature_flag`, `database_registry`, `platform_audit_event`, `event_publication`.

## 3. Module Responsibilities

### 3.1 Tenancy Module — New / Changed Files

| Artifact | Layer | Purpose |
|---|---|---|
| `TenantSchemaProvisioningListener` | adapter/in/messaging | Listens to `TenantCreated`, creates schema + runs Liquibase |
| `TenantActivationListener` | adapter/in/messaging | Listens to `TenantRealmReady`, marks ACTIVE |
| `TenantSchemaReady` | api/event | Internal event: schema migration complete |
| `TenantActivated` | api/event | `@Externalized` event: tenant fully operational |
| `SchemaAwareMultiTenantConnectionProvider` | adapter/out/client/database | Implements `MultiTenantConnectionProvider`, wraps tenant HikariCP |
| `CurrentTenantIdentifierResolver` | adapter/out/client/database | Implements `CurrentTenantIdentifierResolver`, reads from `TenantContext` |
| `DataSourceRoutingConfiguration` | configuration | Wires `AbstractRoutingDataSource` + both pools |
| `CoreDataSourceProperties` | configuration | `@ConfigurationProperties("emme.datasource.core")` |
| `TenantDataSourceProperties` | configuration | `@ConfigurationProperties("emme.datasource.tenant")` |

### 3.2 Tenancy Module — Files to Delete

| File | Reason |
|---|---|
| `TenantContextAspect.java` | Replaced by `MultiTenantConnectionProvider` |
| `TenantProvisioningProcessManager.java` | ShedLock polling replaced by event-driven listeners |
| `TenantProvisioningWorker.java` | Legacy JDBC path, replaced by listener |

### 3.3 Identity Module — Adapt

| Artifact | Change |
|---|---|
| `TenantCreatedConsumer.java` | Rename → `TenantRealmProvisioningListener`. Change event filter: `TenantCreated` → `TenantSchemaReady` |
| `KeycloakRealmProvisioningProcessManager.java` | Merge into `TenantRealmProvisioningListener`. Drop retry loop (Modulith handles retry). Keep idempotency logic. |
| `TenantRealmReady` (new, api/event) | Internal event published after realm provisioning completes |

### 3.4 Identity Module — Unchanged

- `KeycloakAdminClient` — called by new listener, same API
- `MultiRealmJwtDecoder` — already resolves per-realm JWTs, trusts `emme-*` prefixes
- `IdentityJwtTrustPolicy` — already accepts `{baseUrl}/realms/emme-{slug}` issuers

### 3.5 E2E Provisioner — Changes

| Artifact | Change |
|---|---|
| `E2eProvisionerApplication.java` | Synchronous flow: create tenant → schema migration → realm `"emme-e2e-studio"` → seed data → activate |
| `HttpKeycloakAdminClient.java` | Use realm `"emme-e2e-studio"` instead of `"emme"` |
| `RealmDocumentFactory.java` | Parameterize realm name: `"emme-" + slug` |
| Drop user `tenant_id` attribute | Realm IS the tenant boundary; no `tenant_id` claim needed on users |

## 4. Configuration Changes

### 4.1 `application-e2e.yml`

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: none
    properties:
      hibernate:
        multi_tenancy: DATABASE
  datasource:
    core:
      url: "jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/emme?currentSchema=emme_core"
      hikari:
        minimum-idle: 2
        maximum-pool-size: 5
    tenant:
      url: "jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/emme"
      hikari:
        minimum-idle: 2
        maximum-pool-size: 10

app:
  keycloak:
    provisioning:
      enabled: true
    admin-password: "${E2E_KEYCLOAK_ADMIN_PASSWORD:}"
```

### 4.2 Compose Changes

No changes needed for the compose files — both pools connect to the same PostgreSQL container. The `compose.environment-e2e.yaml` already references `emme-service:local`.

## 5. Implementation Order

1. **Dual connection pools** — wire `AbstractRoutingDataSource`, core + tenant HikariCP pools, `MultiTenantConnectionProvider`, `CurrentTenantIdentifierResolver`. Verify platform starts and login works.
2. **Drop AspectJ approach** — delete `TenantContextAspect`, `TenantProvisioningWorker`, `TenantProvisioningProcessManager`.
3. **Schema listener** — `TenantSchemaProvisioningListener` calls `LiquibaseTenantSchemaMigrationAdapter`. Publish `TenantSchemaReady`.
4. **Realm listener** — adapt `TenantCreatedConsumer` + `KeycloakRealmProvisioningProcessManager` into `TenantRealmProvisioningListener`. Listens to `TenantSchemaReady`. Publish `TenantRealmReady`.
5. **Activation listener** — `TenantActivationListener`. Marks ACTIVE. Publishes `TenantActivated` to Kafka.
6. **E2E provisioner** — synchronous realm-per-tenant provisioning.
7. **Kafka contract tests** — update for `TenantActivated`.
8. **E2E test suite** — run full real suite with realm-per-tenant.

## 6. Idempotency & Failure Modes

### Schema provisioning
- `CREATE SCHEMA IF NOT EXISTS` — safe to re-run.
- Liquibase `update` — changelog tracks applied changesets per schema via `databasechangelog` table (one per tenant schema). Safe to re-run.
- `UPDATE WHERE schema_status != 'READY'` — no-op if already done.

### Realm provisioning
- GET realm → if exists and matches expected config → skip create. If exists but partial → delete + recreate.
- Keycloak Admin API returns 409 Conflict on duplicate realm — caught, treated as success.
- `UPDATE WHERE keycloak_realm IS NULL` — no-op if already set.

### Outbox retry
- `spring.modulith.events.republish-outstanding-events-on-restart: true` — any event stuck in `event_publication` with `completion_date IS NULL` gets retried on next platform restart.
- Each listener is annotated `@Transactional` — event marked complete only after full success.

## 7. Design Decisions & Rationale

### Why dual pools instead of single pool + search_path?
Hibernate's `default_schema` generates fully-qualified SQL (`public.business_profile`). The `SET LOCAL search_path` AspectJ approach is bypassed because PostgreSQL search_path only resolves unqualified table names. Dual pools give Hibernate explicit schema routing via `connection.setSchema()` which supersedes `default_schema`.

### Why event chain instead of ShedLock polling?
ShedLock creates a polling interval where a newly created tenant is in limbo (not yet provisioned). Event chain is immediate: each step fires as soon as the previous completes. No polling delay. No cron configuration. Every listener is independently testable without scheduling infrastructure.

### Why no two-phase commit?
Each step is idempotent and self-contained. A failure in step 2 doesn't need to undo step 1 — step 1's work (a schema with tables) is harmless and reusable. The outbox pattern via `event_publication` guarantees each event is delivered at-least-once.

### Why `keycloak_realm=null` on initial tenant creation?
The realm doesn't exist yet — it's created asynchronously during provisioning. `null` signals "not provisioned" to the gateway/auth layer. After `TenantRealmReady`, the field is updated.

## 8. Verification

### Unit tests (fast feedback, no infra)
- `TenantSchemaProvisioningListenerTest` — mocks `LiquibaseTenantSchemaMigrationAdapter`, verifies event publication
- `TenantRealmProvisioningListenerTest` — mocks `IdentityProviderAdministrationPort`, verifies realm creation + event
- `TenantActivationListenerTest` — mocks `TenantProvisioningRepository`, verifies status transition
- `CurrentTenantIdentifierResolverTest` — verifies schema name resolution from `TenantContext`
- `DataSourceRoutingConfigurationTest` — verifies correct pool selection per lookup key

### Integration tests (real DB, containerized)
- `SchemaProvisioningIntTest` — `@MicronautTest` (or `@SpringBootTest`) with testcontainers PostgreSQL, verifies full schema create + Liquibase run
- `MultiTenantConnectionProviderIntTest` — verifies connection.setSchema() routes to correct tenant schema
- `TenantProvisioningChainIntTest` — full chain: event → schema → realm → activated, with test Keycloak container

### E2E tests (Playwright, full stack)
- Provision → login via `emme-e2e-studio` realm → query services/customers → verify isolation
- Multi-tenant: provision two tenants → verify each sees only its own data
- Failure recovery: kill Keycloak mid-provisioning → restart → verify retry succeeds
