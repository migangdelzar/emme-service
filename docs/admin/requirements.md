# EMME Admin Requirements Catalog

| Field | Value |
|---|---|
| Source | `docs/vision.md`, `docs/entity_model.md` |
| Audience | Platform administrators |
| Scope | v1 Spring Modulith — platform governance app |
| Date | 2026-08-04 |

## Functional Requirements

### Tenant Lifecycle

| ID | Title | User Story | Priority | Status |
|---|---|---|---|---|
| FR-A001 | Create tenant | As a platform administrator, I want to create a tenant so that a new salon can use EMME. | High | Implemented |
| FR-A002 | View tenant | As a platform administrator, I want to view tenant configuration and status so that I can diagnose lifecycle issues. | High | Implemented |
| FR-A003 | List tenants | As a platform administrator, I want to filter and list tenants by status so that I can operate the platform portfolio. | High | Implemented |
| FR-A004 | Update tenant | As a platform administrator, I want to update tenant domain, subscription, limits, and configuration so that tenant operation reflects its agreement. | High | Implemented |
| FR-A005 | Suspend tenant | As a platform administrator, I want to suspend a tenant while preserving its data so that its traffic is blocked safely. | High | Implemented |
| FR-A006 | Reactivate tenant | As a platform administrator, I want to reactivate a suspended tenant so that its authorized traffic resumes. | High | Implemented |
| FR-A007 | Stage tenant deletion | As a platform administrator, I want to stage tenant deletion with an audit hold so that one action cannot immediately destroy tenant data. | High | Implemented |
| FR-A008 | View tenant health | As a platform administrator, I want to view tenant integration and operational health so that failures can be corrected before the tenant notices. | Medium | Implemented |
| FR-A009 | Request tenant provisioning | As a platform administrator, I want to trigger tenant provisioning so that infrastructure is prepared before handover. | High | Implemented |
| FR-A010 | View provisioning status | As a platform administrator, I want to track provisioning progress per tenant so that I can confirm readiness. | High | Implemented |

### Feature Flags & Entitlements

| ID | Title | User Story | Priority | Status |
|---|---|---|---|---|
| FR-A011 | Manage feature flags | As a platform administrator, I want to define and toggle platform-wide feature flags so that capabilities can be released, gated, and sunset safely. | High | Implemented |
| FR-A012 | Override tenant features | As a platform administrator, I want to override a feature flag for a specific tenant so that selective rollout is possible without affecting other tenants. | High | Implemented |
| FR-A013 | View tenant effective features | As a platform administrator, I want to see which features are active for a tenant including overrides so that I can troubleshoot capability gaps. | High | Implemented |

### Membership & Access

| ID | Title | User Story | Priority | Status |
|---|---|---|---|---|
| FR-A014 | View memberships | As a platform administrator, I want to list all memberships across tenants so that I can audit who has access. | High | Implemented |
| FR-A015 | Assign membership | As a platform administrator, I want to assign a user to a tenant with a specific role so that they can access the tenant workspace. | High | Implemented |
| FR-A016 | Revoke membership | As a platform administrator, I want to revoke a user's membership so that their tenant access is removed immediately. | High | Implemented |
| FR-A017 | View user permissions | As a platform administrator, I want to see a user's effective permissions so that I can verify access matches their responsibilities. | High | Implemented |
| FR-A018 | Require administrator MFA | As a platform administrator, I want TOTP and recovery-code MFA enforced on my account so that privileged platform access is protected. | High | Implemented |

### Subscription Enforcement

| ID | Title | User Story | Priority | Status |
|---|---|---|---|---|
| FR-A019 | View subscription | As a platform administrator, I want to view any tenant's subscription, plan, and billing status so that I can verify entitlement. | High | Implemented |
| FR-A020 | Enforce entitlement | As a platform administrator, I want subscription limits enforced at use-case boundaries so that tenants receive only contracted capabilities. | High | Implemented |

### Audit & Observability

| ID | Title | User Story | Priority | Status |
|---|---|---|---|---|
| FR-A021 | Audit platform events | As a platform administrator, I want security-sensitive and consequential platform events recorded with tenant, actor, and request metadata so that activity is traceable. | High | Implemented |
| FR-A022 | Reconcile derived models | As a platform administrator, I want to detect and rebuild missing, stale, or corrupt tenant projections so that pgvector search remains recoverable. | High | Implemented |

## Non-Functional Requirements

