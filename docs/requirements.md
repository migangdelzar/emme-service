# EMME Modulith Requirements Catalog

| Field | Value |
|---|---|
| Source | `docs/vision.md`, `EMME-STACK.md`, and selected behavior from `~/Development/emme-platform` |
| Scope | First Spring Modulith version |
| Status | Draft for review |
| Date | 2026-07-03 |

`EMME-STACK.md` is authoritative for technical scope. Existing platform behavior is retained where it does not conflict with that stack or the approved Modulith direction.

## Functional Requirements

| ID | Title | User Story | Priority | Status |
|---|---|---|---|---|
| FR-001 | Sign in | As a platform user, I want to authenticate through Keycloak so that I can access authorized EMME capabilities. | High | Open |
| FR-002 | Sign out | As a platform user, I want to end my authenticated session so that access from my device is revoked. | High | Open |
| FR-003 | Select tenant | As a user with multiple memberships, I want to select an active tenant so that my actions apply to the intended business. | High | Open |
| FR-004 | Resolve tenant from host | As a tenant user, I want EMME to resolve my tenant from the approved subdomain so that I enter the correct workspace. | High | Open |
| FR-005 | View current identity | As a platform user, I want to view my profile, memberships, role, and permissions so that I understand my current access. | High | Open |
| FR-006 | Enforce permissions | As a tenant owner, I want every protected action checked against database-driven permissions so that staff access matches assigned responsibilities. | High | Open |
| FR-007 | Require administrator MFA | As a platform administrator, I want TOTP and recovery-code MFA enforced so that privileged platform access is protected. | High | Open |
| FR-008 | Create tenant | As a platform administrator, I want to create a Nails tenant so that a new salon can use EMME. | High | Open |
| FR-009 | Update tenant | As a platform administrator, I want to update tenant domain, subscription, limits, and configuration so that tenant operation reflects its agreement. | High | Open |
| FR-010 | List tenants | As a platform administrator, I want to filter and list tenants so that I can operate the platform portfolio. | High | Open |
| FR-011 | View tenant | As a platform administrator, I want to view tenant configuration and status so that I can diagnose lifecycle issues. | High | Open |
| FR-012 | Suspend tenant | As a platform administrator, I want to suspend a tenant while preserving its data so that its traffic is blocked safely. | High | Open |
| FR-013 | Reactivate tenant | As a platform administrator, I want to reactivate a suspended tenant so that its authorized traffic resumes. | High | Open |
| FR-014 | Stage tenant deletion | As a platform administrator, I want to stage tenant deletion with an audit hold so that one action cannot immediately destroy tenant data. | High | Open |
| FR-015 | View tenant health | As a platform administrator, I want to view tenant integration and operational health so that failures can be corrected. | Medium | Open |
| FR-016 | Manage entitlements | As a platform administrator, I want to enable or disable tenant entitlements so that capability access follows the tenant subscription. | High | Open |
| FR-017 | Complete onboarding | As a tenant owner, I want to configure initial business details, hours, services, and calendar connections so that the salon can begin operating. | High | Open |
| FR-018 | View dashboard | As a salon manager, I want to view daily appointments, revenue indicators, and operational summaries so that I can manage the day. | High | Open |
| FR-019 | View service catalog | As a salon user, I want to list and filter nail services so that I can find current offerings. | High | Open |
| FR-020 | Create service | As a salon manager, I want to create a nail service with duration, category, description, and price information so that it can be offered to customers. | High | Open |
| FR-021 | Update service | As a salon manager, I want to update a nail service so that the catalog remains accurate. | High | Open |
| FR-022 | Retire service | As a salon manager, I want to retire a nail service without corrupting appointment history so that it cannot be selected for new bookings. | High | Open |
| FR-023 | Manage artist capability | As a salon manager, I want to associate artists with services and specialties so that booking suggestions use qualified staff. | Medium | Open |
| FR-024 | View customers | As a staff member, I want to search and view tenant customers so that I can support salon operations. | High | Open |
| FR-025 | Create customer | As a staff member, I want to create a customer profile so that appointments and preferences can be associated with the customer. | High | Open |
| FR-026 | Update customer | As a staff member, I want to update contact details, notes, allergies, preferences, VIP status, and birthday so that service remains accurate and safe. | High | Open |
| FR-027 | Retire customer | As a manager, I want to retire a customer profile according to retention policy so that it is unavailable for new operations without losing required history. | Medium | Open |
| FR-028 | View customer history | As a staff member, I want to view a customer's visits and spending history so that I can provide informed service. | High | Open |
| FR-029 | View appointments | As a staff member, I want to view appointments by date and status so that I can manage the schedule. | High | Open |
| FR-030 | Search appointment slots | As a customer or staff member, I want to find available slots using salon hours, service duration, artist capability, and existing bookings so that I can choose a valid time. | High | Open |
| FR-031 | Create appointment | As a customer or staff member, I want to create an appointment for a valid customer, service, artist, and slot so that the booking is recorded. | High | Open |
| FR-032 | Prevent booking collision | As a salon owner, I want concurrent requests for the same slot serialized so that double booking cannot occur. | High | Open |
| FR-033 | Reschedule appointment | As a customer or staff member, I want to move an appointment to another valid slot so that schedule changes are supported. | High | Open |
| FR-034 | Cancel appointment | As a customer or staff member, I want to cancel an eligible appointment so that the slot is released and the lifecycle is recorded. | High | Open |
| FR-035 | Update appointment status | As a staff member, I want to confirm, start, complete, or mark an appointment as a no-show so that its operational state is accurate. | High | Open |
| FR-036 | View appointment detail | As a staff member, I want to view appointment, customer, service, artist, and status details so that I can fulfill the booking. | High | Open |
| FR-037 | Export appointments | As a salon manager, I want to export appointment data so that I can use it in approved external reporting tools. | Medium | Open |
| FR-038 | View financial summary | As a salon owner, I want to view revenue, average ticket, completed appointments, and period comparisons so that I can assess performance. | High | Open |
| FR-039 | Export finances | As a salon owner, I want to export financial summaries so that I can perform approved external analysis. | Medium | Open |
| FR-040 | Connect Google Calendar | As a salon owner, I want to authorize supported Google calendar integrations so that approved scheduling workflows can run. | Medium | Open |
| FR-041 | Manage business settings | As a salon owner, I want to configure business profile, working hours, booking rules, notifications, and channel behavior so that EMME follows salon policy. | High | Open |
| FR-042 | Receive WhatsApp message | As a customer, I want to send a message through the salon's Meta WhatsApp channel so that I can interact with EMME. | High | Open |
| FR-043 | Use web chat | As a customer, I want to use web chat with the same core capabilities as WhatsApp so that I can choose my preferred channel. | High | Open |
| FR-044 | Verify WhatsApp webhook | As a system operator, I want Meta webhook verification and signatures validated so that only authentic callbacks are processed. | High | Open |
| FR-045 | Normalize channel messages | As a conversation system, I want channel-specific payloads normalized into one tenant-scoped message contract so that orchestration is channel independent. | High | Open |
| FR-046 | Deduplicate inbound message | As a salon owner, I want retried webhooks deduplicated so that one customer message cannot trigger duplicate work. | High | Open |
| FR-047 | Transcribe voice input | As a customer, I want supported voice notes transcribed so that I can communicate without typing. | Medium | Open |
| FR-048 | Analyze image input | As a customer, I want nail-design images converted into structured style features so that EMME can recommend and estimate relevant services. | High | Open |
| FR-049 | Combine multimodal input | As a customer, I want text instructions to refine accompanying voice or image content so that my complete intent is respected. | High | Open |
| FR-050 | Detect conversation intent | As a customer, I want EMME to identify one or more intents from my message so that relevant tools can answer my request. | High | Open |
| FR-051 | Recommend services | As a customer, I want tenant-catalog service recommendations grounded in my request so that I can select an appropriate service. | High | Open |
| FR-052 | Estimate price | As a customer, I want a price estimate derived from the tenant's structured catalog so that the estimate reflects current salon pricing. | High | Open |
| FR-053 | Answer policy question | As a customer, I want answers grounded in the tenant's approved knowledge base so that salon policies are represented accurately. | High | Open |
| FR-054 | Draft conversational booking | As a customer, I want EMME to assemble a booking draft from conversation context so that I do not repeat known details. | High | Open |
| FR-055 | Confirm consequential action | As a customer, I want EMME to request explicit confirmation before booking, cancellation, or payment actions so that unintended changes are prevented. | High | Open |
| FR-056 | Resume pending confirmation | As a customer, I want a pending confirmation to survive application restart until its expiry so that a recoverable interruption does not lose my booking flow. | High | Open |
| FR-057 | Cancel expired confirmation | As a salon owner, I want unconfirmed actions to expire without changing business state so that stale requests cannot execute. | High | Open |
| FR-058 | Store conversation history | As a salon user with authorization, I want tenant-scoped conversation history retained according to policy so that customer interactions can be audited and resumed. | High | Open |
| FR-059 | Summarize conversation | As a conversation system, I want to maintain an expiring summary of long conversations so that AI context remains efficient. | Medium | Open |
| FR-060 | Handle AI provider failure | As a customer, I want an actionable fallback response when an AI provider fails so that unsafe or fabricated actions are not performed. | High | Open |
| FR-061 | Upload knowledge source | As a tenant manager, I want to upload an approved knowledge document so that tenant-specific information can be indexed. | High | Open |
| FR-062 | Convert knowledge source | As a tenant manager, I want supported documents converted into normalized text so that heterogeneous files can enter one ingestion pipeline. | High | Open |
| FR-063 | Index knowledge source | As a tenant manager, I want converted content chunked, embedded, and keyword-indexed so that it becomes retrievable. | High | Open |
| FR-064 | Track ingestion status | As a tenant manager, I want to view document ingestion status and errors so that failed sources can be corrected. | High | Open |
| FR-065 | Retrieve tenant knowledge | As an AI system, I want hybrid vector and keyword retrieval filtered by tenant before ranking so that responses use only authorized knowledge. | High | Open |
| FR-066 | Request notification | As a business module, I want to request a notification through a channel-neutral interface so that domain logic does not depend on delivery providers. | High | Open |
| FR-067 | Deliver WhatsApp response | As a customer, I want replies delivered through the salon's direct Meta WhatsApp integration so that the conversation completes in the originating channel. | High | Open |
| FR-068 | Send appointment reminder | As a salon owner, I want scheduled appointment reminders sent according to tenant settings so that missed appointments are reduced. | Medium | Open |
| FR-069 | Track notification outcome | As a staff member, I want delivery attempts and outcomes recorded so that communication failures can be diagnosed. | Medium | Open |
| FR-070 | View subscription | As a tenant owner, I want to view the current subscription, entitlements, limits, and billing status so that I understand available capabilities. | High | Open |
| FR-071 | Enforce subscription entitlement | As a platform administrator, I want subscription limits enforced at use-case boundaries so that tenants receive only contracted capabilities. | High | Open |
| FR-072 | Initiate payment | As a customer or tenant owner, I want to initiate an approved payment through the configured provider so that an eligible charge can be completed. | Medium | Open |
| FR-073 | Process payment callback | As a payment system, I want provider callbacks authenticated and idempotently applied so that payment state remains correct under retries. | High | Open |
| FR-074 | Refund payment | As a manager with authorization, I want to request an eligible refund so that failed or cancelled transactions can be corrected. | Medium | Open |
| FR-075 | Audit business event | As a platform administrator, I want security-sensitive and consequential business events recorded with tenant, actor, request, and timestamp metadata so that activity is traceable. | High | Open |
| FR-076 | Project semantic change | As a retrieval system, I want committed domain changes projected idempotently into pgvector and Apache AGE so that derived search models reflect authoritative business data. | High | Open |
| FR-077 | Reconcile derived models | As a system operator, I want to detect and rebuild missing, stale, or corrupt tenant projections so that pgvector and Apache AGE remain recoverable without becoming sources of truth. | High | Open |

