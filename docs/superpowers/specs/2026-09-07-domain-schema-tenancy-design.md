# Domain-Schema Tenancy and DDD Architecture Design

| Field | Value |
|---|---|
| Product | Emme Service |
| Repository | `emme-service` |
| Date | 2026-09-07 |
| Status | Approved for handoff-plan review |
| Scope | Platform-wide DDD boundaries, domain schemas, tenant isolation, and future sharding readiness |

## 1. Decision summary

Emme will use bounded contexts as the primary ownership boundary. Each bounded
context will own a PostgreSQL schema in the initial modular-monolith
deployment. Tenant-owned rows in those schemas will carry `tenant_id` and will
be protected by PostgreSQL Row-Level Security (RLS).

The initial physical topology is one PostgreSQL database with a dedicated
`platform` schema and one schema per domain. The design does not create a
database per tenant or shard the database before scale and workload evidence
justify it.

The architecture remains ready for two later moves:

1. Extract one bounded context into a service with its domain schema as its
   initial database.
2. Distribute high-volume tenant-owned tables by `tenant_id`, or place selected
   tenants/databases on dedicated infrastructure.

Domain schemas, tenant isolation, and sharding are separate concerns:

```text
DDD ownership       -> bounded context + schema + API/event contract
Tenant isolation    -> tenant_id + PostgreSQL RLS
ORM convenience     -> Hibernate tenant support where useful
Horizontal scale    -> tenant-keyed partitioning/sharding later
```

PostgreSQL schemas organize objects but do not provide rigid database-level
separation; code and privileges must still enforce ownership. PostgreSQL RLS
provides a database-side row filter and defaults to deny when enabled without
an applicable policy. Hibernate's `@TenantId` is discriminator-based ORM
support and must not be treated as the security boundary.

## 2. Goals

- Define platform-wide bounded-context ownership.
- Identify candidate aggregate roots, child entities, value objects, and
  domain services.
- Keep domain code independent from JPA, PostgreSQL, Spring, and Hibernate.
- Make every tenant-owned persistence model explicit about `tenant_id`.
- Fail closed when tenant context is missing or invalid.
- Remove cross-domain database coupling that would prevent service extraction.
- Preserve a reversible migration path from the current schema-per-tenant
  implementation.
- Keep `tenant_id` usable as a future distribution key for sharding.
- Preserve existing business behavior while the persistence topology changes.

## 3. Non-goals

- Implementing the migration in this design document.
- Selecting a sharding product or operating a distributed PostgreSQL cluster
  immediately.
- Splitting the modular monolith into deployable services as part of the first
  migration.
- Replacing domain events with cross-schema SQL joins.
- Making Hibernate responsible for authorization or tenant security.
- Inventing new product behavior not represented by the existing modules and
  use cases.

## 4. Current-state constraints

The repository is a Java 25 Spring Modulith modular monolith. It already has:

- Module-owned domain models under `modules/*/domain/model`.
- Application ports and adapters around persistence.
- A shared `TenantOwnedEntity` persistence base type with `tenant_id`.
- A tenancy module with tenant context, tenant registry, schema provisioning,
  database placement, and Hibernate schema multi-tenancy infrastructure.
- Domain and integration tests for tenant isolation and provisioning.
- Durable application events and Kafka externalization patterns.

The current persistence setup includes schema-per-tenant configuration and
tenant database-pool abstractions. The migration must therefore use an
expand/contract approach and cannot assume that all tenants can be moved in a
single transaction or deployment.

## 5. Bounded-context and schema ownership

The following is the target ownership map. Names are logical schema names and
may be adjusted only through a documented architecture decision.