| ID | Title | Requirement | Category | Priority | Status |
|---|---|---|---|---|---|
| NFR-A001 | Tenant isolation | Automated PostgreSQL integration tests must demonstrate 0 cross-tenant rows returned for every tenant-owned repository. | Security | High | Implemented |
| NFR-A002 | Trusted tenant source | 100% of protected requests must derive tenant context from a trusted host, provider binding, or validated identity context; client-supplied tenant identifiers must not grant access. | Security | High | Implemented |
| NFR-A003 | Authorization coverage | 100% of protected commands and queries must enforce an explicit permission before accessing tenant data. | Security | High | Implemented |
| NFR-A004 | Administrator MFA | 100% of platform-administrator interactive logins must require TOTP and provide recovery codes. | Security | High | Implemented |
| NFR-A005 | Secret leakage | Automated secret scanning must report 0 plaintext production credentials in committed files and built artifacts. | Security | High | Implemented |
| NFR-A006 | Sensitive logging | Automated log tests must detect 0 raw access tokens, credentials, payment secrets, or unredacted message bodies in standard production logs. | Security | High | Open |
| NFR-A007 | Encryption in transit | Production external and internal network traffic must use TLS 1.3 or the strongest version supported by the managed endpoint. | Security | High | Open |
| NFR-A008 | Module boundaries | Every CI run must execute Spring Modulith verification and ArchUnit rules with 0 module cycles or forbidden internal-package dependencies. | Maintainability | High | Implemented |
| NFR-A009 | Test gates | CI must run unit, H2 smoke, PostgreSQL Testcontainers, and boundary tests with zero failed or skipped required tests. | Maintainability | High | Implemented |
| NFR-A010 | Production DB fidelity | Every RLS, pgvector, PostgreSQL locking, Liquibase, and tenant-context behavior must have at least 1 passing PostgreSQL Testcontainers test. | Maintainability | High | Implemented |
| NFR-A011 | Dependency reproducibility | A clean CI environment must build 100% of artifacts from lockfiles without unpinned dynamic application dependencies. | Maintainability | High | Implemented |
| NFR-A012 | Observability context | 100% of server request logs and traces must include request ID; tenant IDs must be included when applicable and safe. | Maintainability | High | Open |
| NFR-A013 | Health reporting | Kubernetes liveness and readiness endpoints must reflect application and required-dependency health within 30 seconds of a state change. | Availability | High | Open |
| NFR-A014 | Outbox backlog | An alert must fire within 5 minutes when incomplete Modulith event publications grow continuously or the oldest incomplete publication exceeds 5 minutes. | Reliability | High | Open |
| NFR-A015 | Database diagnosis | PostgreSQL `pg_stat_statements` must be enabled in production and queries exceeding 500 ms must be observable. | Maintainability | Medium | Open |
| NFR-A016 | Recovery point | PostgreSQL backup and WAL retention must provide an RPO of 15 minutes or less. | Availability | High | Open |
| NFR-A017 | Recovery time | A documented restore exercise must restore the production-sized baseline dataset within 60 minutes before launch and at least quarterly afterward. | Availability | High | Open |
| NFR-A018 | Graceful shutdown | During a rolling deployment, the application must stop accepting new traffic before termination and allow up to 30 seconds for in-flight requests to finish. | Availability | High | Open |
| NFR-A019 | Subscription enforcement | 100% of tenant subscription limits must be enforced at use-case boundaries before business operations execute. | Security | High | Implemented |
| NFR-A020 | Projection freshness | Under normal provider operation, 95% of committed projection-relevant changes must become queryable in pgvector within 60 seconds. | Performance | Medium | Open |

## Constraints