## Non-Functional Requirements

| ID | Title | Requirement | Category | Priority | Status |
|---|---|---|---|---|---|
| NFR-001 | Tenant isolation | Automated PostgreSQL integration tests must demonstrate 0 cross-tenant rows returned for every tenant-owned repository and RAG query. | Security | High | Open |
| NFR-002 | Trusted tenant source | 100% of protected requests must derive tenant context from a trusted host, provider binding, or validated identity context; client-supplied tenant identifiers must not grant access. | Security | High | Open |
| NFR-003 | Authorization coverage | 100% of protected commands and queries must enforce an explicit permission before accessing tenant data. | Security | High | Open |
| NFR-004 | Administrator MFA | 100% of platform-administrator interactive logins must require TOTP and provide recovery codes. | Security | High | Open |
| NFR-005 | Encryption in transit | Production external and internal network traffic must use TLS 1.3 or the strongest TLS version supported by the managed endpoint without enabling TLS 1.0 or 1.1. | Security | High | Open |
| NFR-006 | Secret leakage | Automated secret scanning must report 0 plaintext production credentials in committed files and built artifacts. | Security | High | Open |
| NFR-007 | API read latency | Under the agreed Locust baseline load, 95% of non-AI API reads must complete within 500 ms and 99% within 1,500 ms. | Performance | High | Open |
| NFR-008 | API write latency | Under the agreed Locust baseline load, 95% of non-provider API writes must complete within 1,000 ms and 99% within 2,500 ms. | Performance | High | Open |
| NFR-009 | Conversation latency | Excluding external channel delivery, 95% of text-only AI responses must complete within 10 seconds and 95% of single-image responses within 20 seconds. | Performance | Medium | Open |
| NFR-010 | Baseline concurrency | The production profile must sustain 100 concurrent active users with less than 1% server errors during a 15-minute Locust test. | Scalability | Medium | Open |
| NFR-011 | Availability | The production application must achieve 99.5% monthly availability excluding announced maintenance. | Availability | High | Open |
| NFR-012 | Graceful shutdown | During a rolling deployment, the application must stop accepting new traffic before termination and allow up to 30 seconds for in-flight requests to finish. | Availability | High | Open |
| NFR-013 | Recovery point | PostgreSQL backup and WAL retention must provide an RPO of 15 minutes or less. | Availability | High | Open |
| NFR-014 | Recovery time | A documented restore exercise must restore the production-sized baseline dataset within 60 minutes before launch and at least quarterly afterward. | Availability | High | Open |
| NFR-015 | Idempotency | Replaying the same supported webhook, booking request, or payment callback 10 times must produce no more than one durable business effect. | Reliability | High | Open |
| NFR-016 | Module boundaries | Every CI run must execute Spring Modulith verification and ArchUnit rules with 0 module cycles or forbidden internal-package dependencies. | Maintainability | High | Open |
| NFR-017 | Test gates | CI must run unit, H2 smoke, PostgreSQL Testcontainers, frontend unit, and critical Playwright tests with zero failed or skipped required tests. | Maintainability | High | Open |
| NFR-018 | Production DB fidelity | Every RLS, pgvector, PostgreSQL locking, Liquibase, and tenant-context behavior must have at least 1 passing PostgreSQL Testcontainers test. | Maintainability | High | Open |
| NFR-019 | Accessibility | Retained and modified frontend flows must satisfy WCAG 2.2 AA automated checks with zero critical violations. | Usability | Medium | Open |
| NFR-020 | Browser support | The web frontend must support the latest 2 stable versions of Chrome, Firefox, Safari, and Edge. | Portability | Medium | Open |
| NFR-021 | Mobile support | The retained Capacitor application must complete 100% of its critical smoke flow on the current and previous major iOS and Android versions supported by Capacitor. | Portability | Medium | Open |
| NFR-022 | Observability context | 100% of server request logs and traces must include request ID; tenant and conversation IDs must be included when applicable and safe. | Maintainability | High | Open |
| NFR-023 | Sensitive logging | Automated log tests must detect 0 raw access tokens, credentials, payment secrets, or unredacted message bodies in standard production logs. | Security | High | Open |
| NFR-024 | Health reporting | Kubernetes liveness and readiness endpoints must reflect application and required-dependency health within 30 seconds of a state change. | Availability | High | Open |
| NFR-025 | Outbox backlog | An alert must fire within 5 minutes when incomplete Modulith event publications grow continuously or the oldest incomplete publication exceeds 5 minutes. | Reliability | High | Open |
| NFR-026 | Database diagnosis | PostgreSQL `pg_stat_statements` must be enabled in production and queries exceeding 500 ms must be observable. | Maintainability | Medium | Open |
| NFR-027 | Dependency reproducibility | A clean CI environment must build 100% of backend and frontend artifacts from lockfiles without unpinned dynamic application dependencies. | Maintainability | High | Open |
| NFR-028 | Projection freshness | Under normal provider operation, 95% of committed projection-relevant changes must become queryable in pgvector and Apache AGE within 60 seconds. | Performance | Medium | Open |