| Bounded context | Initial schema | Owns | Does not own |
|---|---|---|---|
| Platform tenancy | `platform` | tenant lifecycle, tenant placement, provisioning state, platform event publication | business aggregates |
| Identity and access | `identity` | users, memberships, roles, permissions, customer identity links, feature flags | tenant provisioning |
| Studio configuration | `studio` | business profile, operating hours, booking policy, notification preferences | appointments and catalog offerings |
| Service catalog | `services` | services, artists, artist capabilities | bookings, payments |
| Clients | `clients` | customer profiles and customer status | authentication identity and appointments |
| Appointments | `appointments` | appointments, holds, availability decisions | payment settlement and calendar provider state |
| Calendar | `calendar` | provider connections, sync state, external event links | appointment ownership |
| Documents and knowledge | `documents` | documents, document chunks, ingestion state, document search metadata | conversations and authorization policy |
| Conversations and assistant | `assistant` | conversations, conversation events, pending actions, channel participants | authoritative domain mutations |
| Subscriptions | `subscriptions` | tenant subscription and entitlement state | payment provider transaction details |
| Payments | `payments` | payment intents/transactions and provider references | subscription policy and appointment policy |
| Notifications | `notifications` | notification delivery records and status | channel credentials and business policy |
| Audit | `audit` | append-only audit records and outcome metadata | mutable business state |

The `platform` schema is the only shared metadata area. A domain may consume
another domain's public API or event contract, but it must not read its tables
through a repository or create an ORM relationship to its entities.

## 6. DDD model catalog

This catalog is the initial best-fit model to validate during implementation.
An aggregate boundary is justified by transactional invariants and command
ownership, not by the number of tables. A persistence entity is not
automatically a domain entity, and a database table is not automatically an
aggregate.

### 6.1 Platform tenancy

**Aggregate root: `Tenant`**

- Entities: tenant lifecycle record and provisioning state history when the
  history is required for operational recovery.
- Value objects: `TenantId`, `TenantSlug`, `TenantName`, `TenantStatus`,
  `TenantProvisioningState`, `TenantPlacement`.
- Invariants: slug is valid and unique; lifecycle transitions are explicit;
  a tenant cannot be activated before required infrastructure is ready.
- Domain events: `TenantCreated`, `TenantActivated`, `TenantSuspended`,
  `TenantProvisioningFailed`.

Database registry and placement are infrastructure-facing records owned by
the tenancy context. They are not embedded in every business aggregate.

### 6.2 Identity and access

**Aggregate roots: `UserAccount`, `Membership`**

- Entities: role and permission assignments, customer identity links, feature
  flag assignments where assignment changes have their own lifecycle.
- Value objects: `UserId`, `MembershipId`, `RoleName`, `PermissionCode`,
  `ExternalIdentity`, `RealmName`.
- Invariants: membership belongs to one tenant; permissions are resolved from
  identity contracts; a revoked membership cannot authorize tenant commands.
- Cross-context rule: identity supplies an authenticated actor and tenant
  context; business contexts do not query identity tables directly.

`Role` and `Permission` may remain reference-like entities if their lifecycle
is platform-controlled. They should not become a shared domain model imported
by every context.

### 6.3 Studio configuration

**Aggregate root: `BusinessProfile`**

- Child entities/value objects: `OperatingHours`, `BookingPolicy`, and
  notification preferences.
- Value objects: `BusinessName`, `ContactDetails`, `Address`, `TimeRange`,
  `WeeklySchedule`, `BookingPolicyRules`.
- Invariants: one active profile per tenant; schedules are internally
  consistent; booking policy values are valid together.
- Domain events: `BusinessProfileUpdated`, `BookingPolicyChanged`.

Configuration changes are kept together when they must be validated as one
business configuration. They are not coupled to appointment transactions.

### 6.4 Service catalog

**Aggregate roots: `ServiceOffering`, `Artist`**

- Entity: `ArtistCapability` is an association owned by the catalog context.
- Value objects: `ServiceCode`, `ServiceName`, `Duration`, `Money`,
  `ArtistName`, `CapabilityStatus`.
- Invariants: service code is unique within a tenant; duration and price are
  valid; retired services cannot be newly assigned or booked; capabilities
  reference catalog-owned IDs within the same tenant.
- Domain events: `ServiceOfferingCreated`, `ServiceOfferingUpdated`,
  `ServiceOfferingRetired`, `ArtistCapabilityChanged`.

The appointment context consumes service and artist snapshots or contract
references. It does not hold JPA relationships to catalog entities.

### 6.5 Clients

**Aggregate root: `Customer`**