| ID | Title | Constraint | Category | Priority | Status |
|---|---|---|---|---|---|
| C-A001 | Backend runtime | Backend application code must use Java 25. | Technical | High | Implemented |
| C-A002 | Build language | Gradle build scripts and convention plugins must use Kotlin DSL. | Technical | High | Implemented |
| C-A003 | Application framework | The backend must use Spring Boot 4.1 and Spring Modulith 2.1. | Technical | High | Implemented |
| C-A004 | Deployment unit | v1 backend capabilities must run as one Spring Boot deployment and one application image. | Technical | High | Implemented |
| C-A005 | Durable database | PostgreSQL 17 with pgvector must be the durable source of truth. | Technical | High | Implemented |
| C-A006 | Tenant storage | v1 tenant-owned tables must use shared-schema `tenant_id` isolation enforced by PostgreSQL RLS. | Technical | High | Implemented |
| C-A007 | Database migrations | Liquibase SQL-formatted changelogs with a YAML master must own all application schema changes. | Technical | High | Implemented |
| C-A008 | Cache boundary | Redis 7.4 must store only expiring cache, lock, rate-limit, idempotency, OAuth-state, and conversation-summary data. | Technical | High | Implemented |
| C-A009 | Identity provider | Keycloak 26 must issue tokens; the backend must operate as an OAuth2 resource server and must not implement password authentication. | Technical | High | Implemented |
| C-A010 | Internal communication | v1 modules must communicate through Java APIs and Modulith events, not internal REST, gRPC, or protobuf. | Technical | High | Implemented |
| C-A011 | Event durability | Cross-module event publication must use the Spring Modulith JDBC event registry and retain incomplete publications for retry and recovery. | Technical | High | Implemented |
| C-A012 | Kafka event streaming | Stable public `api.event` contracts selected for externalization must be published to Kafka with explicit topics and tenant partition keys. | Technical | High | Implemented |
| C-A013 | Web model | HTTP APIs must use Spring Web MVC with virtual threads; WebFlux must not be introduced in v1. | Technical | High | Implemented |
| C-A014 | Persistence API | Durable relational persistence must use Spring Data JPA/Hibernate unless a PostgreSQL-specific query requires an explicit JDBC/native adapter. | Technical | High | Implemented |
| C-A015 | Scheduling | In-process scheduled work must use Spring scheduling and ShedLock JDBC coordination. | Technical | Medium | Implemented |
| C-A016 | API documentation | Externally callable HTTP endpoints must be documented using springdoc OpenAPI. | Technical | Medium | Open |
| C-A017 | Domain modeling | Core business entities must use explicit relational models; generic catch-all business-object tables are prohibited. | Technical | High | Implemented |
| C-A018 | Module purity | Domain packages must not import Spring, JPA, Redis, transport, provider SDK, or AI framework types. | Technical | High | Implemented |
| C-A019 | Dependency direction | Modules must expose explicit public APIs, prohibit repository/entity access across module boundaries, and contain no cyclic dependencies. | Technical | High | Implemented |
| C-A020 | Public application API | v1 application APIs must use REST over Spring Web MVC with OpenAPI contracts; GraphQL and Apollo runtime dependencies must not be required. | Technical | High | Implemented |
| C-A021 | Backend tests | Backend testing must use JUnit, H2 for portable smoke tests, and PostgreSQL Testcontainers for production-specific behavior. | Technical | High | Implemented |
| C-A022 | Boundary tests | Spring Modulith verification must validate application module boundaries in CI. | Technical | High | Implemented |
| C-A023 | CI/CD | GitHub Actions must test, build one application image, publish it to GHCR, and deploy the selected Kustomize overlay. | Operational | High | Implemented |
| C-A024 | Container image | The backend image must be built with Spring Boot Buildpacks or Jib and must not require an application Dockerfile. | Technical | Medium | Implemented |
| C-A025 | Secrets | Repository secrets must use SOPS with age or Sealed Secrets; plaintext production secrets are prohibited. | Security | High | Open |
| C-A026 | Observability | Production signals must use OpenTelemetry with Grafana, Loki, Prometheus, Tempo, and Alloy or the approved Grafana Cloud equivalent. | Operational | High | Open |
| C-A027 | Local infrastructure | Daily local development must use k3d against the same Kubernetes manifests used by production. | Operational | High | Open |
| C-A028 | Kubernetes packaging | EMME workloads must use Kustomize base plus development and production overlays; Helm is limited to third-party charts. | Operational | High | Implemented |
| C-A029 | Kubernetes database | Production PostgreSQL must use CloudNativePG rather than a raw PostgreSQL Deployment and PVC. | Operational | High | Open |
| C-A030 | Kubernetes ingress | Kubernetes ingress must use the selected Traefik or ingress-nginx controller with cert-manager-managed TLS. | Operational | High | Open |
| C-A031 | Infrastructure provisioning | Terraform must provision shared cloud, cluster, DNS, networking, and backup infrastructure without deploying application workloads. | Operational | Medium | Open |
| C-A032 | Performance tests | Locust must own HTTP workload and performance regression scenarios. | Technical | Medium | Open |
| C-A033 | Excluded frameworks | Quarkus and Micronaut dependencies and runtime configuration must not be introduced. | Technical | High | Implemented |
| C-A034 | Excluded service runtimes | Python AI gateway and TypeScript AI bridge behavior must be translated into Java Spring AI modules rather than retained as deployables. | Technical | High | Implemented |
| C-A035 | Excluded technologies | Spring Batch, GraalVM native images, WebFlux, service mesh, n8n, and Argo CD must not be required by v1. | Technical | High | Implemented |
| C-A036 | Schema-per-tenant exclusion | Schema-per-tenant and dedicated-database modes are superseded for v1 by the shared-schema RLS constraint. | Technical | High | Implemented |
| C-A037 | Graph projection | Apache AGE must be a tenant-isolated, derived, disposable, and rebuildable read model rather than an authoritative data store. | Technical | High | Implemented |
| C-A038 | Projection mechanism | pgvector and Apache AGE projections must be populated after commit through durable Spring Modulith events with idempotent projectors, retries, and reconciliation. | Technical | High | Implemented |