## Constraints

| ID | Title | Constraint | Category | Priority | Status |
|---|---|---|---|---|---|
| C-001 | Backend runtime | Backend application code must use Java 25. | Technical | High | Open |
| C-002 | Build language | Gradle build scripts and convention plugins must use Kotlin DSL. | Technical | High | Open |
| C-003 | Application framework | The backend must use Spring Boot 4.1 and Spring Modulith 2.1, subject to final compatible stable patch verification. | Technical | High | Open |
| C-004 | Deployment unit | v1 backend capabilities must run as one Spring Boot deployment and one application image. | Technical | High | Open |
| C-005 | Frontend preservation | The existing React/Vite frontend, Capacitor iOS/Android projects, and shared TypeScript packages must be retained. | Technical | High | Open |
| C-006 | Durable database | PostgreSQL 17 with pgvector must be the durable source of truth. | Technical | High | Open |
| C-007 | Tenant storage | v1 tenant-owned tables must use shared-schema `tenant_id` isolation enforced by PostgreSQL RLS. | Technical | High | Open |
| C-008 | Database migrations | Liquibase SQL-formatted changelogs with a YAML master must own all application schema changes and development seeds. | Technical | High | Open |
| C-009 | Cache boundary | Redis 7.4 must store only expiring cache, lock, rate-limit, idempotency, OAuth-state, and conversation-summary data. | Technical | High | Open |
| C-010 | Identity provider | Keycloak 26 must issue tokens; the backend must operate as an OAuth2 resource server and must not implement password authentication. | Technical | High | Open |
| C-011 | AI framework | AI orchestration, model access, tools, and vector-store integration must use Spring AI 2.0 inside the Modulith. | Technical | High | Open |
| C-012 | AI source of truth | Prices and availability must be obtained through structured module APIs backed by SQL, never from RAG output. | Business | High | Open |
| C-013 | Local AI | Ollama must support local development models and the approved production embedding model. | Technical | Medium | Open |
| C-014 | External messaging | Production WhatsApp must integrate directly with Meta WhatsApp Cloud API. | Technical | High | Open |
| C-015 | Internal communication | v1 modules must communicate through Java APIs and Modulith events, not internal REST, gRPC, or protobuf. | Technical | High | Open |
| C-016 | Event durability | Cross-module event publication must use the Spring Modulith JDBC event registry. | Technical | High | Open |
| C-017 | Deferred broker | RabbitMQ and event externalization must remain deferred until Phase 3 WhatsApp scaling requires them. | Schedule | High | Deferred |
| C-018 | Web model | HTTP APIs must use Spring Web MVC with virtual threads; WebFlux must not be introduced in v1. | Technical | High | Open |
| C-019 | Persistence API | Durable relational persistence must use Spring Data JPA/Hibernate unless a PostgreSQL-specific query requires an explicit JDBC/native adapter. | Technical | High | Open |
| C-020 | Scheduling | In-process scheduled work must use Spring scheduling and ShedLock JDBC coordination. | Technical | Medium | Open |
| C-021 | API documentation | Externally callable HTTP endpoints must be documented using springdoc OpenAPI. | Technical | Medium | Open |
| C-022 | Local infrastructure | Daily local development must use k3d against the same Kubernetes manifests used by the single-node k3s VM. | Operational | High | Open |
| C-023 | Kubernetes packaging | EMME workloads must use Kustomize base plus development and production overlays; Helm is limited to third-party charts. | Operational | High | Open |
| C-024 | Kubernetes database | Production PostgreSQL must use CloudNativePG rather than a raw PostgreSQL Deployment and PVC. | Operational | High | Open |
| C-025 | Infrastructure provisioning | Terraform must provision shared cloud, cluster, DNS, networking, and backup infrastructure without deploying application workloads. | Operational | Medium | Open |
| C-026 | Kubernetes ingress | Kubernetes ingress must use the selected Traefik or ingress-nginx controller with cert-manager-managed TLS. | Operational | High | Open |
| C-027 | Secrets | Repository secrets must use SOPS with age or Sealed Secrets; plaintext production secrets are prohibited. | Security | High | Open |
| C-028 | Observability | Production signals must use OpenTelemetry with Grafana, Loki, Prometheus, Tempo, and Alloy or the approved Grafana Cloud equivalent. | Operational | High | Open |
| C-029 | Backend tests | Backend testing must use JUnit, H2 for portable smoke tests, and PostgreSQL Testcontainers for production-specific behavior. | Technical | High | Open |
| C-030 | Boundary tests | Spring Modulith verification must validate application module boundaries in CI. | Technical | High | Open |
| C-031 | Performance tests | Locust must own HTTP workload and performance regression scenarios. | Technical | Medium | Open |
| C-032 | Frontend tests | Existing frontend unit and Playwright coverage must be retained and adapted to the unified backend. | Technical | High | Open |
| C-033 | CI/CD | GitHub Actions must test, build one application image, publish it to GHCR, and deploy the selected Kustomize overlay. | Operational | High | Open |
| C-034 | Container image | The backend image must be built with Spring Boot Buildpacks or Jib and must not require an application Dockerfile. | Technical | Medium | Open |
| C-035 | Excluded frameworks | Quarkus and Micronaut dependencies and runtime configuration must not be copied into the target project. | Technical | High | Open |
| C-036 | Excluded service runtimes | Python AI gateway and TypeScript AI bridge behavior must be translated into Java Spring AI modules rather than retained as deployables. | Technical | High | Open |
| C-037 | Excluded technologies | Spring Batch, GraalVM native images, WebFlux, Kafka, service mesh, n8n, and Argo CD must not be required by v1. | Technical | High | Deferred |
| C-038 | Domain modeling | Core business entities must use explicit relational models; generic catch-all business-object tables are prohibited. | Technical | High | Open |
| C-039 | Module purity | Domain packages must not import Spring, JPA, Redis, transport, provider SDK, or AI framework types. | Technical | High | Open |
| C-040 | Dependency direction | Modules must expose explicit public APIs, prohibit repository/entity access across module boundaries, and contain no cyclic dependencies. | Technical | High | Open |
| C-041 | Graph projection | Apache AGE must be a tenant-isolated, derived, disposable, and rebuildable read model rather than an authoritative data store. | Technical | High | Open |
| C-042 | Projection mechanism | pgvector and Apache AGE projections must be populated after commit through durable Spring Modulith events with idempotent projectors, retries, and reconciliation; Debezium must not be required by v1. | Technical | High | Open |
| C-043 | Public application API | v1 application APIs must use REST over Spring Web MVC with OpenAPI contracts; GraphQL and Apollo runtime dependencies must not be required. | Technical | High | Open |

## Review Notes

- Schema-per-tenant and dedicated-database modes from the source platform are superseded for v1 by the shared-schema RLS constraint in `EMME-STACK.md`.
- Fiscal/CFDI and veterinary capabilities remain outside v1.