- Value objects: `CustomerId`, `CustomerName`, `EmailAddress`, `PhoneNumber`,
  `CustomerNotes`.
- Invariants: customer status transitions are explicit; customer data is
  tenant-owned; external identity links are handled by identity, not clients.
- Domain events: `CustomerCreated`, `CustomerUpdated`, `CustomerDeactivated`.

### 6.6 Appointments

**Aggregate root: `Appointment`**

- Entity: `AppointmentHold` is a separate aggregate if it has an independent
  expiration/release lifecycle; otherwise it remains an appointment child.
- Value objects: `AppointmentId`, `AppointmentTimeRange`, `AppointmentStatus`,
  `TenantResourceReference`, `CancellationReason`.
- Invariants: start precedes end; referenced tenant resources belong to the
  same tenant; conflicting bookings are rejected; state transitions are
  explicit and idempotent.
- Domain events: `AppointmentBooked`, `AppointmentRescheduled`,
  `AppointmentCancelled`, `AppointmentHoldReleased`.

Availability and collision detection are domain/application policies over
  appointment data and published resource contracts. They must not depend on
  cross-schema joins.

### 6.7 Calendar

**Aggregate roots: `CalendarConnection`, `CalendarSyncState`**

- Entity: `CalendarEventLink` belongs to the sync aggregate when link updates
  are part of one synchronization transaction.
- Value objects: `CalendarAccountReference`, `ExternalEventId`,
  `CalendarProvider`, `SyncCursor`.
- Invariants: provider credentials and external references are tenant-scoped;
  sync status transitions are idempotent.

### 6.8 Documents and knowledge

**Aggregate root: `Document`**

- Child entity: `DocumentChunk` belongs to a document and cannot outlive its
  document in the domain model.
- Value objects: `DocumentId`, `DocumentName`, `SourceType`,
  `ContentFingerprint`, `ChunkIndex`, `EmbeddingReference`.
- Invariants: chunk indexes are unique within a document; ingestion version
  and status transitions are explicit; search projections are rebuildable.
- Domain events: `DocumentUploaded`, `DocumentIngestionCompleted`,
  `DocumentRemoved`.

Embeddings and search indexes are projections/adapters, not additional
authoritative aggregates.

### 6.9 Conversations and assistant

**Aggregate roots: `Conversation`, `PendingAction`**

- Child entity: `ConversationEvent` belongs to a conversation and is append
  only.
- Entity/value object: `ChannelParticipant` is tenant-scoped channel identity
  data; it may be an aggregate if its lifecycle is independently managed.
- Value objects: `ConversationId`, `ConversationChannel`, `MessageId`,
  `ActionType`, `ConsentStatus`, `ActionExpiry`.
- Invariants: conversation events are ordered and append-only; a pending
  action is single-use, tenant-scoped, and expires deterministically; the
  assistant may propose commands but cannot bypass domain authorization.

### 6.10 Subscriptions, payments, notifications, and audit

- `Subscription` is the subscriptions aggregate root. Value objects include
  `PlanCode`, `Entitlement`, `BillingPeriod`, and `SubscriptionStatus`.
- `Payment` is the payments aggregate root. Value objects include
  `PaymentId`, `PaymentAmount`, `Currency`, `ProviderReference`, and
  `PaymentStatus`.
- `Notification` is the notifications aggregate root. Value objects include
  `NotificationId`, `NotificationChannel`, `DeliveryAttempt`, and
  `NotificationStatus`.
- `AuditRecord` is an append-only audit entity owned by the audit context. It
  is created from domain/application events and is not used to enforce live
  business invariants.

These contexts communicate through explicit events such as subscription
activation, payment completion, appointment changes, and notification
requests. They do not share mutable aggregate objects.

## 7. Tenant identity and persistence contract

Every tenant-owned persistence table must satisfy:

```sql
tenant_id uuid not null
```

The implementation must also apply:

- `tenant_id` as the leading column in tenant-scoped unique constraints and
  high-frequency indexes.
- Composite foreign keys within a bounded context when needed to prevent
  accidentally linking rows from different tenants.
- No cross-domain foreign keys. Cross-domain references are UUIDs validated
  through APIs, events, or an application port.
- Stable globally unique IDs so records can move between schemas/databases
  without identifier rewriting.
- Explicit classification of global/reference rows versus tenant-owned rows.

The tenant context is resolved at the inbound boundary from trusted
authentication and is propagated through application calls. A client-supplied
tenant identifier is never authoritative by itself.

## 8. RLS enforcement model

RLS is the authoritative database-side control for shared domain schemas.
Application connections use a role that does not own tenant tables and does
not have `BYPASSRLS`. Migration connections use a separate privileged role.
Tenant tables are enabled and forced under RLS.

The policy pattern is conceptually:

```sql
alter table services.service_offering enable row level security;
alter table services.service_offering force row level security;

create policy service_offering_tenant_isolation
on services.service_offering
using (tenant_id = nullif(current_setting('app.tenant_id', true), '')::uuid)
with check (tenant_id = nullif(current_setting('app.tenant_id', true), '')::uuid);
```

The actual migration must use a safe, reusable policy function where that
reduces duplication. Missing tenant context must produce no visible tenant
rows and must reject writes. The connection adapter must set the tenant
setting transaction-locally and clear/reset it when the connection returns to
the pool.

RLS coverage must include Hibernate, native JDBC, scheduled jobs, event
listeners, background workers, and search SQL. Administrative operations that
intentionally cross tenants require a separate explicitly authorized path and
must not reuse an ordinary tenant-scoped connection.

## 9. Hibernate role

The target persistence strategy is shared tables inside domain schemas, not
schema-per-tenant Hibernate multi-tenancy. The current
`MultiTenantConnectionProvider`, schema resolver, and tenant schema pool
behavior therefore become migration targets rather than the final isolation
mechanism.

Hibernate's discriminator support may be used as a convenience layer:

- Annotate tenant-owned ORM fields with `@TenantId` only after verifying the
  repository's Hibernate version and behavior in unit/integration tests.
- Use it to prevent accidental tenant-id mutation and reduce repetitive ORM
  predicates where appropriate.
- Do not rely on it for native SQL, JDBC, reporting, search, migrations, or
  connections used outside Hibernate.
- Keep tenant context validation and RLS active even when `@TenantId` is used.
- Keep domain models free of `@TenantId`; the annotation belongs in the
  persistence adapter/entity layer.

If a Hibernate version or Spring integration prevents reliable use of
`@TenantId`, the fallback is an explicit persistence tenant field plus RLS;
the database security contract does not change.

## 10. Sharding and extraction readiness

Sharding is deferred until measurements show that one PostgreSQL node or one
domain's working set is a constraint. When enabled, `tenant_id` is the default
distribution key for tenant-centric workloads, and related tables must use a
compatible key so joins remain co-located.

The model must avoid assumptions that prevent distribution:

- Include `tenant_id` in relevant primary/unique keys.
- Keep queries tenant-scoped and make tenant predicates explicit.
- Keep cross-domain reporting asynchronous or projection-based.
- Avoid cross-domain transactions and foreign keys.
- Keep large append-only tables candidates for time partitioning in addition to
  tenant distribution.

Before extracting a domain, its schema must have:

- One owning module and migration history.
- No inbound table-level dependencies from other domains.
- Public input/output contracts and versioned events.
- A rebuildable read-model strategy for consumers.
- Independent backup, restore, migration, and health-check procedures.

## 11. Migration strategy

The implementation plan must use these reversible phases:

1. **Inventory and contract freeze.** Catalogue current tables, migrations,
   repositories, queries, events, and cross-domain joins. Record the source
   and target owner of every table.
2. **DDD model artifacts.** Validate the bounded-context map, aggregate
   invariants, value objects, commands, events, and public contracts against
   existing use cases and tests.
3. **Expand schema foundation.** Create domain schemas and shared migration
   tooling. Add target tables with `tenant_id`, indexes, constraints, and
   RLS disabled only for controlled backfill operations.
4. **Backfill and reconcile.** Copy data from tenant schemas into target domain
   schemas with deterministic tenant mapping. Run row counts, checksums,
   referential consistency checks within each domain, and replay-safe retries.
5. **Dual-read/dual-write transition.** Introduce ports/adapters that can
   compare old and new stores. Enable target writes behind a controlled
   rollout, then make target reads authoritative after reconciliation.
6. **Enable RLS and switch connections.** Set transaction-local tenant context,
   validate fail-closed behavior, and switch Hibernate/JDBC adapters to the
   domain schemas.
7. **Contract verification.** Run module, integration, security, migration,
   and end-to-end tests for two or more tenants, missing context, background
   work, and administrative flows.
8. **Contract and cleanup.** Remove old schema-per-tenant paths only after a
   defined observation window, backup verification, and rollback checkpoint.

Each phase must have a rollback action. No destructive source-schema removal
belongs in the initial migration.

## 12. Verification strategy

### Domain tests

- Aggregate tests cover valid commands, invariant violations, and lifecycle
  transitions.
- Value-object tests cover normalization, validation, equality, and boundary
  values.
- Domain tests import no Spring, JPA, Hibernate, or database classes.

### Persistence and security tests

- Verify every tenant-owned entity persists with the correct `tenant_id`.
- Verify tenant A cannot select, update, delete, or insert into tenant B's
  rows through Hibernate, native JDBC, and repository paths.
- Verify missing or invalid tenant context fails closed.
- Verify pooled connections do not leak tenant settings across requests.
- Verify unique constraints and references cannot cross tenant boundaries.
- Verify privileged migration/admin paths are explicit and audited.

### Migration tests

- Run migrations from an empty database and from a representative legacy
  schema-per-tenant database.
- Validate counts, checksums, IDs, timestamps, status values, and event
  replay behavior.
- Exercise rollback checkpoints without deleting source data.

### Extraction-readiness tests

- Architecture tests reject cross-domain persistence imports and ORM
  relationships.
- Contract tests verify public APIs/events without loading another domain's
  persistence classes.
- Query tests identify cross-tenant scans and missing tenant predicates.

## 13. Requirement-to-design mapping

| User concern | Design response |
|---|---|
| Schema per domain for future microservices | One schema per bounded context, explicit ownership, no cross-schema persistence coupling |
| Multiple databases versus sharding | One database initially; future domain extraction or tenant-keyed sharding are independent placement options |
| Tenant ID in databases | `tenant_id` on every tenant-owned table, with composite indexes/constraints |
| Hibernate `@TenantId` versus RLS | Use both as layered controls where supported; RLS is authoritative and fail-closed |
| DDD entities and aggregates | Context-specific aggregate catalog based on transactional invariants |
| Value objects | Typed IDs, money, time ranges, codes, statuses, provider references, and policy types at domain boundaries |
| Migration risk | Expand/contract, reconciliation, dual-read/write transition, rollback checkpoints, delayed cleanup |

## 14. Risks and mitigations

| Risk | Mitigation |
|---|---|
| RLS bypass by table owner or privileged role | Separate application/migration roles; `FORCE ROW LEVEL SECURITY`; integration tests |
| Tenant setting leaks through pool reuse | Transaction-local setting plus reset-on-release tests |
| Cross-domain joins reappear in reporting | Build event-fed projections or explicit query APIs |
| Aggregate boundaries reflect tables instead of invariants | Validate every boundary against commands and transactional rules |
| Sharding key creates poor joins or hotspots | Keep `tenant_id` explicit and measure tenant skew before distribution |
| Backfill creates duplicate or inconsistent rows | Idempotent migration keys, checksums, reconciliation, and source retention |
| Hibernate behavior differs across versions | Pin and test the repository version; retain RLS independent of ORM behavior |

## 15. Definition of design completion

- Every current module has one declared data owner.
- Every persistence table is classified as platform, global reference, or
  tenant-owned.
- Every tenant-owned table has a `tenant_id` and RLS policy specification.
- Aggregate roots and value objects have documented invariants and commands.
- Cross-domain references have API/event contracts instead of table joins.
- Migration phases have verification and rollback checkpoints.
- The next session can execute the implementation plan without deciding the
  tenancy strategy again.
