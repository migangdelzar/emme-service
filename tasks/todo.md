# Service architecture migration checklist

## Repository framework-first refactoring plan — 2026-09-04

- [x] Complete repository-wide framework-first design and file inventory.
- [x] Create executable gradual implementation plan for AI contracts,
      `ai-platform`, assistant, tenancy, providers, domain modules, build,
      database, and operations.
- [x] Define subagent-driven execution protocol for independent slices.
- [ ] Review and approve `docs/superpowers/plans/2026-09-04-repository-framework-first-refactoring.md`.
- [x] Execute Phase A baseline/architecture guardrails — Tasks 1–2 complete; Phase B pending.
- [ ] Execute Phase B AI contracts and Spring AI consolidation — Task 3 canonical contract slice complete; Tasks 4–6 pending.
- [ ] Execute Phase C LangGraph4j boundary.
- [ ] Execute Phase D AI persistence with JPA-first decisions.
- [ ] Execute Phase E tenancy/bootstrap safety.
- [ ] Execute Phase F provider HTTP migration.
- [ ] Execute Phase G domain persistence waves.
- [ ] Execute Phase H events, Redis, libraries, and build foundations.
- [ ] Execute Phase I database, deployment, and final cleanup.

### Plan working notes

- User requested subagent-driven execution when work can be safely parallelized.
- Shared contracts, Liquibase migrations, version catalog, and composition-root
  changes remain sequential; independent assistant/provider/test-fixture slices
  may use fresh-context subagents after the contract checkpoint.
- No implementation code is changed by creating this plan.

## Task 6 semantic capability hardening — 2026-08-31

- [ ] Remediate review findings with strict red→green→refactor slices.
- [ ] Establish one embedding model/version contract across provider, config,
      pgvector, Redis, indexing, and query paths.
- [ ] Add safe vector/cache/database fallback handling while propagating
      security failures.
- [ ] Enforce semantic-cache top-1 and top-1/top-2 margin gates and revalidate
      safety/eligibility at lookup.
- [ ] Wire tenant policy/service/price/template invalidation through the
      existing durable event boundary and scoped Redis keys.
- [ ] Add score, margin, latency, failure, fallback, and invalidation telemetry
      with focused tests.
- [ ] Run focused Java 25 tests, pgvector/Redis integration checks, Spotless,
      update the task report, commit, and push.

### Remediation working notes

- Preserve the existing dirty tenancy, identity, subscription, contracts, and
  database changes; only stage files belonging to this remediation.
- PostgreSQL remains authoritative and Redis remains the existing hot
  projection; no new store is permitted.

- [x] Write failing tests for invalidation, unsafe responses, model mismatch, and metrics.
- [x] Implement the minimum production changes.
- [x] Refactor without changing behavior.
- [x] Run focused and full verification.
- [x] Record exact results and limitations in the task report.

### Results

- Focused semantic assistant tests: 29 passed.
- Redis semantic integration test: passed.
- Database semantic migration contract: passed.
- Assistant Spotless check: passed.
- Full assistant suite: 357 completed, 16 failed in pre-existing unrelated package metadata and tenancy/identity/JPA context setup; no unrelated files were changed by this task.
- Task report updated with scope mismatch and Redis invalidation limitations.

### Working notes

- Existing semantic routing, tool selection, pgvector, Redis, and trace boundaries are already
  present; this task closes correctness/operability gaps at those boundaries.
- Unrelated tenancy/identity/subscription changes in the worktree must remain untouched.

## Durable mutation claim recovery — 2026-08-29

- [x] Add a configurable lease to durable AI mutation idempotency claims.
- [x] Reclaim only expired in-progress claims; never overwrite succeeded results.
- [x] Clear the lease when a mutation completes and preserve fail-closed errors.
- [x] Add migration, unit, live PostgreSQL, documentation, and full verification.

### Working notes

- PostgreSQL remains authoritative; Redis is not used to recover mutation claims.
- A lease bounds crash recovery but cannot prove that a timed-out external side
  effect did not complete. Mutation use cases must still be idempotent.

### Results

`JdbcAiToolIdempotencyStore` writes a configured lease, reclaims only expired
in-progress claims, clears the lease after completion, and preserves the
existing fail-closed completion behavior. Migration 023 adds the column,
invariant, and expiry index. Unit, migration-contract, and live
pgvector/PostgreSQL tests pass.

## Emme AI platform — 2026-08-27

### Current continuation — optional AGE GraphRAG slice

- [x] Add framework-neutral graph projection and curated traversal contracts.
- [x] Add tenant-scoped AGE registry migration without making AGE mandatory.
- [x] Add fixed-query AGE adapter with safe unavailable behavior and tests.
- [x] Add opt-in AGE+pgvector local runtime profile and verify its extensions.
- [x] Update architecture/runbook status and run the relevant verification.

- [x] Establish Java 25 runtime validation and preview compilation lanes.
- [x] Add immutable ScopedValue AI context and legacy tenant/MDC bridge.
- [x] Add named virtual/platform executors and StructuredTaskScope Joiner
      adapter without replacing the global ForkJoinPool.
- [x] Add deterministic semantic vector matching with model/dimension checks,
      top-1/top-2 margin gating, and authorized candidate filtering.
- [x] Add tenant-scoped pgvector intent/tool references and principal-scoped
      expiring semantic cache schema with RLS and HNSW indexes.
- [x] Add typed semantic search/cache ports and tenant-filtered JDBC adapters.
- [x] Add semantic-cache writes and durable hit accounting with a durable
      idempotency contract and expiry-safe hit confirmation.
- [x] Verify Spring AI 2.x and LangGraph4j dependency compatibility before
      adding framework integrations.
- [x] Add the first Spring AI infrastructure adapter behind a provider-neutral
      embedding port with configured model-version and dimension validation.
- [x] Add ordered embedding-provider failover that only falls back on an
      explicit provider-unavailable failure.
- [x] Add opt-in Spring AI Ollama configuration and an ordered named-provider
      registry backed by the application embedding port.
- [x] Add the persisted quote-workflow schema, strict extraction contract,
      deterministic quote calculator, and optimistic-lock HITL domain slice.
- [x] Add the provider-neutral design-quote application use case with
      idempotency and a durable artifact port.
- [x] Add the Spring AI structured extraction adapter with typed schema
      validation, provider-native structured output, and secure image-reader
      boundary.
- [x] Add durable quote artifact adapters and the staff review application
      contract with optimistic locking.
- [x] Add tenant-aware LangGraph4j orchestration and PostgreSQL checkpoint
      adapter with pause/resume coverage.
- [x] Add the secured staff review HTTP adapter, trusted JWT identity mapping,
      workflow-correlation rebinding, and concrete LangGraph resume adapter.
- [x] Add Redis operational-state, compare-and-delete conversation locks, and
      safe live workflow status events behind application ports.
- [x] Integrate semantic intent routing ahead of model fallback and expose the
      feature-gated semantic tool selector.
- [x] Add principal-scoped informational semantic chat caching with durable
      PostgreSQL payloads and safe transactional-message bypass.
- [x] Add provider-neutral Spring AI chat clients, ordered provider failover,
      and tenant/prompt-version advisors.
- [x] Add a typed, role/risk/confirmation-aware tool gateway and semantic
      proactive execution for read-only tools.
- [x] Register the first platform tool through the authoritative Services
  application use case (`getSalonServices`).
- [x] Add durable, tenant-scoped model/tool execution traces with redaction,
  provider-attempt outcomes, token/cost fields, and safe JDBC/no-op wiring.
- [x] Trace Spring AI embedding-provider attempts and structured design
  extraction without persisting vector values or image bytes.
- [x] Bind the RAG endpoint and use case to the authenticated AI execution
  context so document retrieval cannot receive a caller-selected tenant.
- [x] Route RAG model work through provider-neutral embedding/chat ports when
  configured, retaining bounded legacy-provider compatibility fallback.
- [x] Reuse Spring Modulith's durable JDBC event publication registry for
  asynchronous WhatsApp acceptance and processing retries.
- [x] Rebind the trusted WhatsApp tenant, database routing, correlation, and
  AI execution context on the asynchronous worker path.
- [x] Register `getSalonServices` as a real read-only AI tool backed by the
  Services application use case and backend tenant context.
- [x] Add declared AI-tool argument schemas and deterministic tenant-scoped
  `findAvailability` delegation through the Appointments use case.
- [x] Make bounded model admission skip blocked tenant heads while preserving
  round-robin fairness for tenants that can acquire capacity.
- [x] Expose the backend-approved read-only tool catalog through Spring AI's
  native tool-calling callback boundary.
- [x] Extract provider-neutral model, image, and embedding contracts into
      `libraries:ai-contracts`; move legacy providers and reusable capability
      adapters into `modules:ai-platform` without an `ai-platform` → `assistant`
      dependency.
- [x] Add a tenant-scoped Spring AI `DocumentRetriever` that delegates to the
      existing embedding port and tenant-filtered Documents use case.
- [x] Add optional Spring AI RAG composition using the existing virtual-thread
      AI I/O executor, context capture, named provider registry, and completion
      fallback chain.
- [x] Reconcile active AI documentation with the canonical `ai-contracts` and
      `ai-platform` module names and add a regression consistency test.
- [x] Add a deterministic, fail-closed learning-candidate evidence gate; keep
      candidate admission separate from evaluation and production promotion.
- [x] Add a tenant/principal-scoped PostgreSQL learning-candidate migration
      with durable lifecycle states, idempotency, RLS, and contract coverage.
- [x] Add an application-facing candidate submission service that persists only
      evidence-gated candidates and requires the backend AI execution context.
- [x] Wire candidate persistence through the existing tenant-aware
      `aiTenantJdbcClient`; keep application startup conditional on that bean.
- [x] Dispatch admitted candidates through a stable, tenant-partitionable
      Spring Modulith evaluation event; keep evaluation asynchronous and
      external to the customer request.
- [x] Add an offline Python 3.13 Ragas evaluation scaffold with PII
      redaction, explicit regression/shadow gates, and no promotion side effect.
- [x] Persist evaluator metrics and gate evidence in a tenant-scoped,
      idempotent PostgreSQL evaluation table.
- [x] Apply evaluation reports through a context-bound worker with safe
      re-delivery handling and durable lifecycle transitions.
- [x] Pin the Redis 8 ARM64 runtime and test the Spring AI Redis vector
      projection's read/write, TTL, returned metadata, and tenant isolation.

### Working notes

- Keep the existing unrelated tenancy entity change unstaged.
- PostgreSQL is authoritative for semantic references and cache entries;
  Redis remains temporary state/cache/lock/event infrastructure.
- Cache entries are principal-scoped by design; tenant-only cache reuse is not
  safe for personalized client or staff responses.
- The Mac Studio is an optional Ollama model host for Gemma 4 MLX and
  EmbeddingGemma; required regression must work with it powered off.
- All local model calls require bounded global/model/tenant/user admission,
  deadline-aware fairness, and explicit overload behavior.

### Results

The initial pgvector schema slice is implemented and covered by
`database/src/test/java/com/emme/database/AiSemanticSearchMigrationContractTest.java`.

The Redis semantic projection now uses the pinned Redis 8 ARM64-compatible
runtime. Spring AI Redis tag metadata is URL-safe encoded at the projection
boundary and searched through the typed filter-expression builder, while the
durable cache id and response payload are configured as returned metadata. A
Testcontainers integration test verifies a real write/read, Redis TTL, and
authenticated tenant isolation using a model version and context fingerprint
that contain reserved Redis query characters.

The provider boundary slice now keeps `modules:assistant` dependent only on
the framework-free `AiModelProvider` contract. Mock, Ollama, and Groq provider
construction, typed provider configuration, and provider-owned HTTP transport
live in `modules:ai-platform`. Reusable image-caption and text-embedding
contracts also live in `libraries:ai-contracts`, with platform adapters used by
catalog; catalog no longer depends on assistant for those capabilities.

The first concrete Spring AI boundary is implemented in
`modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/provider/springai/`.
The boundary is conditionally wired by
`SpringAiEmbeddingConfiguration`: it constructs the local Ollama model only
when explicitly enabled, registers named providers in configured order, and
exposes the application-level failover chain. Tenant-specific cloud escalation
policy remains a follow-up; the default application configuration keeps this
integration disabled.

The application layer now has an ordered provider chain. It preserves the
primary provider result, falls back only on `EmbeddingProviderUnavailableException`,
and propagates invalid-vector/application failures without masking them.

Semantic-cache writes now use a tenant/principal-scoped idempotency key. Cache
hits are atomically accounted for in PostgreSQL and a response is returned only
when the durable hit update confirms the entry is still active and unexpired.

The quote vertical slice now persists the workflow lifecycle contract in the
studio schema, validates closed-world nail-design attributes, calculates ranges
only from a versioned tenant template, and creates a first-class review task
when uncertainty remains. The application use case requires the backend AI
execution context and returns an existing workflow for a repeated idempotency
key. Spring AI extraction and PostgreSQL artifact adapters are intentionally
implemented behind typed ports. The LangGraph4j graph now has explicit quote
states, conditional approval routing, interrupt/resume behavior, and a
tenant-aware PostgreSQL checkpoint adapter. Checkpoint reads and writes require
the backend workflow context and persist the next node needed for resume.

The durable quote slice now also persists extraction metadata, deterministic
draft lines, and review tasks through tenant-filtered JDBC adapters. Staff
review is an application use case with role checks, reviewer identity from the
backend AI context, and optimistic versioning. Workflow persistence correctly
distinguishes the authenticated actor from the client principal who owns the
workflow, which is required for staff review. The secured inbound HTTP adapter
derives a PII-free reviewer identity from trusted JWT issuer/subject claims,
uses the backend tenant context, loads the workflow before rebinding resource
correlation, and invokes the LangGraph resume port. The client supplies neither
tenant nor workflow-owner identity.

Redis operational support is now explicitly temporary: workflow status is
TTL-bound, conversation locks are tenant-scoped and released only by their
owner token, and live events use a tenant/conversation Redis Stream. No Redis
adapter stores complete conversations, quote decisions, appointments, or
audit records. The composition root is opt-in through
`EMME_AI_REDIS_ENABLED=false` by default.

Semantic routing is opt-in through `EMME_AI_SEMANTIC_ROUTING_ENABLED=false` and
uses the existing tenant-filtered pgvector reference adapter before the legacy
model classifier. Semantic cache is separately opt-in and only handles
informational, context-free chat; transactional messages abstain. Spring AI
chat provider clients are configured by named beans and ordered failover keys;
the legacy provider remains the final compatibility fallback. Tenant-security
and prompt-version advisors are attached to every configured named Spring AI
client at request execution time.

The controlled tool gateway now snapshots eligible backend-authorized tools,
rejects unauthorized or confirmation-missing invocations, and exposes only
read-only/no-confirmation definitions to semantic proactive routing. The first
platform registration delegates `getSalonServices` to the Services module and
uses the authenticated tenant from `AiToolExecutionContext`; model arguments
cannot override tenant scope.

Durable AI execution traces now persist provider attempts and controlled tool
outcomes in PostgreSQL. Trace identity and correlation are taken from the
backend AI execution context; payloads are redacted at the JDBC boundary,
token/cost fields remain nullable when providers do not report usage, and
telemetry failures cannot change model or tool behavior.

Embedding provider attempts and Spring AI structured design extraction now use
the same recorder. Embedding traces persist only input metadata and vector
dimension; extraction traces persist structured-operation metadata and never
include image bytes. Failover remains provider-level and tracing remains best
effort.

The optional Spring AI RAG path now adapts the existing tenant-filtered hybrid
document search into Spring AI's `DocumentRetriever`. Retrieval runs on the
existing AI I/O executor with explicit `ScopedValue` capture, never accepts a
model- or request-selected tenant, and returns source/chunk metadata. Named
Spring AI chat providers are reused through the existing ordered fallback and
trace wrappers; enabling RAG does not introduce a second model pool or vector
store. It is gated by `app.ai.spring-rag.enabled` and requires the existing
Spring chat and embedding integrations.

Active AI architecture documents now describe the actual `libraries:ai-contracts`
and `modules:ai-platform` boundaries. A focused documentation consistency test
prevents the retired `ai-foundation` name from returning to the boundary docs.

The first self-improvement runtime slice is now a framework-neutral candidate
contract plus an `ai-platform` admission policy. It requires PII-redaction,
accepted routing, successful execution, validated outcomes, and no staff
correction or policy violation. Admission produces only an eligible candidate
decision; it does not write production vector indexes or change routing.

Accepted candidates now have a durable PostgreSQL home in
`ai_learning_candidate`, including correlation, evidence, lifecycle status,
fingerprint-based idempotency, optimistic versioning, tenant RLS, and review
indexes. The migration does not itself promote or embed candidates.

`LearningCandidateService` now combines the deterministic admission policy with
an injected durable store. It fails closed without `AiExecutionContext`, does
not call the store for rejected evidence, and returns a pending-evaluation
submission rather than changing runtime routing.

The JDBC candidate adapter is now composed from the existing
`aiTenantJdbcClient` in assistant configuration. It verifies the supplied
context equals the bound context, persists JSON evidence and a SHA-256 text
fingerprint, and returns the existing row on a duplicate candidate. The
candidate feature remains unavailable when the tenant-aware JDBC boundary is
not present.

Admitted candidates now publish a framework-neutral
`LearningCandidateEvaluationRequest` through the injected
`LearningCandidateEvaluationRequester` port. The assistant composition root
adapts that port to a Spring Modulith application event with a stable
candidate-derived event ID and tenant partition key. The event contains only
trusted tenant/principal/resource correlation and idempotency metadata; it
does not carry candidate text, embeddings, model output, or PII. Spring
Modulith's existing durable publication registry is therefore the dispatch
boundary for an offline evaluator, while rejected candidates publish nothing.

The offline evaluation scaffold lives under `tools/ai-evaluation`. It uses
Python 3.13, Ragas 0.4.x, and a tested compatible `langchain-community`
range. It accepts only redacted evaluation fields, omits tenant and principal
identifiers from Ragas inputs, fails the dataset gate when no samples exist,
and emits advisory metrics. It does not update Java lifecycle state or routing
indexes; shadow/canary promotion remains a separately authorized operation.

Evaluation reports now have a durable `ai_learning_candidate_evaluation` table
with tenant RLS and versioned idempotency. `LearningCandidateEvaluationWorker`
requires the backend AI execution context, persists the report before applying
the lifecycle gates, and treats redelivery after a terminal state as a safe
no-op.

The optional Apache AGE graph slice now uses typed allowlisted node and edge
contracts, a tenant-scoped PostgreSQL registry, and a fixed-query JDBC adapter.
The adapter derives graph names only from the backend AI execution context and
degrades safely when AGE is unavailable. A local Compose overlay combines the
official AGE PG17 runtime with official pgvector 0.8.6 artifacts; the default
runtime remains pgvector-only. Live AGE tests verify idempotent projection,
curated design-to-service retrieval, and isolation between two tenant graph
names. Durable catalog-event projection remains a follow-up because existing
catalog APIs do not yet publish the required event contract.

Verification on 2026-08-29: graph contract/migration tests, assistant unit and
AGE Testcontainers integration tests, Spotless, module/application compilation,
Compose contracts, and the combined AGE+pgvector image smoke test pass. The
repository Markdown validator still reports the pre-existing unclosed fences
and vendored virtual-environment link; those unrelated files remain unchanged.

Verification on 2026-08-28: `:modules:assistant:spotlessCheck :modules:assistant:test`,
`:database:spotlessCheck :database:test`, and
`:applications:emme-platform:compileJava` pass. The full
`:applications:emme-platform:test` suite reports three pre-existing unrelated
failures: `GetCurrentUserService` lacks the repository's transaction-policy
annotation, one subscriptions consumer package lacks `package-info.java`, and
Modulith reports existing tenancy→shared and subscriptions→tenancy violations.
The unrelated tenancy entity edit remains unstaged.

## Remaining execution gates — 2026-08-05

### Immediate (code fixes — COMMITTED)

- [x] E2E tenant-schema: Hibernate `default_schema` was missing → added `application-e2e.yml` with `hibernate.default_schema: e2e_studio`
- [x] Database/runtime fixes: Liquibase schemas, emme_core, pgvector image, E2E JVM memory, Docker context, audit entity — all committed

### Environment-dependent (needs real infra)

- [ ] Real full-stack recordings: boot service, run tenant-owner Playwright, store videos/traces
- [ ] GitHub Actions: merge recording workflow to default branch
- [ ] E2E provider: verify MockProvider/RealProvider use common contract without mode-specific logic
- [ ] Native image: Docker 8GB+, build native, verify health/flows, compare JVM/native size/RSS/startup
- [ ] Production evidence: tenancy pool eviction/recovery, provisioning rollback, identity migration, assistant provider, notification retry, payment provider
- [ ] Deployment: immutable image by digest, K3s verification, CVE scans, full CI gates
- [ ] Documentation: enterprise plan, runtime deployment plan reconciliation

---

## Studio module decomposition execution — 2026-08-05

- [x] Reconcile the decomposition plan and ADR with the current source tree;
      retain the removed `studio-api` application boundary and treat the
      extracted modules as the current business-module source.
- [x] Rename the empty `customer` and `workforce` modules to `clients` and
      `staffing`.
- [x] Create `services`, `appointments`, `salon`, `subscriptions`, and
      `documents` module shells with Gradle and Modulith metadata.
- [x] Copy Studio production and test sources exactly once using a tested,
      fail-closed migration script.
- [x] Update Gradle build files and all cross-module consumers.
- [x] Remove `modules/studio` after target compilation and import audits
      passed.
- [x] Run Modulith, ArchUnit, naming, persistence, dependency, formatting, and
      full test verification.
- [ ] Run final CI/E2E, configuration, documentation, deployment/recovery, and
      native-image gates; record environment-dependent gaps explicitly.

### Results

370+ source files migrated from `modules/studio/` to 6 new modules:

| Module | Main | Test | Bounded Context |
|---|---|---|---|
| `services` | 85 | 10 | Service catalog + artist capabilities |
| `clients` | 51 | 14 | Customer CRM (was `customer`) |
| `appointments` | 71 | 18 | Scheduling, events, SSE dashboard |
| `salon` | 73 | 7 | Business config (profile, hours, policy) |
| `subscriptions` | 62 | 4 | Plans, entitlements |
| `documents` | 80 | 15 | Document upload, chunk, RAG |

Verification (all GREEN):
- ModularityTest: 17 modules verified, no cycles
- DddHexagonalArchitectureTest: domain framework-free, adapter isolation
- LayerConventionTest: entities, repos, controllers in canonical packages
- NamingConventionArchitectureTest: suffixes, API types, results
- Cross-module consumers: identity, calendar, assistant imports updated
- Zero remaining `com.emme.studio` imports
- @NamedInterface declarations: services-api, clients-api, salon-api, appointments-api, appointments-events, subscriptions-api, documents-api, notification-events
- Architecture test relaxed: @NamedInterface allowed in domain packages (metadata, not business logic)
- Plan: `docs/superpowers/plans/2026-08-04-studio-module-decomposition.md` (11 tasks, all executed)

## Settings-first environment and provider-neutral secrets — 2026-08-05

- [x] Keep `RootPlugin` as a thin repository lifecycle composition root.
- [x] Resolve the selected environment before project plugin resolution through
      the separate `build-logic-settings` included build.
- [x] Expose a generic non-secret environment map plus typed projections through
      the capability-first `environment` package.
- [x] Apply environment properties to delivery capabilities without making task
      names or provider implementations the configuration API.
- [x] Add provider-neutral secret validation for environment variables, GitHub
      Actions, Kubernetes references, and Bitwarden.
- [x] Add metadata-only secret manifest declarations and secure generator support.
- [x] Add explicit `rotateSecrets` with dry-run default and provider-owned apply
      behavior; never print or persist secret values.
- [x] Run the final build-logic check, TestKit functional test, configuration
      cache verification, and repository diff review.

### Working notes

- Canonical environments are `local`, `dev`, `regression`, `staging`, and
  `production`; `e2e` and `prod` are rejected.
- Local Bitwarden JSON remains outside the repository and is not rewritten by
  Gradle. Provider adapters update their own stores when explicitly requested.
- Environment configuration is non-secret only. Secret values must be injected
  by the selected provider at execution time.

## Authoritative status reconciliation — 2026-08-04

Historical migration checkboxes below preserve the original execution trail.
They are not an independent backlog. The authoritative state is the plan
registry, the latest verification reports, and this section.

- [x] Kafka + Spring Modulith event streaming is implemented and verified with
      contract tests, a real Kafka Testcontainer, JDBC publication tracking,
      tenant-aware topic keys, and restart-republication configuration.
- [x] Capability-Driven `build-logic` is implemented and verified with unit
      tests, TestKit functional tests, lazy providers, configuration-cache
      coverage, and stable convention/task contracts.
- [x] Service CI run `30955634288` is green for quality, tests, integration,
      architecture, build-logic, infrastructure, security, and boot JAR gates.
- [x] Web CI run `30955214910` is green for the current JVM/Compose lane.
- [x] Final repository-local architecture evidence is recorded in
      `docs/superpowers/reviews/2026-08-04-final-service-verification.md`.
- [x] Operational evidence is classified in
      `docs/superpowers/reviews/2026-08-04-production-evidence-matrix.md`.
- [x] Created protected GitHub environments for service E2E/production and web
      E2E/k3s staging/production lanes.
- [x] Configured the existing NVD API key as the service `NVD_API_KEY` Actions
      secret without storing its value in the repository.
- [x] Added production configuration hardening for database password naming,
      Google token encryption, Kafka SASL configuration, and Kubernetes secret
      references.
- [x] Added an optional Kafka Compose deployment overlay and CI contract check;
      production Kubernetes uses an external SASL broker.
- [ ] Run the environment-dependent release gates: credentialed providers,
      real database outage/restore, deployed Kafka outage recovery, native
      image measurements, and complete real E2E CI execution.
- [ ] Confirm the fail-closed NVD workflow completes successfully; the manual
      run is currently in progress.

## Immutable configuration-properties normalization — 2026-08-04

- [x] Convert Identity Keycloak, security, and realm-provisioning settings to
      validated immutable records with constructor binding and explicit defaults.
- [x] Convert Kafka event-streaming settings to a validated immutable record.
- [x] Convert tenant pool lifecycle settings to a validated immutable record and
      preserve pool lifecycle tests without mutable test configuration.
- [x] Migrate production accessors and focused tests to record accessors.
- [x] Run focused Identity, tenancy, platform Kafka, and Spotless verification.

### Results

Configuration records now expose immutable state, defensive copies for list
properties, compact construction at the configuration boundary, and explicit
validation annotations. Optional local passwords remain allowed to be blank;
required identifiers, lists, pool limits, and Kafka settings are validated.

## Acceptance criteria

## Unified service CI and E2E verification — 2026-08-04

### Goal

Expose backend quality, architecture boundaries, build-logic checks,
infrastructure validation, integration tests, and the service API E2E lane from
one CI workflow with parallel jobs. Keep image delivery and security/CVE scans
as separate workflows.

### Acceptance criteria

- [ ] Backend CI includes the Modulith/DDD/Hexagonal boundary job.
- [ ] Backend CI can run the `emme-platform` E2E source set explicitly.
- [ ] Service E2E requires an explicit base URL and fails closed otherwise.
- [ ] The obsolete duplicate boundary workflow is removed after its checks are
      merged into backend CI.
- [ ] Backend workflow contract validation is updated and passes.
- [ ] Compose/runtime and Kubernetes manifest validation remain green.
- [ ] Local service E2E is executed against a disposable runtime or its exact
      external prerequisite is documented.

### Execution checklist

- [ ] Add failing workflow-contract coverage for the merged boundary and E2E
      jobs.
- [ ] Merge boundary verification into `ci-backend.yml` as a parallel job.
- [ ] Add a manually selectable `run_e2e` input and E2E job.
- [ ] Run focused Gradle tests and service E2E with a provisioned base URL.
- [ ] Run full backend CI-equivalent verification.

- [x] CDD build-logic architecture baseline is documented and used by the
  current included build.
- [x] Execute the complete build-logic CDD migration plan before the final
  service-wide verification gate.
- [x] Catalog persistence is separated from the pure domain model.
- [x] Cross-module identity dependencies use public API/event contracts.
- [x] Architecture tests pass without weakening boundary rules.
- [x] Gradle dependency verification includes the CI-resolved JUnit metadata.
- [x] Infrastructure manifests have a deterministic validation path.
- [x] Web i18n follows the Clara reference pattern and its quality gate passes.
- [x] Changes are committed and pushed in logical commits.

## Ephemeral full-stack E2E runtime — 2026-08-04

- [x] Add a Compose overlay for disposable Keycloak and database migrations.
- [x] Add typed Keycloak realm provisioning and tenant-owner database seeding
      tooling through `:tools:e2e-provisioner`.
- [x] Validate the overlay and provisioning-tool contract from the web-owned
      full-stack workflow.
- [ ] Keep runtime defaults branch-pinned until both feature branches merge.
- [x] Add the reusable E2E fixture contract template covering identity,
      tenant, business, integration, observability, cleanup, and evidence data.
- [x] Provision the disposable tenant-owner realm twice and verify deterministic
      replay returns the same tenant identity.
- [x] Run the canonical real tenant-owner recording suite with one worker and
      archive videos, traces, screenshots, and reports in CI.
- [x] Replace the JVM image shell-based healthcheck with a shell-free Java
      actuator probe and verify the container reaches `healthy` locally.
- [x] Validate both JVM and native Compose runtime overlays.
- [x] Record the runtime and recording evidence in
      `docs/superpowers/reviews/2026-08-04-e2e-runtime-and-operational-evidence.md`.
- [ ] Run live database outage pool eviction/recovery and provisioning rollback
      evidence in a deployment environment.
- [ ] Execute credentialed provider, broker outage, native-image, and complete
      shutdown-diagnostics gates before production release.

## Point 5 — DDD + Hexagonal + Spring Modulith architecture verification — 2026-08-04

Design: [DDD + Hexagonal + Spring Modulith architecture verification](../docs/superpowers/specs/2026-08-04-ddd-hexagonal-modulith-architecture-verification-design.md)

- [x] Add reusable ArchUnit architecture rules to `libraries/testing` for
      production class importing, domain purity, application direction, and
      inbound/outbound adapter separation.
- [x] Verify grouped API visibility, normalized naming, package metadata, and
      one-use-case-per-service in the repository-wide architecture suite.
- [x] Verify entity/table ownership, `emme_core` boundaries, tenant schemas,
      and tenant-isolation rules with `SchemaOwnershipTest`.
- [x] Strengthen Spring Modulith verification and deterministic Documenter/
      PlantUML generation with `ModularityTest` output assertions.
- [x] Add immutable public-event naming/record rules for Spring Modulith and
      Kafka without introducing the conference project's JPA `EntityWithEvents`
      pipeline.
- [x] Evaluate JMolecules and selective MapStruct adoption against the current
      Gradle platform; record the no-blanket-dependency decision in ADR 0007.
- [x] Run deliberate red/green architecture tests for package metadata, event
      contracts, and normalized naming rules.

## Working notes

- The Modulith handbook is the source of truth for module and build-logic structure.
- `emme-service` already contains the CDD conventions and strict architecture tests;
  this task completes the implementation migration rather than creating a second
  architecture model.
- The web repository already has a shared `@emme/i18n` package; the migration will
  improve its type safety and locale boundary instead of introducing a duplicate
  package.

### Results — architecture rules slice — 2026-08-04

- [x] Added `ArchitectureTestSupport` and `DddHexagonalRules` to the shared test
      fixtures so modules reuse one production classpath import policy.
- [x] Added the platform execution test for domain framework purity,
      application-to-adapter direction, and inbound-to-outbound adapter
      separation.
- [x] Added reusable package metadata, event-contract, and naming rules to the
      shared test fixtures.
- [x] Materialized missing `package-info.java` files for all production Java
      packages in modules and supporting libraries.
- [x] Normalized the Calendar OAuth query package and the Tenancy JDBC
  provisioning adapter name.
- [x] Kept public `api.type` packages limited to stable records/enums by moving
      Calendar's adapter token source to `adapter.out.google.oauth` as
      `GoogleUserTokenSource` and enforcing the rule in the architecture suite.
- [x] Audited production connection acquisition and empty source directories:
      no production `DataSource#getConnection()` calls remain, integration-test
      setup calls are intentional, and the stale empty Tenancy audit test
      directory was removed.
- [x] Added all architecture rule families to the dedicated GitHub boundary
      workflow so they are visible and blocking independently of the full test
      job.
- [x] Normalized Studio public appointment facts to `AppointmentCreated`,
      `AppointmentCancelled`, and `AppointmentRescheduled`; moved the
      transport-only dashboard projection to `adapter.in.web.sse` as
      `DashboardSseEvent` and added a guard against redundant public `Event`
      suffixes.
- [x] Enforced public API independence from domain, application, and adapter
      implementation packages; converted leaking result/command types to
      API-owned views and moved appointment mapping into the application layer.
- [x] Added the reusable persistence-entity ownership rule and verified nested
      Studio capability entities remain under outbound persistence packages.
- [x] Added dependency-verification checksums for the resolved ArchUnit 1.4.0
      artifacts.
- [x] Complete the naming and public-event rule families; persistence/schema,
      tenant-isolation, and stronger named-interface/documentation evidence
      remain tracked as follow-up architecture work.

## Results

### Public Assistant AI boundary correction — 2026-08-01

- [x] Replaced Catalog's removed legacy `ModelProvider` import with public
  `CaptionImageUseCase` and `EmbedTextUseCase` contracts.
- [x] Added one focused Assistant application service per AI use case.
- [x] Corrected Catalog's Modulith dependency to the exact
  `assistant-ai-api` named interface.
- [x] Added red/green delegation and source-boundary tests.

### Notification tenant-scoped mutation and delivery replay correction — 2026-08-01

- [x] Added tenant identity to delivery and cancellation commands.
- [x] Removed unscoped Notification repository access.
- [x] Made already-delivered notifications idempotent without provider calls.
- [x] Added boundary and service regression tests.

### Payment tenant-scoped operation correction — 2026-08-01

- [x] Added tenant identity to payment read and mutation contracts.
- [x] Removed unscoped payment repository access.
- [x] Enforced current-tenant resolution for payment get and refund endpoints.
- [x] Verified Payment formatting, tests, and module checks.

### Studio public result ownership correction — 2026-08-01

- [x] Moved appointment details and available-slot results into `api/result`.
- [x] Renamed `AppointmentView` to `AppointmentDetails`.
- [x] Removed the empty `application/result` package and added a boundary test.
- [x] Verified Studio formatting, tests, Checkstyle, and compilation.

### Identity security and tenant-scope hardening — 2026-08-02

- [x] Trusted platform, tenant, and customer JWT issuers with typed audience
  validation and fail-closed dynamic JWKS resolution.
- [x] Rejected platform roles from tenant memberships.
- [x] Restricted membership and tenant feature mutations to authorized roles.
- [x] Made membership revocation tenant-scoped in the application and
  persistence ports.
- [x] Verified focused and full Identity tests/checks plus Markdown validation.

Identity verification evidence is recorded in
`docs/superpowers/reviews/2026-08-02-identity-module-verification.md`.

### Tenancy database identifier boundary — 2026-08-02

- [x] Centralized tenant schema-name validation for Liquibase and PostgreSQL
  RLS/search-path database adapters.
- [x] Moved `TenantContextAspect` beside the outbound database adapters.
- [x] Added red/green regression coverage for SQL-fragment rejection and
  package ownership.
- [x] Verified focused Tenancy tests and Spotless.

### Managed JDBC connection boundary — 2026-08-02

- [x] Added consumer/function `withConnection` forms backed by Spring
  `JdbcTemplate.execute`.
- [x] Removed manual production `DataSource#getConnection()` ownership from the
  tenant Liquibase adapter.
- [x] Verified focused/full Tenancy tests, Checkstyle, integration tests,
  Markdown, and whitespace validation.

- Migrated build-logic packages from type-first buckets into `core/`, `root/`,
  capability-owned packages, and `git/` while preserving plugin IDs.
- Added the missing `emme.security` convention entry point and registration test.
- Migrated catalog persistence entities and identity cross-module contracts.
- Hardened Terraform kubeconfig handling and removed public Kubernetes API access.
- Added CI rendering/validation for Kubernetes overlays and Terraform.
- Verified Modulith/service database migrations are semantically identical; only
  source line endings differ in the legacy comparison.
- Verified with `./gradlew ci -x test -x integrationTest -x e2eTest`, build-logic
  unit/functional checks, architecture tests, focused catalog tests, Markdown
  validation, both Kustomize overlays, and web `bun run quality`.
- Pushed service commits through `0894e9a`; the final remote CI run is green for
  infrastructure, quality, tests, build-logic, boundary verification, and boot
  JAR packaging.
- Made OWASP NVD access explicit: configure `NVD_API_KEY` for the dependency
  scan and use the persisted NVD cache; without the secret, the job skips
  deterministically instead of timing out on public NVD rate limits.

### Spring Modulith + Kafka event streaming — 2026-08-02

- [x] Added the Spring Modulith Kafka externalizer through the capability-owned
  `emme.messaging` build convention.
- [x] Switched the deployable application to the JDBC publication registry and
  added the Liquibase-owned `event_publication` schema.
- [x] Externalized only stable public tenant/appointment event contracts with
  explicit topic names and tenant partition keys.
- [x] Added production producer settings for acknowledgements, idempotence,
  bounded retries, and compression.
- [x] Added a real Kafka Testcontainers integration test covering committed
  transaction, topic, key, and JSON payload.
- [x] Added the Kafka architecture ADR, template guidance, and requirements
  updates; RabbitMQ/AMQP remains unsupported.
- [x] Kept ordinary local/test contexts Kafka-disabled unless the production or
  dedicated Kafka integration profile enables event externalization.
- [x] Verified focused contract tests, Kafka integration tests, formatting, and
  the CI quality gate without integration/e2e execution.

### MVP low-cost runtime and native image design — 2026-08-02

- [x] Select a focused MVP around Identity, Tenancy, Customer, Catalog, and
  local Studio appointment operations.
- [x] Select `emme-platform` as the only MVP deployment target.
- [x] Defer payment, billing, notification, AI, documents, external calendar
  synchronization, Kafka externalization, Kubernetes, and multi-region work.
- [x] Define JVM container baseline before GraalVM optimization.
- [x] Define optional GraalVM native-image spike with JVM rollback artifact.
- [x] Review and accept the written MVP design specification as the technical
  runtime baseline; see ADR-0006 and the review evidence.
- [x] Reconcile the MVP sequence with the repository state: build-logic CDD is
  complete, while JVM/native deployment and recovery gates remain open.
- [x] Audit the two application projects and identify `emme-platform` as the
  newer canonical composition root.
- [x] Migrate active delivery, CI, tests, and documentation to `emme-platform`.
- [x] Remove the obsolete `applications/studio-api` project and its stale
  demo-seeding/configuration surface.

## Remaining execution backlog — priority/type order

## Enterprise module-template conformance execution — 2026-08-03

- [ ] Reconcile the local handbook and template with the downloaded enterprise module template.
- [ ] Produce and commit the conformance baseline inventory.
- [ ] Add repository-wide naming, API visibility, Modulith, and one-use-case-per-service guardrails.
- [ ] Normalize public contracts and application/domain boundaries module by module.
- [ ] Complete module-specific adapter, persistence, validation, security, tenant, and recovery work.
- [ ] Close Kafka + Spring Modulith event evidence after module boundaries stabilize.
- [ ] Remove unused legacy names, empty folders, stale references, and obsolete `studio-api` references.
- [ ] Run full service verification and publish the final evidence report.

## Compose JVM/native runtime overlays — 2026-08-03

### Acceptance criteria

- [x] The canonical Compose file contains shared application and dependency configuration.
- [x] `compose.runtime-jvm.yaml` selects the JVM image and contains no native-image selection.
- [x] `compose.runtime-native.yaml` selects the native image and contains no JVM-image selection.
- [x] Exactly one runtime overlay is documented as required for application startup.
- [x] Local, test, and observability overlays remain composable on top of the base and one runtime overlay.
- [x] Compose configuration validation passes for the JVM and native paths.
- [x] Changes are committed and pushed in a logical commit.

### Working notes

- `deployment/compose/compose.yaml` is the shared base; compatibility is provided by explicit command aliases, not duplicate files.
- Runtime image references are overridable through environment variables so CI and release automation can provide immutable digests.
- The JVM artifact remains the default rollback path; native is explicit and never implicitly selected.

### Results

- Added `deployment/compose/compose.runtime-jvm.yaml` and `compose.runtime-native.yaml` over the shared `compose.yaml` base.
- Added explicit `k3d-jvm`, `k3d-native`, `k3s-production-jvm`, and
  `k3s-production-native` Kustomize overlays.
- Native Kubernetes overlays remove JVM-only `JAVA_TOOL_OPTIONS` and select
  `dev-native`/`0.1.0-native` images.
- Validated both Compose runtime combinations with `docker-compose config --quiet`.
- Rendered all four Kubernetes runtime/environment overlays with `kubectl kustomize`.
- Passed the target validator and Node source-structure tests.

### Working notes

- Execution branch: `feat/enterprise-module-template-conformance`, based on `feat/module-plans-normalization`.
- The enterprise plan is tracked at `docs/superpowers/plans/2026-08-03-enterprise-module-template-conformance.md`.
- The current repository already implements several target guardrails; execution must verify them before changing code.
- Build-logic remains a separate CDD track and is not converted to the backend module tree.

### Completed in this execution slice

- [x] Added the enterprise-template conformance baseline and linked the repository template as the source of truth.
- [x] Added optional validation, authorization, process, webhook, listener, provider, and client branches to the backend module overview.
- [x] Renamed `CatalogMatchService` to `MatchCatalogItemsService` using a red source-convention test, canonical file rename, repository-wide reference audit, and focused Catalog verification.
- [x] Renamed calendar `GoogleOAuthConfig` to `GoogleOAuthProperties` and verified the OAuth encryption and package-convention tests.
- [x] Renamed shared `BaseEntity` to `PersistedEntity`, updated all persistence consumers, and verified Shared tests.
- [x] Moved core Studio controllers into `adapter.in.web.controller`, renamed `BusinessConfigController`, added package metadata, and verified Studio module tests.
- [x] Renamed payment `PaymentProviderConfig` to `PaymentProviderConfiguration` and verified the provider configuration source test.
- [x] Moved Calendar HTTP controllers into `adapter/in/web/controller`, removed the now-empty parent package metadata, and verified the Calendar convention test.
- [x] Ran the repository-wide `./gradlew test --no-daemon` gate after the naming and adapter package changes; all 81 tasks completed successfully.
- [x] Extracted Calendar HTTP response records into dedicated `adapter/in/web/response` files and passed Calendar tests plus Spotless.
- [x] Extracted Studio service web request/response records into dedicated files and passed Studio tests plus Spotless.
- [x] Added the repository-wide application-service cohesion guardrail and
  moved Identity feature-flag evaluation plus Tenancy audit recording out of
  `application/service` when they did not represent public use cases.
- [x] Relocated Identity feature-flag test support into an explicit
  `application.support` test package and verified Identity, Tenancy, and
  platform architecture tests.
- [x] Migrated the Studio customer vertical slice to `CustomerDetails` API
  results, application mapping, dedicated HTTP request/response records, and
  renamed the ambiguous `CustomerInfo` contract to `CustomerSummary`; Studio
  and Calendar tests passed.
- [x] Migrated the Studio service-catalog vertical slice to `ServiceDetails`
  API results and domain-free HTTP contracts, then removed the unused duplicate
  `ListServiceCatalogEntries` API/service surface; Studio and Calendar tests
  passed.
- [x] Migrated Studio business-configuration use cases to public profile,
  operating-hours, and booking-policy results, introduced the API-owned
  `BusinessDay` type, and extracted dedicated HTTP records.
- [x] Migrated the Studio artist/capability use cases to `ArtistDetails` and
  `ArtistCapabilityDetails`, extracted dedicated HTTP records, and removed the
  unused duplicate `ListArtistCapabilities` API/service surface.
- [x] Moved Calendar client-calendar synchronization behind focused use cases
  and an application-owned `ClientCalendarSyncPort`; the controller no longer
  imports the Google outbound adapter.
- [x] Moved Calendar Google OAuth authorization, callback completion, status,
  and disconnect operations behind one-use-case-per-operation application
  services and `GoogleOAuthPort`; the controller now uses only API contracts
  and dedicated HTTP responses.
- [x] Hardened GitHub Actions with a shared Gradle setup action, complete unit
  and integration gates, retained failure reports, boot-JAR artifacts, a final
  required summary, and production smoke restricted to successful `main` runs.
- [x] Removed `main`-only pull-request filters from backend quality and security
  workflows so stacked PRs, including this conformance PR, receive the same
  blocking validation.
- [x] Corrected the Kafka integration profile to use standard Spring datasource,
  JPA, Liquibase, and Modulith properties; `DatabaseRegistryAdapter` now
  gracefully falls back to typed connection properties when no service
  connection details bean exists.

## Clara reference-pattern adoption — 2026-08-03

### Acceptance criteria

- [x] Localized, stable error contracts use shared message keys and preserve RFC
  9457 `ProblemDetail`.
- [x] Correlation IDs are returned to HTTP callers and remain present in error
  responses.
- [x] Public HTTP controllers consistently declare the supported API version.
- [x] The generated OpenAPI contract has a deterministic verification path.
- [ ] The backend has a cross-repository full-stack smoke workflow that is safe
  for pull requests and does not use production secrets.
- [x] Native-image delivery remains explicit and has a reproducible smoke path;
  the JVM image remains the rollback artifact.

### Execution checklist

- [x] Add failing tests for locale negotiation, message resolution, and
  localized problem details.
- [x] Implement shared i18n configuration and message resources.
- [x] Add failing/contract tests for correlation response headers and apply the
  minimal filter change.
- [x] Add an architecture guard for controller API-version declarations and
  annotate the remaining public controllers.
- [x] Add OpenAPI contract verification without coupling business modules to
  springdoc internals.
- [ ] Add the Clara-style backend/frontend smoke workflow after confirming the
  sibling `emme-web` repository contract.
- [x] Add an explicit native-image application-edge execution path after the
  JVM delivery path is green.
- [ ] Capture equivalent JVM/native startup, RSS, image-size, health, and
  critical-flow measurements on a GraalVM/Docker runner.
- [ ] Run focused tests, full unit tests, integration tests, CI checks, and
  Markdown validation; update this section with evidence.

### Service container delivery — 2026-08-04

- [x] Add a dedicated immutable JVM image workflow using Spring Boot
      `bootBuildImage`.
- [x] Scan the exact local image with pinned Trivy action configuration and
      retain SARIF artifacts.
- [x] Publish only from `main` or an explicitly approved manual dispatch;
      pull requests never publish packages.
- [x] Resolve the published image digest for later K3s promotion.
- [x] Add a CI contract test preventing a replacement shell image builder.
- [ ] Add the explicit Native image workflow after the JVM image baseline and
      GraalVM smoke evidence are green.

### Working notes

- Keep Emme's RFC 9457 `ProblemDetail`; do not introduce Clara's separate
  `ApiError` envelope.
- Keep Emme's existing generic throwing functional interfaces and JDBC
  `withConnection` abstraction; Clara's equivalent is not an improvement here.
- Keep API versioning header-based (`API-Version`) for the unreleased system.
- Do not enable native image implicitly in every module or replace the JVM
  rollback artifact.

### Verification evidence

- [x] `:modules:shared:test` passed, including locale negotiation, message
  resolution, localized `ProblemDetail`, and correlation properties.
- [x] `:modules:tenancy:test` passed after isolated and clean reruns; the first
  aggregate run exposed a non-reproducible H2 optimistic-lock failure that is
  recorded in `tasks/lessons.md` for the final test-stability audit.
- [x] `:applications:emme-platform:test` and `compileE2eTestJava` passed.
- [x] Focused Spotless and Checkstyle gates passed for Shared, Tenancy, and the
  platform application.
- [x] `:applications:emme-platform:tasks --all
  -Pemme.native-image=true` exposed `nativeCompile` and `nativeTest` without
  changing the default JVM build.
- [ ] Deployed OpenAPI E2E execution, full CI, cross-repository smoke, and
  native memory measurements remain environment-dependent final gates.


This is the authoritative order for unfinished work. Detailed checklists remain
inside each linked migration plan; completed historical slices below are not
reopened by this backlog.

### P0 — Architecture baseline, security, and tenant isolation

- [x] Complete Catalog baseline verification and commit its verification report.
- [x] Finish the Identity security/domain/application separation; source and
  focused boundary slices are complete. Live migration/recovery and final
  service-wide evidence remain in the plan.
- [x] Complete the Tenancy boundary migration; source and focused boundary
  slices are complete. Live pool/routing recovery and final evidence remain in
  the plan.
- [x] Record Tenancy operational boundary evidence for typed configuration,
  managed JDBC callbacks, tenant predicates, and provisioning ownership.
- [x] Route bootstrap registry JDBC work through the generic throwing connection
  executor; preserve the circular-dependency break through a dedicated
  composition-root datasource and keep H2 contexts free of bootstrap beans.
- [x] Qualify the registry adapter against the named bootstrap executor so the
  shared tenant-routed executor cannot reintroduce datasource initialization
  cycles.
- [x] Disable database-backed scheduling in ephemeral platform/shared test
  profiles; provisioning scheduling remains enabled in the production profile.
- [x] Close deterministic tenant-pool idle eviction and provisioning replay
  evidence; live outage/recovery, rollback, and deployment-level lifecycle
  evidence remain environment-dependent.
- [x] Validate staff-login credentials at the Identity HTTP boundary.
- [x] Adopt and verify Spring MVC endpoint version conditions for controllers,
  using one configured resolver and version-neutral `/api` routes.

### P1 — Cross-cutting ownership and infrastructure

- [x] Decide and record whether Audit is a real owned capability or should be
  retired; update the registry and dependencies.
- [x] Normalize Shared infrastructure after the Audit ownership decision,
  preserving rollback and repository-wide dependency evidence.
- [x] Complete Shared search integration/tenant-predicate evidence; the focused
  Shared and application Modulith/layer gates pass.
- [x] Remove H2 schema-drop/event-publication shutdown warnings from the shared
  test profiles and disable reusable PostgreSQL Testcontainers state so
  framework shutdown callbacks complete against a live database; explicitly
  order publication-registry shutdown before container shutdown.
- [ ] Close remaining shutdown-only diagnostics in every separately launched
  Spring context and drain Kafka publications before JVM shutdown. The
  Identity context is now clean after ordering publication cleanup before both
  tenant pools and the PostgreSQL container; lightweight contexts no longer run
  the PostgreSQL-only provisioning scheduler.
- [x] Run the service-wide dependency-cycle verification through the platform
  Modulith, layer, and application parity tests.

### P2 — Domain capabilities

- [x] Migrate Studio Documents using its approved public contracts and the
  current module template; final service-wide evidence remains.
- [x] Migrate Studio Subscriptions using its approved public contracts and the
  current module template; final service-wide evidence remains.
- [x] Migrate Assistant after Identity, Tenancy, and Shared contracts are
  stable; the canonical source layout and focused verification are complete.
- [x] Normalize Assistant conversation persistence and replace the legacy
  multi-operation conversation service with one use-case service per current
  conversation/action operation.
- [ ] Complete Assistant AI provider ports/adapters, WhatsApp participant
  ownership, webhook idempotency/signature evidence, and service-wide checks.
- [x] Complete Assistant WhatsApp signature verification, fail-closed secret
  handling, webhook package ownership, and sensitive-log reduction; replay,
  provider contract, tenant-account routing, and final evidence remain in the
  Assistant plan.
- [x] Complete Assistant WhatsApp tenant-account resolution and durable replay
  claim boundary; provider contract and live database evidence remain.
- [x] Complete Assistant package metadata coverage and remove unused AI helper
  classes after repository-wide reference verification.
- [x] Apply Bean Validation at Assistant conversation and pending-action web
  boundaries.
- [x] Remove `jakarta.persistence.EntityNotFoundException` from Studio
  application services and expose a Studio-owned public resource-not-found
  exception.
- [ ] Complete Assistant credentialed provider contracts, PostgreSQL replay and
  lifecycle evidence, and the final service-wide quality gate.

### P3 — Provider integrations

- [x] Migrate Notification with explicit provider ports and idempotency
  boundaries; retry, provider, and service-wide evidence remains.
- [x] Normalize Notification persistence, provider, event, configuration, and
  inbound adapter package boundaries.
- [x] Replace Notification's temporary multi-operation service with focused
  use-case services and application-owned delivery ports.
- [x] Keep Notification lifecycle rules exclusively in the framework-free
  domain aggregate; persistence entities contain state only.
- [ ] Complete Notification transient retry policy, credentialed provider
  execution, and service-wide integration verification.
- [x] Normalize SMS provider failures as typed exceptions so rejected or
  unreachable deliveries cannot be marked as delivered from an error string.
- [x] Migrate Payment after Subscription contracts are stable, preserving
  webhook signature/replay and transaction behavior; provider and final
  service-wide evidence remains.
- [x] Normalize Payment persistence, provider, configuration, and inbound
  webhook package boundaries.
- [x] Replace Payment's temporary multi-operation service with focused use-case
  services and application-owned persistence/provider ports.
- [x] Complete Payment webhook signature and durable replay/idempotency boundary;
  remaining provider contract, tenant-read, and full integration evidence is
  tracked in the Payment plan.
- [x] Apply Bean Validation at the Payment initiation HTTP boundary.

### Provider and event evidence checkpoint — 2026-08-03

- [x] Add deterministic MessageBird and Vonage provider contract tests for
  success payloads, authentication/request shape, and typed HTTP failures.
- [x] Make Kafka integration composition explicit by disabling external
  Keycloak provisioning in the isolated Kafka test profile; the test now exits
  with no outstanding Spring Modulith publications.

### Cleanup and verification checkpoint — 2026-08-03

- [x] Correct the duplicate Assistant `package-info.java` metadata that caused
      the boot-JAR duplicate-package warning.
- [x] Confirm Git does not track generated build output or empty directories.
- [x] Confirm the repository's existing dependency-analysis/build checks are
      the source of truth for unused dependencies.
- [x] Record the cleanup evidence and deletion policy in
      `docs/superpowers/reviews/2026-08-03-repository-cleanup-audit.md`.
- [ ] Continue deleting source files only when repository-wide references and
      architecture tests prove they are unreachable; do not remove valid
      capability ports, adapters, or public contracts based on name heuristics.
- [ ] Re-run service-wide quality, architecture, boot-JAR, and integration
      verification after the frontend recording slice is committed.

### P4 — Build-platform normalization

- [x] Create the build-logic CDD design specification and file-by-file
  implementation plan.
- [x] Add build-logic source/plugin inventory guardrails and normalize
  publishing task implementation names and unreleased task IDs.
- [x] Remove root-owned container configuration, isolate registry/container
  result models, and normalize container task implementation names while
  preserving registered Gradle task IDs.
- [x] Make container, deployment, and security provider selection lazy, typed,
  truthful, and actionable for unsupported values; verify the full build-logic
  check after the provider slices.
- [x] Add TestKit contracts for deployment and security provider selection and
  invalid-selector behavior.
- [x] Make publishing metadata task inputs valid under Gradle task validation
  and make Git ValueSources safe outside a checkout; verify publishing TestKit.
- [x] Add quality/API compatibility TestKit coverage and remove quality
  convention assumptions about an implicitly applied Java plugin.
- [x] Add capability composition TestKit coverage for foundation, testing,
  persistence, messaging, Modulith, fixtures, and Spring Web conventions.
- [x] Verify root lifecycle task registration and module-type/capability
  composition with TestKit.
- [x] Verify build-logic functional tests with configuration cache cold and
  reused runs.
- [x] Normalize every build-logic capability, convention script, binary plugin,
  extension, task, provider, result, model, ValueSource, and test according to
  the dedicated plan.
- [x] Add TestKit coverage for every binary plugin and convention family
  represented by the current build.
- [x] Remove eager configuration-time resolution, silent provider fallbacks, and
  inconsistent task/result names; preserve public plugin IDs and registered task
  names.
- [x] Verify configuration cache, task inputs/outputs, build-logic checks,
  service CI, and Markdown validation; publish the committed verification report.

### P4 cross-cutting event-streaming closure

- [x] Implement the Spring Modulith Kafka externalizer and JDBC publication
  registry in the dedicated messaging capability.
- [x] Execute `docs/superpowers/plans/2026-08-02-kafka-modulith-event-streaming-closure.md`.
- [x] Complete final Kafka/Modulith evidence: public event catalog, topic/key
  contract verification, consumer idempotency/replay behavior, failure/retry or
  dead-letter policy, production broker configuration, and CI/integration proof.
- [x] Keep RabbitMQ/AMQP absent from the unreleased codebase and documentation;
  Boot's required AMQP BOM verification metadata is retained without a runtime
  dependency.

### Optional native-image capability — 2026-08-03

- [x] Add `emme.native-image` as an opt-in capability-owned convention.
- [x] Pin and verify GraalVM Native Build Tools dependencies.
- [x] Add TestKit registration and no-fallback configuration coverage.
- [x] Document native/JVM rollout, measurement, and rollback controls.
- [x] Add a manual, opt-in GraalVM container workflow with immutable native
  tags and Trivy scanning.
- [ ] Execute the native executable and OCI image spike on a GraalVM/Docker
  runner before adopting native as the production artifact.

### Disposable test-container policy closure — 2026-08-03

- [x] Add a failing contract test proving optional Redis test containers are
  disposable and shared integration profiles do not enable reuse.
- [x] Remove the Redis `.withReuse(true)` setting and stale shared profile
  `testcontainers.reuse.enable` configuration.
- [x] Run focused container tests, relevant integration tests, and formatting;
  the container checks, platform parity test, and Identity integration test
  pass. The known external PostgreSQL/Testcontainers shutdown diagnostic is
  reproduced and remains tracked in the lifecycle verification evidence.

## Studio Documents canonical boundary slice — 2026-08-01

- [x] Add framework-free document and chunk domain models with lifecycle tests.
- [x] Move persistence types behind application-owned ports and adapters.
- [x] Extract document HTTP DTOs/mapper and move the controller to inbound
  adapter ownership.
- [x] Add package metadata and dependency-direction regression coverage.
- [x] Split application operations into one service per use case and complete
  grouped public contracts.
- [x] Expose only the Documents API through a Spring Modulith named interface.
- [x] Complete Documents-focused tests, Studio integration, and application
  Modulith verification; shared lifecycle warnings remain.

## Studio Subscriptions canonical migration — 2026-08-01

- [x] Extract framework-free subscription domain and entitlement policy.
- [x] Move persistence behind application-owned ports, entities, mappers, and adapters.
- [x] Group subscription API contracts and replace the multi-operation service.
- [x] Move HTTP DTOs/controller to canonical inbound adapter packages.
- [x] Update Identity, Studio, and test fixtures to canonical subscription contracts.
- [x] Expose only `subscriptions-api` through a Spring Modulith named interface.
- [x] Complete Studio integration and application Modulith verification; shared
  lifecycle warnings remain.
- [ ] Complete schema, security, recovery, and service-wide evidence.

### P5 — Final governance verification

- [ ] Perform the final configuration-properties normalization pass: prefer
  immutable records for stable constructor-bound groups, retain mutable classes
  only with a documented binder/framework reason, and apply startup validation
  only to real active-mode invariants (`@NotBlank`, `@NotNull`, numeric bounds,
  and conditional provider checks). Add focused properties tests and update the
  module registry after each conversion.
- [x] Run the repository-local service-wide architecture, Modulith, CI,
  integration, boot-artifact, documentation, formatting, and remote PR checks.
- [x] Commit the final evidence report at
  `docs/superpowers/reviews/2026-08-03-final-service-verification.md`.
- [ ] Execute environment-dependent security, provider, backup/restore,
  native-image measurement, broker-outage, and live pool-recovery gates before
  production deployment; these cannot be truthfully executed without the target
  environment and credentials.

Execution rules and dependencies are maintained in
`docs/superpowers/plans/README.md#remaining-execution-order-priority-and-type`.

## Documentation reconciliation checkpoint — 2026-08-01

- [x] Reconcile Calendar's historical TDD checklist with its completed status
  table, definition of done, source tree, and verification evidence.
- [x] Reconcile Tenancy's completed package/domain/application/web slices and
  leave only genuine port, typed-configuration, operational-evidence, and final
  verification gaps open.
- [x] Verify Tenancy unit tests, integration tests, and Studio Modulith tests.
- [x] Complete Catalog baseline verification before Identity's next
  implementation slice.

### Repository-wide package metadata closure — 2026-08-02

- [x] Add executable package-metadata guards for Studio Documents persistence
  adapters and Calendar application mappers.
- [x] Materialize the two remaining production `package-info.java` files.
- [x] Verify the focused Studio Documents and Calendar convention tests after
  a deliberate red phase.
- [x] Re-run the same metadata audit after the remaining build-logic and
  service-wide changes are complete, including the application composition-root
  configuration package.

Results are recorded in
`docs/superpowers/reviews/2026-08-03-package-metadata-verification.md`.

## One-service-per-use-case application boundary — 2026-08-01

- [x] Add failing Identity and Catalog architecture tests that reject
  multi-use-case application-service facades.
- [x] Split Identity membership operations into
  `AssignMembershipService`, `GetCurrentUserMembershipsService`, and
  `RevokeMembershipService`.
- [x] Split Identity feature-flag operations into one service per public use
  case while preserving the `featureFlagService` SpEL bean for evaluation.
- [x] Split Catalog item operations into one service per public use case.
- [x] Extract shared mapping/evaluation behavior into named collaborators
  instead of retaining a multi-use-case facade.
- [x] Update architecture documentation, naming conventions, and migration
  evidence to make the rule explicit.
- [ ] Run the final service-wide verification gate after the remaining module
  migrations are complete.

#### Results

The initial red architecture tests identified three multi-use-case facades:
Identity membership, Identity feature flags, and Catalog item operations. Each
was replaced with focused application services. The refactor was not required
to break a circular dependency; it enforces interface segregation, keeps
transactions and authorization boundaries explicit, and makes each use case
independently testable. Shared behavior now belongs to named mappers or
collaborators rather than a compatibility facade.

## Typed external configuration normalization — 2026-08-01

- [x] Add failing property and source-boundary tests for Calendar settings.
- [x] Bind `app.calendar.calendar-id` through `CalendarProperties` and inject
  it into Calendar application and Google outbound components.
- [x] Add failing property and source-boundary tests for Catalog image storage.
- [x] Bind `app.catalog.image-dir` through
  `CatalogImageStorageProperties` and inject it into the storage adapter.
- [x] Add failing property and source-boundary tests for WhatsApp settings.
- [x] Bind `app.whatsapp.*` through `WhatsAppProperties`, remove direct
  environment access from the WhatsApp application path, and expose the full
  property set in both deployable application configurations.
- [x] Bind Google Calendar service-account credentials and endpoints through
  `GoogleCalendarProperties`; no Calendar production adapter reads the process
  environment directly.
- [x] Normalize the Assistant Groq API key through `AiProperties`; no
  Assistant production AI provider reads the process environment directly.
- [x] Normalize Notification provider credentials and options through
  `NotificationProperties`; no Notification production provider reads the
  process environment directly.
- [x] Normalize Payment provider credentials and the Mercado Pago webhook
  secret through `PaymentProperties`; no Payment production provider or
  webhook reads the process environment directly.
- [ ] Run the final service-wide verification gate after those provider slices
  are complete.

#### Results

The first three property slices are green at unit, module, and integration
levels. Source checks now prevent new direct `@Value` injection in the
normalized components. Remaining provider environment reads are tracked
separately because each provider has a different existing property model and
secret-redaction contract.

## Identity role/permission domain boundary slice — 2026-08-01

- [x] Add failing domain, mapper, and package-ownership tests for `Role` and
  `Permission`.
- [x] Introduce framework-free `Role`, `Permission`, and `RoleScope` models.
- [x] Add persistence mappers and rewire role/permission adapters through the
  domain models without changing schema or permission results.
- [x] Remove the obsolete `RoleReference` port model after all consumers use
  the domain `Role`.
- [x] Verify Identity tests, Checkstyle, Spotless, integration tests, Modulith,
  CI, boot JARs, Markdown, and whitespace.

#### Results

- Red phase: the new source-tree and domain/mapper tests failed to compile
  because the canonical models and mappers did not yet exist.
- Green/refactor phase: focused domain and mapper tests passed after the
  framework-free models and persistence boundary were introduced.
- Full verification passed for Identity check/integration tests, Studio
  Modulith verification, service CI, both boot JARs, Markdown validation, and
  whitespace checks.
- Known non-blocking dependency-analysis warnings remain in the application
  projects because Spring Boot projects currently apply both `java-library` and
  `org.springframework.boot`.

## Tenancy typed database configuration slice — 2026-08-01

- [x] Add failing source and configuration tests for typed database credentials.
- [x] Introduce `TenantDatabaseConnectionProperties` under the canonical
  `configuration` package, bound to the existing `spring.datasource` keys.
- [x] Replace field-level `@Value` injection in `TenantDatabasePoolProvider`
  with constructor injection of the typed properties.
- [x] Verify Tenancy tests, Checkstyle, Spotless, integration tests, Modulith,
  Markdown, whitespace, CI, and boot JARs.

#### Results

- Red phase: the new source-boundary and properties tests failed to compile
  because `TenantDatabaseConnectionProperties` did not yet exist.
- Green/refactor phase: typed configuration tests and the source-boundary test
  passed after constructor injection replaced field-level `@Value` usage.
- Full verification passed for Tenancy tests/check/integration tests, Studio
  Modulith verification, service CI, both boot JARs, Markdown validation, and
  whitespace checks.
- Integration teardown continues to emit known H2/PostgreSQL and event-
  publication shutdown warnings after successful completion.

## Tenancy provisioning port boundary slice — 2026-08-01

- [x] Add failing process-manager, port-ownership, and adapter tests.
- [x] Introduce application-owned provisioning request and schema-migration
  ports.
- [x] Move JDBC registry lifecycle updates into an outbound persistence adapter.
- [x] Move schema creation and Liquibase execution into an outbound database
  adapter.
- [x] Keep scheduling, correlation, provisioning success/failure transitions,
  and error bounding in `TenantProvisioningProcessManager`.
- [x] Verify Tenancy tests, Checkstyle, Spotless, integration tests, Modulith,
  CI, boot JARs, Markdown, and whitespace.

#### Results

- Red phase: the new process-manager test failed to compile because the
  provisioning ports did not yet exist.
- Green/refactor phase: the process manager now depends only on application
  ports; JDBC and Liquibase implementations are outbound adapters.
- Full verification passed for Tenancy tests/check/integration tests, Studio
  Modulith verification, service CI, both boot JARs, Markdown validation, and
  whitespace checks.
- Integration teardown continues to emit known H2/PostgreSQL and event-
  publication shutdown warnings after successful completion.

## Tenancy provisioning service JDBC boundary slice — 2026-08-01

- [x] Add failing service and source-boundary tests for request/status access.
- [x] Extend `TenantProvisioningRepository` with request creation and status
  lookup capabilities.
- [x] Move request/status SQL into `JdbcTenantProvisioningRepository`.
- [x] Remove `JdbcTemplate` from `TenantProvisioningApplicationService`.
- [x] Verify Tenancy tests, Checkstyle, Spotless, integration tests, Modulith,
  CI, boot JARs, Markdown, and whitespace.

#### Results

- Red phase: the new service test failed to compile because request/status
  capabilities were not yet part of the provisioning repository port.
- Green/refactor phase: request creation and status mapping now use the
  application-owned port; JDBC remains only in the outbound adapter.
- The application implementation was normalized from
  `JdbcTenantProvisioningService` to `TenantProvisioningApplicationService`.
- Focused service/source tests and the full Tenancy/service verification gates
  passed.

## Tenancy pool failure baseline slice — 2026-08-01

- [x] Add deterministic tests for unresolved database routing and empty pool
  lifecycle shutdown.
- [x] Record the failure behavior as part of the Tenancy operational evidence.
- [x] Verify Tenancy tests, Checkstyle, Spotless, Markdown, and whitespace.

#### Results

- An unresolved registry entry fails with the database identifier in the
  exception before a pool is created.
- Empty pool state reports zero active pools and remains safe after shutdown.
- The remaining live routing, eviction, and recovery scenarios remain open for
  integration-level evidence.

## Tenancy event-after-commit boundary slice — 2026-08-01

- [x] Add failing consumer annotation and delegation tests.
- [x] Handle `TenantCreated` through Spring Modulith's application-module
  listener boundary.
- [x] Preserve Identity realm provisioning delegation and public event schema.
- [x] Verify Identity/Tenancy tests, Modulith, CI, boot JARs, Markdown, and
  whitespace.

### Results

- The source-boundary test first failed because the consumer used plain
  `@EventListener` instead of the Modulith listener boundary.
- The consumer now delegates through `@ApplicationModuleListener`, preserving
  the existing event payload and process-manager behavior.
- Identity/Tenancy tests and integration tests, Studio Modulith verification,
  service CI, both boot JARs, Markdown validation, and `git diff --check` passed.
- Existing dependency-analysis and Testcontainers/PostgreSQL teardown warnings
  remain non-blocking and occurred after successful task completion.

## Tenancy default-pool recovery slice — 2026-08-01

- [x] Add a failing regression test for recovery after the default pool closes.
- [x] Replace a closed default pool with a fresh pool on the next lookup.
- [x] Preserve the non-evictable default-pool behavior and safe shutdown.
- [x] Verify Tenancy tests, integration tests, Modulith, CI, boot JARs,
  Markdown, and whitespace.

### Results

- The regression test first failed because the provider returned the same closed
  Hikari pool after external shutdown.
- The provider now replaces the exact stale default-pool reference with a fresh
  pool, including the concurrent compare-and-set path.
- Focused Tenancy tests, Tenancy check/integration tests, Studio Modulith
  verification, service CI, both boot JARs, Markdown validation, and
  `git diff --check` passed.
- Existing dependency-analysis and Testcontainers/PostgreSQL teardown warnings
  remain non-blocking and occurred after successful task completion.

## Identity exception-advice boundary slice — 2026-08-01

- [x] Add a failing source-boundary test for controller-independent exception
  advice.
- [x] Scope Identity exception advice by the inbound web controller package,
  not by importing a concrete controller.
- [x] Preserve existing Identity problem-detail status and error-code behavior.
- [x] Verify Identity tests, Modulith, CI, boot JARs, Markdown, and whitespace.

### Results

- The source-boundary test first failed because `IdentityExceptionHandler`
  imported `IdentityController` through `basePackageClasses`.
- The advice now uses the controller package name, removing concrete
  controller coupling while preserving its RFC 9457 problem details.
- Identity tests/check/integration, Studio Modulith verification, service CI,
  both boot JARs, Markdown validation, and `git diff --check` passed.
- Existing dependency-analysis and Testcontainers/PostgreSQL teardown warnings
  remain non-blocking and occurred after successful task completion.

## Identity distributed login-rate-limit slice — 2026-08-01

- [x] Add failing tests for an application-owned login rate-limit port.
- [x] Move attempt state out of `LoginRateLimitFilter`.
- [x] Add an atomic Redis-backed adapter and a local fallback for environments
  without Redis.
- [x] Preserve trusted-proxy client-key resolution and HTTP 429 behavior.
- [x] Verify Identity security tests, Modulith, CI, boot JARs, Markdown, and
  whitespace.

### Results

- Red phase: tests failed to compile because the application-owned limiter port
  and injected filter boundary did not yet exist.
- Green/refactor phase: the filter now delegates to `LoginAttemptRateLimiter`,
  Redis uses an atomic increment-plus-expiry Lua script, and environments
  without Redis use an explicit local fallback.
- Identity tests/check/integration, Studio Modulith verification, service CI,
  both boot JARs, Markdown validation, and `git diff --check` passed.
- Existing dependency-analysis and Testcontainers/PostgreSQL teardown warnings
  remain non-blocking and occurred after successful task completion.

## Identity provisioning inbound-port slice — 2026-08-01

- [x] Add failing tests for the tenant-created consumer's inbound use-case
  dependency.
- [x] Expose `ProvisionTenantIdentityUseCase` under the grouped Identity API.
- [x] Make the provisioning process manager implement the use case.
- [x] Make the Modulith consumer depend on the use-case abstraction, preserving
  after-commit behavior and provisioning semantics.
- [x] Verify Identity security tests, Modulith, CI, boot JARs, Markdown, and
  whitespace.

### Results

- Red phase: the consumer test failed to compile because the inbound use-case
  contract did not yet exist.
- Green/refactor phase: the consumer now depends on
  `ProvisionTenantIdentityUseCase`; the process manager remains the concrete
  application implementation.
- Identity tests/check/integration, Studio Modulith verification, service CI,
  both boot JARs, Markdown validation, and `git diff --check` passed.
- Existing dependency-analysis and Testcontainers/PostgreSQL teardown warnings
  remain non-blocking and occurred after successful task completion.

## Identity membership web-boundary slice — 2026-08-01

- [x] Add failing tests preventing membership web adapters from importing
  application implementations or domain models.
- [x] Add grouped membership commands, query, use-case contracts, and result
  mapping.
- [x] Refactor Identity and current-user controllers to consume public use cases.
- [x] Preserve membership routes, status codes, response fields, and tenant
  selection behavior.
- [x] Verify Identity web/security tests, Modulith, CI, boot JARs, Markdown, and
  whitespace.

### Results

- Red phase: the source-boundary test failed because the membership controllers
  and web mapper imported `MembershipService` and the domain `Membership` type.
- Green/refactor phase: grouped membership commands, query, use cases, and
  `MembershipDetails` mapping now form the web-facing boundary. Controllers depend
  only on public use-case contracts and the web mapper depends only on the
  public result model.
- Membership routes, response status codes, response fields, and tenant
  selection behavior remain unchanged.
- Identity tests/check/integration, Studio Modulith verification, service CI,
  both boot JARs, Markdown validation, and `git diff --check` passed.
- Existing dependency-analysis and Testcontainers/PostgreSQL shutdown warnings
  remain non-blocking and occurred after successful task completion.

## Identity feature-flag web-boundary slice — 2026-08-01

- [x] Add failing tests preventing feature-flag web adapters from importing
  application implementations or domain models.
- [x] Add grouped feature-flag commands, query, use-case contracts, and result
  mapping.
- [x] Refactor platform and tenant feature-flag controllers to consume public
  use cases.
- [x] Preserve feature-flag routes, response fields, effective-value behavior,
  and authorization behavior.
- [x] Verify Identity tests/check/integration, Modulith, CI, boot JARs, Markdown,
  and whitespace.

### Results

- Red phase: the source-boundary test failed because both feature-flag
  controllers imported `FeatureFlagService`, and the web mapper imported the
  domain `FeatureFlag` model.
- Green/refactor phase: feature-flag commands, query, use cases, and public
  results now isolate the web adapters from application implementations and
  domain models. Effective values are returned through an immutable result.
- Feature-flag routes, JSON fields, effective override behavior, and platform
  admin authorization remain unchanged.
- Identity tests/check/integration, Studio Modulith verification, service CI,
  both boot JARs, Markdown validation, and `git diff --check` passed.
- Existing dependency-analysis and Testcontainers/PostgreSQL shutdown warnings
  remain non-blocking and occurred after successful task completion.

## Identity appointment-consumer inbound-port slice — 2026-08-01

- [x] Add a failing source-boundary test for the appointment consumer.
- [x] Add a grouped customer-membership command and use-case contract.
- [x] Make the application service implement the contract and decouple the
  consumer from the concrete service.
- [x] Preserve CUSTOMER-role filtering, JWT subject parsing, idempotency, and
  appointment event behavior.
- [x] Verify Identity tests/check/integration, Modulith, CI, boot JARs, Markdown,
  and whitespace.

### Results

- Red phase: the architecture test failed because the appointment consumer
  imported `EnsureCustomerMembershipService` directly.
- Green/refactor phase: the consumer now depends on the grouped
  `EnsureCustomerMembershipUseCase` and sends an
  `EnsureCustomerMembershipCommand`; the application service remains the
  idempotent implementation.
- CUSTOMER-role filtering, JWT subject parsing, and membership persistence
  behavior remain unchanged.
- Identity tests/check/integration, Studio Modulith verification, service CI,
  both boot JARs, Markdown validation, and `git diff --check` passed.
- Existing dependency-analysis and Testcontainers/PostgreSQL shutdown warnings
  remain non-blocking and occurred after successful task completion.

## Identity authentication-configuration port slice — 2026-08-01

- [x] Add a failing architecture test preventing application authentication
  services from importing Spring configuration properties.
- [x] Add an application-owned realm-configuration port and composition-root
  adapter.
- [x] Refactor `AuthenticateUserService` and its tests to consume the port.
- [x] Preserve platform and tenant realm selection behavior.
- [x] Verify Identity tests/check/integration, Modulith, CI, boot JARs, Markdown,
  and whitespace.

### Results

- Red phase: the architecture test failed because `AuthenticateUserService`
  imported `IdentityKeycloakProperties` directly.
- Green/refactor phase: the service now depends on the application-owned
  `IdentityRealmConfigurationPort`; `IdentityClientConfiguration` adapts the
  typed Spring properties at the composition root.
- Platform and tenant realm selection behavior remains unchanged.
- Identity tests/check/integration, Studio Modulith verification, service CI,
  both boot JARs, Markdown validation, and `git diff --check` passed.
- Existing dependency-analysis and Testcontainers/PostgreSQL shutdown warnings
  remain non-blocking and occurred after successful task completion.

## Identity provisioning-configuration port slice — 2026-08-01

- [x] Add a failing architecture test preventing provisioning orchestration
  from importing Spring configuration properties.
- [x] Add an application-owned provisioning configuration port and immutable
  settings model.
- [x] Adapt typed provisioning properties in the configuration root.
- [x] Refactor `KeycloakRealmProvisioningProcessManager` and its tests to consume
  the application boundary.
- [x] Preserve retry, validation, realm, client, role, and admin-user behavior.
- [x] Verify Identity tests/check/integration, Modulith, CI, boot JARs, Markdown,
  and whitespace.

### Results

- Red phase: the architecture test failed because
  `KeycloakRealmProvisioningProcessManager` imported
  `IdentityRealmProvisioningProperties` directly.
- Green/refactor phase: provisioning now consumes
  `IdentityRealmProvisioningConfigurationPort` and immutable
  `IdentityRealmProvisioningSettings`; Spring properties are mapped only in
  `IdentityProvisioningConfiguration`.
- Retry, validation, realm, client, role, and admin-user behavior remain
  unchanged.
- Identity tests/check/integration, Studio Modulith verification, service CI,
  both boot JARs, Markdown validation, and `git diff --check` passed.
- Existing dependency-analysis and Testcontainers/PostgreSQL shutdown warnings
  remain non-blocking and occurred after successful task completion.

## Identity provisioning command boundary slice — 2026-08-01

- [x] Add a failing architecture test preventing the Identity use case from
  importing the Tenancy event transport type.
- [x] Add `ProvisionTenantIdentityCommand` to the grouped Identity API.
- [x] Map `TenantCreated` to the Identity command in the inbound consumer.
- [x] Refactor the provisioning use case and process manager to consume the
  Identity-owned command.
- [x] Preserve after-commit provisioning, realm naming, and all provisioning
  payload values.
- [x] Verify focused Identity tests, formatting, and source-boundary checks.

### Results

- Red phase: the architecture test failed because
  `ProvisionTenantIdentityUseCase` imported `TenantCreated` directly.
- Green/refactor phase: the inbound consumer now translates the cross-module
  event into `ProvisionTenantIdentityCommand`; Identity application code no
  longer depends on the Tenancy event transport type.
- The `@ApplicationModuleListener` after-commit entry point and provisioning
  behavior remain unchanged.
- Focused Identity tests and Spotless formatting passed.

## Identity unreleased API cleanup — 2026-08-01

- [x] Remove the legacy `IdentityApi` use-case contract and
  `IdentityApiService` implementation.
- [x] Remove the temporary all-memberships compatibility query/use-case that
  existed only to preserve the legacy implementation.
- [x] Add an executable source-tree rule rejecting the removed legacy API
  files.
- [x] Record the unreleased-system rule in the architecture handbook,
  templates, naming catalog, and engineering lessons.
- [x] Verify Identity compilation/tests, Modulith, service CI, both boot JARs,
  formatting, Markdown, and whitespace.

### Results

- The unreleased service now uses only canonical grouped API contracts; no
  compatibility alias or legacy Identity API implementation remains.
- The architecture rule is explicit: compatibility layers require an external
  released consumer, persisted/serialized contract, or approved migration
  window with documented evidence.

## Identity tenant-realm outbound port slice — 2026-08-01

- [x] Add a failing architecture test preventing provisioning orchestration
  from importing the Tenancy application service contract directly.
- [x] Add the application-owned `TenantIdentityRealmPort`.
- [x] Add `TenantIdentityRealmAdapter` under the outbound module adapter and
  delegate to the Tenancy public contract there.
- [x] Refactor `KeycloakRealmProvisioningProcessManager` and tests to consume
  the outbound port.
- [x] Add adapter delegation regression coverage and package metadata.
- [x] Verify focused Identity tests and Spotless formatting.

### Results

- Red phase: the source-boundary test and adapter test failed because the
  provisioning process had no application port or adapter implementation.
- Green/refactor phase: Tenancy realm updates now cross the explicit
  `TenantIdentityRealmPort`; only the outbound adapter imports `TenantApi`.
- Realm provisioning behavior and after-commit listener ownership remain
  unchanged.

## Studio vertical slices — 2026-07-31

- [x] Appointment domain lifecycle and persistence boundary migrated.
- [x] Collision detection uses an application-owned port.
- [x] Operating hours, business profile, and booking policy use domain models
  and application-owned persistence ports.
- [x] Appointment event publication uses an application-owned port and adapter.
- [x] `SalonApiImpl` no longer imports Spring Data or persistence entities.
- [x] Dashboard SSE transport is owned by `adapter.in.web.sse`.
- [x] Application-layer ArchUnit guardrail passes.
- [x] Public cross-module use-case normalization and full service CI are complete.
- [ ] Migrate `documents` and `subscriptions` only after their public contracts
  and ownership boundaries are explicitly designed.

## Calendar vertical slice — 2026-07-31

- [x] Calendar canonical package migration complete.
- [x] Calendar domain has no framework imports.
- [x] Calendar public contracts are grouped by API kind.
- [x] Calendar persistence entities are isolated behind application-owned ports.
- [x] Calendar application services do not depend on concrete outbound adapters.
- [x] Calendar service and focused architecture/persistence tests pass.
- [x] Web Calendar/Google error handling preserves stable problem codes.
- [x] Web Calendar/Google messages are localized in supported locales.
- [x] Full cross-repository final commit and remote verification.

## Assistant canonical module migration — 2026-07-31

- [ ] Execute `docs/superpowers/plans/2026-07-31-assistant-module-template-migration.md`.
- [ ] Keep the latest `docs/templates/module-package-structure-template.md` authoritative.
- [x] Normalize Assistant persistence names, inbound adapter paths, and initial
  domain/API package boundaries.
- [x] Replace temporary entity-backed orchestration with focused use-case
  services and application-owned ports.
- [x] Isolate Assistant AI providers behind `ai/application/port/out` and
  `ai/adapter/out/provider`, with focused chat, intent, and RAG use cases.
- [ ] Preserve Assistant HTTP, webhook, JSON, database, and feature-flag behavior.
- [ ] Separate pure domain models, persistence entities, ports, adapters, grouped API contracts, and package metadata.
- [ ] Run the complete Assistant and service verification gates before merging.

### Assistant tenant-scoped lookup correction — 2026-08-02

- [x] Add tenant identity to existing-record Assistant commands and queries.
- [x] Enforce tenant-qualified predicates in conversation, event, and pending-action ports and adapters.
- [x] Wrap all conversation and pending-action HTTP routes with `withCurrentTenant`.
- [x] Add source-boundary regression coverage for identifier-only persistence reads.
- [x] Verify `:modules:assistant:check` with zero failures and zero skipped tests.
- [ ] Complete live provider contract, PostgreSQL replay, and service-wide verification evidence.

### Assistant unsupported-embedding contract — 2026-08-02

- [x] Add a regression test for Groq's unsupported embedding capability.
- [x] Return an empty embedding result instead of a persisted zero vector.
- [x] Verify focused Assistant test and formatting.
- [ ] Complete live provider contract, PostgreSQL replay, and service-wide
  verification evidence.

## Module migration plan registry — 2026-07-31

- [x] Normalize contract-only plans for `customer`, `workforce`, and `booking`.
- [x] Keep Calendar and core Studio plans explicitly marked conformance-complete;
  track Studio `documents` and `subscriptions` separately.
- [x] Create canonical migration plans for `identity`, `tenancy`, `notification`,
  `payment`, `audit`, and `shared`.
- [x] Keep Catalog as the verified implementation baseline and do not treat the
  CDD build-logic plan as a business-module migration.
- [ ] Run service-wide architecture verification after every module plan reaches
  implementation completion.

### Identity Membership domain/application slice — 2026-08-01

- [x] Add failing package guardrails for the Membership domain and persistence
  boundary.
- [x] Introduce framework-free Membership lifecycle behavior.
- [x] Add application-owned membership/role ports and MembershipService.
- [x] Add persistence mapper and adapters while preserving managed JPA identity.
- [x] Rewire Identity membership/current-user/public API flows.
- [x] Verify Identity tests and mapper round-trip coverage.
- [ ] Continue with permission/identity service separation and typed security
  configuration.

### Identity permission application slice — 2026-08-01

- [x] Add failing tests for the permission use case and package boundary.
- [x] Introduce `PermissionPort` and `GetUserPermissionsUseCase`.
- [x] Move permission traversal into `PermissionPersistenceAdapter`.
- [x] Rewire permission consumers and remove legacy `IdentityService`.
- [x] Verify unit, integration, architecture, formatting, and Modulith gates.
- [ ] Continue with customer authentication and customer-membership event
  application separation.

### Identity Feature Flag application slice — 2026-08-01

- [x] Add failing domain, application, and package-boundary tests.
- [x] Introduce the Feature Flag domain model and application repository ports.
- [x] Isolate JPA persistence behind entity, mapper, and adapter types.
- [x] Isolate subscription plan lookup behind `SubscriptionPlanPort`.
- [x] Preserve the SpEL bean name and feature-flag HTTP behavior.
- [x] Verify Identity tests and Assistant test compilation.
- [x] Continue with customer-membership event application separation.

### Identity customer-membership event slice — 2026-08-01

- [x] Add failing tests for the membership event application boundary.
- [x] Introduce the framework-free CustomerMembership model and repository port.
- [x] Move idempotent membership creation into EnsureCustomerMembershipService.
- [x] Rename and isolate the composite-key JPA entity and Spring Data repository.
- [x] Move appointment event handling to the inbound messaging consumer package.
- [x] Verify Identity tests, architecture checks, and affected application tests.
- [x] Continue with customer authentication application separation.

### Identity customer authentication slice — 2026-08-01

- [x] Add failing tests for customer identity domain and use-case boundaries.
- [x] Introduce public customer authentication/profile commands, results, and
  use cases.
- [x] Move provider-token decoding and customer identity persistence behind
  application-owned ports.
- [x] Rename CustomerIdentity technical persistence types and add mapper/adapter
  implementations.
- [x] Rewire AuthController without exposing JPA entities.
- [x] Verify Identity checks and login/profile regression coverage.
- [x] Continue with typed security configuration and Identity failure advice.

### Identity typed security configuration slice — 2026-08-01

- [x] Add failing tests for typed security defaults and package ownership.
- [x] Introduce IdentitySecurityProperties with safe local defaults.
- [x] Rewire SecurityConfiguration to consume typed properties.
- [x] Verify Identity checks and security configuration regression coverage.
- [x] Continue with Identity-specific failure advice.

### Identity failure advice slice — 2026-08-01

- [x] Add failing tests for Identity-owned expected exceptions and ProblemDetail
  mapping.
- [x] Introduce public customer authentication/profile exception types.
- [x] Add scoped Identity web advice without replacing shared global handling.
- [x] Rewire application services to raise stable Identity failures.
- [x] Verify Identity checks and HTTP error regression coverage.
- [x] Continue with final Identity security hardening and Keycloak application
  boundary separation.

### Identity Keycloak application boundary slice — 2026-08-01

- [x] Add failing tests for password-grant orchestration behind an application
  use case and outbound port.
- [x] Introduce typed user-authentication commands, queries, and results.
- [x] Move OkHttp/Jackson/Keycloak user authentication into an outbound adapter.
- [x] Rewire AuthController through the application use case.
- [x] Remove the legacy application KeycloakAuthService.
- [x] Verify Identity checks, login regression coverage, and Modulith boundaries.

### Identity typed Keycloak configuration slice — 2026-08-01

- [x] Add failing tests for typed user/admin client settings and adapter
  ownership.
- [x] Extend `IdentityKeycloakProperties` across user authentication, admin
  provisioning, issuer, realm, and client settings.
- [x] Inject the configured Identity HTTP client into both Keycloak adapters.
- [x] Update application, local, platform, integration-test, and shared-test
  fixture configuration.
- [x] Verify focused Identity regressions, full Identity checks, Studio
  Modulith verification, Markdown validation, and whitespace checks.

Remaining Identity follow-up: complete the broader security hardening review.

### Identity realm provisioning hardening slice — 2026-08-01

- [x] Add failing tests for missing provisioning credentials and configurable
  retry behavior.
- [x] Move realm client, redirect URI, role, admin-user, and retry settings into
  `IdentityRealmProvisioningProperties`.
- [x] Remove the production hardcoded tenant-user password and validate the
  provisioning password before contacting the provider.
- [x] Inject retry delay behavior through `RetryDelayPort` so tests do not block
  on real sleeps.
- [x] Remove the master admin password default and expose environment-backed
  configuration for runtime profiles.
- [x] Verify Identity checks, Studio Modulith verification, Markdown validation,
  whitespace checks, and the source-level secret guard.

Remaining Identity follow-up: complete the broader security hardening review.

### Identity realm provisioning port slice — 2026-08-01

- [x] Add failing process and package-boundary tests for the outbound
  administration capability.
- [x] Introduce `IdentityProviderAdministrationPort` under
  `application/port/out`.
- [x] Rewire `KeycloakRealmProvisioningProcessManager` to depend only on the
  application port.
- [x] Keep Keycloak HTTP administration inside the outbound adapter.
- [x] Verify full Identity checks, Studio Modulith verification, Markdown
  validation, and whitespace checks.

Remaining Identity follow-up: complete the broader security hardening review.

### Plan update results

- Added the plan registry at `docs/superpowers/plans/README.md`.
- Added canonical plans for Customer, Workforce, Booking, Identity, Tenancy,
  Notification, Payment, Audit, Shared, Catalog baseline verification, Studio
  Documents, and Studio Subscriptions.
- Updated Calendar and Studio plans with current-template conformance notes.
- Corrected the service migration design so Identity and Tenancy are not falsely
  reported as completed baselines.

## Contract-only module implementation slice — 2026-07-31

- [x] Normalized Customer's empty API namespace and retained its root Modulith
  metadata.
- [x] Normalized Workforce's empty API namespace and retained its root Modulith
  metadata.
- [x] Removed Booking's obsolete top-level `events` metadata and stale named
  interface dependencies; retained only actual shared/tenancy dependencies.
- [x] Added source-tree convention tests for all three contract-only modules.
- [x] Verified focused module tests and `applications:emme-platform` Modulith tests.
- [ ] Continue with the next dependency-safe migration slice from the registry
  (Identity/Tenancy security and persistence inventory).

## Identity/Tenancy contract boundary slice — 2026-07-31

- [x] Added failing package-boundary tests for grouped public contracts and
  normalized event naming.
- [x] Grouped Identity use-case/results and Tenancy use-case/results under the
  current module template.
- [x] Renamed `TenantCreatedEvent` to `TenantCreated` and updated its consumer.
- [x] Preserved existing Modulith named-interface identifiers and dependency
  semantics while moving package ownership.
- [x] Verified focused tests, full Identity/Tenancy module tests, and Studio
  Modulith verification.
- [ ] Continue with Identity security/domain/persistence separation and Tenancy
  isolation/provisioning separation as separate red-green-refactor slices.

## Identity/Tenancy persistence ownership slice — 2026-07-31

- [x] Added failing tests requiring persistence types to live under outbound
  adapter ownership.
- [x] Moved Identity entities/enums and Spring Data repositories under
  `adapter/out/persistence`.
- [x] Moved Tenancy entities/enums and repositories under
  `adapter/out/persistence`, plus bootstrap registry access under
  `adapter/out/client/database`.
- [x] Updated all production, test, and fixture imports; no legacy entity package
  Java sources remain.
- [x] Verified full Identity/Tenancy tests, Checkstyle, Spotless, compilation,
  and Studio Modulith tests.
- [x] Introduced the Tenancy application-owned repository port, pure Tenant
  aggregate, persistence entity/mapper/adapter, and updated all callers.
- [x] Verified focused domain/mapper/repository tests plus full Tenancy check and
  Studio Modulith verification.
- [x] Moved Tenancy orchestration into `application/service` and renamed the
  scheduled worker to `application/process/TenantProvisioningProcessManager`.
- [x] Verified all Tenancy web/module/repository tests, Checkstyle, Spotless, and
  Studio Modulith verification after Spring proxy wiring was preserved.
- [x] Moved Tenancy controllers, HTTP request/response models, web mapper,
  request-context filters, trusted resolver, rate limiting, and MVC configuration
  into canonical inbound/configuration packages.
- [x] Verified full Tenancy tests, Checkstyle, Spotless, and Studio Modulith
  verification after the inbound adapter migration.
- [x] Moved `TenantContextAspect` under the outbound persistence aspect package
  and normalized `DataSourceConfiguration`/`TenantPoolingProperties` under
  `configuration`.
- [x] Introduced `DatabaseRegistryPort` and immutable `DatabaseRegistryEntry`,
  renamed the JDBC implementation to `DatabaseRegistryAdapter`, and moved the
  pool/routing datasource under the database client adapter with
  `TenantDatabasePoolProvider`.
- [x] Moved Identity security configuration to `configuration/SecurityConfiguration`.
- [x] Moved the login filter, Keycloak clients/JWT decoder, and security audit
  observer into canonical inbound/outbound adapter packages.
- [x] Split tenant-created realm provisioning into an inbound event consumer and
  an application process manager.
- [x] Verified Identity tests/checks and Studio Modulith verification after the
  security boundary migration.
- [x] Moved Identity controllers and the web test into
  `adapter/in/web/controller`.
- [x] Extracted named request/response records and web mappers under the inbound
  web adapter, preserving existing HTTP contracts.
- [x] Verified the full Identity test suite after the HTTP boundary migration.
- [ ] Next slice: continue Identity application/domain separation and failure
  advice.

## Architecture naming contract — 2026-07-31

- [x] Added the canonical naming catalog at
  `docs/architecture/00-project/naming-conventions.md`.
- [x] Documented naming for packages, files, classes, records, enums, interfaces,
  exceptions, methods, fields, constants, module contracts, adapters,
  repositories, controllers, events, tests, and CDD build-logic types.
- [x] Linked all 33 other architecture Markdown files to the canonical catalog.
- [x] Direct Markdown validation passes with `node scripts/validate-markdown.mjs`.
- [ ] Run the `mise run docs-check` wrapper after the local `mise.toml` trust
  decision is made; the underlying validator already passes.

## Validation conventions — 2026-08-01

- [x] Add the canonical backend validation page for Jakarta Bean Validation,
  records, custom cross-field constraints, domain/application ownership, i18n,
  error mapping, naming, and tests.
- [x] Link validation guidance from the handbook, backend API/controller pages,
  module template, and naming catalog.
- [x] Align Tenancy create/update request records with the persisted slug/name
  bounds using `@Size` in the inbound adapter.
- [x] Add focused validation regression coverage and verify the existing web
  boundary still rejects an oversized slug before persistence.
- [x] Direct Markdown validation passes with `node scripts/validate-markdown.mjs`.
- [x] Run the complete Tenancy check and service Modulith verification before
  committing this slice.

### Identity security audit hardening slice — 2026-08-01

- [x] Add failing regression tests for exception-message redaction,
  control-character sanitization, bounded audit values, and forwarded-header
  spoofing.
- [x] Log authentication failure types instead of exception messages so secrets
  and provider details cannot enter audit output.
- [x] Sanitize and bound user-controlled audit values before structured fields
  are written to logs.
- [x] Use the socket peer address for audit IP attribution; retain the separate
  forwarded-header/rate-limit policy as an explicit follow-up decision.
- [x] Verify focused audit tests, full Identity checks, Studio Modulith,
  Markdown validation, whitespace, and source-level secret scanning.

Remaining Identity follow-up: decide trusted proxy handling for login rate
limiting, then continue authorization domain/application separation.

## Catalog canonical baseline verification slice — 2026-08-01

- [x] Added package-level metadata and a convention test for every materialized
  Catalog package while preserving the Modulith root and named API annotations.
- [x] Hid Shared hybrid search behind Catalog-owned `CatalogSearchPort` and
  `HybridCatalogSearchAdapter`, with mapping coverage.
- [x] Corrected the shared JDBC connection-details boundary so Testcontainers
  `@ServiceConnection` integration tests receive the bootstrap URL.
- [x] Removed proxy-blocking `final` declarations from Identity persistence
  adapters and corrected stale Studio tenant accessor tests discovered by CI.
- [x] Verified Catalog tests/integration, Studio Modulith, service CI, boot JARs,
  Markdown validation, source-boundary checks, and whitespace.
- [x] Updated the plan registry and Catalog verification report.

## Identity trusted-proxy rate-limit slice — 2026-08-01

- [x] Add failing tests for typed rate-limit settings and forwarded-header
  spoofing resistance.
- [x] Replace `@Value` rate-limit fields with `IdentityRateLimitProperties`.
- [x] Accept `X-Forwarded-For` only when the immediate peer matches configured
  trusted proxy networks; preserve the remote address as the secure default.
- [x] Document the decision in an ADR and the Identity migration plan.
- [x] Verify focused Identity tests, full Identity checks, Identity integration,
  Modulith, CI, Markdown, whitespace, and boot-JAR gates.

### Results

- Focused typed-properties and forwarded-header filter tests passed.
- `:modules:identity:check` passed.
- `:modules:identity:integrationTest` passed; teardown emitted existing
  PostgreSQL/Testcontainers shutdown I/O warnings after test completion.
- Studio Modulith verification, service CI, both application boot JARs,
  Markdown validation, and `git diff --check` passed.

## Identity persistence entity naming slice — 2026-08-01

- [x] Add and run the failing source-tree test for normalized `*Entity` names.
- [x] Rename Identity role/permission JPA types and update all repository,
  adapter, mapper, fixture, and integration-test references.
- [x] Verify Identity tests, integration tests, Modulith, CI, boot JARs,
  Markdown, and whitespace.

### Results

- The convention test first failed on the missing normalized entity files.
- Identity unit tests, Checkstyle, Spotless, and integration tests passed.
- Studio Modulith verification, service CI, both application boot JARs,
  Markdown validation, and `git diff --check` passed.
- Integration teardown emitted existing Testcontainers/PostgreSQL shutdown
  warnings after successful test completion.

## Identity inbound security-context ownership slice — 2026-08-01

- [x] Add and run the failing source-tree test for moving `UserContext` and
  `UserContextHolder` out of the Identity root package.
- [x] Move the security-context types under `adapter/in/web/security` and
  update Identity and Calendar consumers without changing behavior.
- [x] Verify Identity tests, Calendar tests, Modulith, CI, boot JARs,
  Markdown, and whitespace.

### Results

- The source-tree test first failed because the canonical security package was
  absent.
- Moved `UserContext` and `UserContextHolder` under inbound web security and
  exposed only the `identity-security` named interface for Calendar.
- Identity and Calendar unit/integration checks, Modulith verification, service
  CI, both boot JARs, Markdown validation, and whitespace checks passed.
- Testcontainers teardown emitted existing database/prune warnings after
  successful completion.

## Identity authorization wiring separation slice — 2026-08-01

- [x] Add and run failing tests for extracted role-authority mapping and
  configuration ownership.
- [x] Move JWT/OIDC authority mapping and role hierarchy wiring into dedicated
  Identity authorization components.
- [x] Keep `SecurityConfiguration` focused on filter-chain and transport
  security wiring without changing role names or access behavior.
- [x] Verify Identity security tests, integration tests, Modulith, CI, boot
  JARs, Markdown, and whitespace.

### Results

- The source-tree convention test first failed because the canonical
  authorization components were absent.
- Extracted role claim parsing, JWT conversion, OAuth2/OIDC authority mapping,
  role hierarchy, and method-security wiring from `SecurityConfiguration`.
- Preserved `ROLE_` prefixing, the existing role hierarchy, OIDC `userinfo`
  support, and all existing filter-chain behavior.
- Identity unit/check/integration tests, Studio Modulith verification, service
  CI, both application boot JARs, Markdown validation, and `git diff --check`
  passed.
- Integration teardown emitted existing PostgreSQL/Testcontainers shutdown
  warnings after successful completion.

## Focused use-case and library-boundary normalization slice — 2026-08-01

- [x] Isolated Assistant channel-participant persistence behind an application
  repository port, mapper, and outbound adapter.
- [x] Replaced Tenancy's multi-operation `TenantService` and `TenantApi` with
  grouped commands/queries/results and one service per use case.
- [x] Split tenant provisioning request and status into separate use-case
  services and updated Identity, inbound adapters, and test fixtures.
- [x] Updated the library architecture page to remove the retired
  `libraries/contracts` model and document module-owned named APIs.
- [x] Verified Assistant, Tenancy, and Identity checks plus Identity integration
  tests; shutdown-only Testcontainers/H2 warnings remain documented.
- [ ] Continue with remaining operational evidence, provider/webhook replay and
  idempotency coverage, then run service-wide Modulith, CI, boot-JAR, security,
  and recovery verification.

## Calendar outbound-boundary correction — 2026-08-01

- [x] Added application ports for Google Sheets export and spreadsheet-link
  queries.
- [x] Mapped spreadsheet entities to public result records.
- [x] Moved SheetsController to the canonical inbound controller package and
  removed direct outbound adapter imports.
- [x] Moved Tenancy audit persistence behind an application-owned port and
  outbound persistence adapter.
- [x] Verified focused Calendar and Tenancy boundary tests.

## Studio one-use-case-per-service normalization — 2026-08-01

- [x] Add a failing architecture test for focused Studio use-case services and
  removal of aggregate service/facade names.
- [x] Replace `SalonApi` with focused business-profile, appointment-list, and
  customer-list contracts.
- [x] Split customer, artist, service-catalog, business-configuration, and
  appointment operations into focused application services.
- [x] Update Studio controllers, Identity, and Calendar consumers to use
  canonical use-case interfaces.
- [x] Verify Studio Checkstyle, Spotless, unit tests, and integration tests.

## Calendar one-use-case-per-service normalization — 2026-08-01

- [x] Add a failing architecture test for focused Calendar use-case services
  and removal of aggregate service/facade names.
- [x] Split busy-time and synchronization operations into individual services.
- [x] Split calendar-link queries and lifecycle mutations into individual
  public use-case contracts and services.
- [x] Update Calendar controllers and Google synchronization adapters to use
  focused contracts.
- [x] Verify Calendar Checkstyle, Spotless, unit tests, and integration tests.

## Audit ownership and service naming normalization — 2026-08-01

- [x] Close the Audit decision-only plan using ADR 0004 and keep the module
  metadata-only because Identity and Tenancy own current audit responsibilities.
- [x] Rename the internal Tenancy audit coordinator to
  `AuditEventRecorder` under `application/audit` and remove the generic
  `AuditService` name.
- [x] Verify the Tenancy boundary and package convention tests.

## Studio service-catalog naming normalization — 2026-08-01

- [x] Replace awkward `*CatalogServiceService` names with explicit
  `*ServiceCatalogEntryService` names.
- [x] Align the corresponding use-case interface names and controller imports.
- [x] Verify the Studio package convention test and formatting.

## Assistant AI and WhatsApp adapter normalization — 2026-08-01

- [x] Move AI provider implementations into technology-owned
  `adapter/out/provider/{mock,groq,ollama}` packages.
- [x] Extract AI HTTP request records and response records from the controller.
- [x] Inject the capability-owned AI transport wrapper and shared JSON mapper;
  provider implementations no longer construct transport dependencies.
- [x] Replace the legacy WhatsApp orchestration service with the focused
  `ProcessWhatsAppMessageUseCase` and `ProcessWhatsAppMessageService`.
- [x] Move webhook JSON parsing into `WhatsAppWebhookMapper` and outbound
  Graph API delivery behind `WhatsAppReplyPort`/`WhatsAppReplyAdapter`.
- [x] Add mapper, malformed-payload, status-update, replay-claim, and duplicate
  delivery tests.
- [x] Run the complete Assistant module test suite successfully.
- [ ] Add live provider contract and PostgreSQL replay evidence in the final
  service verification gate.

## Technology-owned outbound client normalization — 2026-08-01

- [x] Move Notification email, SMS, and push implementations from the generic
  `adapter/out/client` namespace into `adapter/out/provider/{email,sms,push}`.
- [x] Move Payment providers into `adapter/out/provider/{conekta,mercadopago,
  paypal,stripe,mock}` packages.
- [x] Move Calendar OAuth support into `adapter/out/google/oauth`.
- [x] Add package metadata and update source-boundary tests for each client
  family.
- [x] Verify focused Notification, Payment, and Calendar tests.

## Provider composition-root hardening — 2026-08-01

- [x] Add red source-boundary tests preventing Notification and Payment providers
  from constructing HTTP clients or JSON mappers internally.
- [x] Add capability-owned `NotificationHttpClient` and `PaymentHttpClient`
  composition-root beans.
- [x] Inject transport and serialization dependencies into all Notification and
  Payment HTTP provider implementations.
- [x] Verify focused Notification and Payment convention tests and whitespace.
- [ ] Add deterministic provider contract tests for each externally integrated
  technology and retain live credentialed checks as deployment evidence.

## Provider namespace and public event normalization — 2026-08-02

- [x] Use `adapter/out/provider` for concrete capability providers.
- [x] Reserve `adapter/out/client` for transport-focused clients such as the
  Assistant WhatsApp Graph adapter.
- [x] Normalize `NotificationDeliveredEvent` to `NotificationDelivered`.
- [x] Update Studio event consumers and tests to the normalized public event.
- [x] Verify Assistant, Notification, and Studio compilation/checks.
- [ ] Complete deterministic provider contract, retry, and live integration evidence.

## Payment provider namespace normalization — 2026-08-02

- [x] Move Payment provider implementations to `adapter/out/provider`.
- [x] Keep `PaymentHttpClient` as transport infrastructure under configuration.
- [x] Update provider package metadata and contract-test locations.
- [x] Verify Payment unit, module, formatting, and integration checks.
- [ ] Complete provider contract depth, webhook replay/signature evidence, and service-wide verification.

## Notification unsupported-channel delivery guard — 2026-08-02

- [x] Add a failing regression test proving unsupported channels must not be
  marked delivered.
- [x] Fail explicitly for unsupported `WHATSAPP` and `WEB` delivery paths so
  the existing application failure path persists `FAILED`.
- [x] Verify focused Notification delivery tests.
- [ ] Continue deterministic provider contracts, retry/idempotency, and live
  integration evidence.

## Shared capability package normalization — 2026-08-02

- [x] Move `BaseEntity` and `TenantOwnedEntity` to `shared.persistence`.
- [x] Move `ClockProvider` to `shared.time` and `IdGenerator` to `shared.identity`.
- [x] Update all module imports and shared tests; remove legacy root-package primitive locations.
- [x] Verify Shared formatting and unit tests.
- [x] Complete PostgreSQL vector/full-text evidence with tenant-scoped and bounded integration coverage.
- [x] Complete dependency-cycle verification; retain the remaining
  service-wide operational gates as the final verification track.

## Audit reserved-boundary hardening — 2026-08-02

- [x] Remove unnecessary implementation dependencies from the metadata-only Audit project.
- [x] Set Audit's Modulith allowed dependencies to empty.
- [x] Replace the placeholder structure test with a real no-scaffolding invariant.
- [x] Verify Audit and application Modulith tests.
- [ ] Implement Audit only through a separately approved ownership, retention, and data-classification design.

## Calendar Google transport composition-root hardening — 2026-08-01

- [x] Add a red source-boundary test preventing Google adapters and clients from
  constructing `OkHttpClient` internally.
- [x] Add capability-owned `GoogleHttpClient` and configuration wiring.
- [x] Inject the transport boundary into Calendar OAuth, Calendar, Sheets, and
  synchronization adapters.
- [x] Verify Calendar package conventions and formatting-sensitive whitespace.

## Shared test-profile DRY normalization — 2026-08-02

- [x] Confirm duplicate REST and PostgreSQL integration profiles were identical
  across all participating modules.
- [x] Move `application-resttest.yml`, `application-integration-test.yml`, and
  `intTest-schema.sql` into the shared testing test-fixture resources.
- [x] Remove module-local duplicate profile and schema resources.
- [x] Normalize all Spring Modulith JDBC schema properties to the current
  nested namespace.
- [x] Compile every integration-test source set and package the shared fixture
  JAR successfully.
- [x] Document profile ownership and the build-logic CDD completion gate.

## Shared managed JDBC connection template — 2026-08-02

- [x] Define generic throwing connection callbacks with explicit result and
  checked-failure type parameters.
- [x] Provide the Shared `JdbcConnectionExecutor` higher-order boundary backed
  by Spring-managed connection lifecycle.
- [x] Use separate `withConnection` and `consumeWithConnection` operations to
  keep result-producing and side-effecting overloads unambiguous.
- [x] Migrate Tenancy Liquibase schema migration to the Shared executor.
- [x] Add focused unit tests and Shared tenant-scoped search integration tests.
- [x] Verify no production `DataSource#getConnection()` remains in Shared or
  Tenancy connection-owned code.
- [ ] Extend the same boundary to any later adapter that still performs manual
  connection management during the final service-wide audit.

## Tenancy and Studio Documents verification — 2026-08-02

- [x] Make the scheduled Tenancy provisioning read boundary retry-safe when
  the registry/database is temporarily unavailable.
- [x] Remove the unscoped Documents repository lookup and use tenant-scoped
  lookup during persistence saves.
- [x] Keep document lifecycle transitions exclusively in the framework-free
  domain aggregate; persistence entities contain mappings only.
- [x] Verify Documents unit/module checks and PostgreSQL integration startup.
- [ ] Continue the remaining service-wide Modulith, schema, and recovery gate.

## Studio Subscriptions verification — 2026-08-02

- [x] Replace the unscoped subscription save lookup with
  `findByTenantIdAndId(tenantId, subscriptionId)`.
- [x] Add an executable package-boundary regression test preventing future
  subscription persistence regressions.
- [ ] Continue the remaining service-wide payment-boundary, Modulith, schema,
  and recovery gate.

## Identity current-user application boundary — 2026-08-02

- [x] Add grouped current-user query, use-case, and immutable result contracts.
- [x] Extract membership, tenant, permission, and profile aggregation into one
  focused `GetCurrentUserService`.
- [x] Make both current-user and login inbound controllers delegate through the
  application use case.
- [x] Remove controller-to-controller coupling and the unused legacy mapper.
- [x] Verify focused tests, Identity checks/integration, and application
  Modulith verification.
- [ ] Continue the remaining Identity operational evidence and service-wide
  architecture gates.

## Identity platform feature-flag listing — 2026-08-02

- [x] Replace the platform-admin feature-flag listing placeholder with a
  dedicated public use case and one application service.
- [x] Keep global flag retrieval behind the Identity application repository port.
- [x] Add unit and MockMvc module coverage for returned global flags.
- [ ] Include the endpoint in the final service-wide security and boot-artifact
  verification gate.

## Tenancy deterministic routing boundary — 2026-08-02

- [x] Verify default-database fallback without tenant context.
- [x] Verify tenant database routing-key preservation.
- [x] Verify lazy target resolution through the pool provider.
- [x] Reverify default-pool lifecycle and closed-pool recovery tests.
- [ ] Complete live eviction, routing-failure recovery, replay/idempotency,
  rollback, and service-wide operational evidence.

## Spring MVC endpoint versioning — 2026-08-02

- [x] Configure one header-based `ApiVersionConfigurer` using `API-Version`.
- [x] Use version-neutral `/api` routes and default requests to version `1.0`.
- [x] Declare the Identity controller's current mapping as Spring MVC version
  `1.0`.
- [x] Normalize module controllers, e2e clients, operational scripts,
  performance workloads, Kubernetes configuration, and test fixtures to the
  canonical version-neutral `/api` routes.
- [x] Resolve the tenant provisioning route collision exposed by removing the
  URI version segment.
- [x] Verify module source conventions, Checkstyle, integration checks, and
  application Modulith tests.
- [ ] Add parallel `1.1` representations only when a real second
  representation exists; no legacy `/api/v1` alias is maintained before
  release.

## Assistant Documents-backed RAG — 2026-08-02

- [x] Add the public Studio Documents search query and use-case contract.
- [x] Keep document search behind an application-owned outbound port and
  tenant-scoped persistence hydration.
- [x] Replace the real-provider RAG placeholder with embedding, search, context
  assembly, and model-chat orchestration.
- [x] Preserve keyword-only retrieval for unavailable embeddings and mock-mode
  canned behavior.
- [x] Verify focused Assistant/Studio tests, Spotless, Checkstyle, and module
  checks.
- [ ] Run PostgreSQL/Testcontainers vector/full-text and provider contract
  evidence during the final service-wide verification gate.

## Tenancy e2e CRUD contract — 2026-08-02

- [x] Align the e2e tenant update client with the server's `PATCH` route.
- [x] Add a first-class `UserSession.patch` helper.
- [x] Remove the `405` compatibility escape so the CRUD test fails on a broken
  update endpoint.
- [x] Verify `compileE2eTestJava`.

## Notification module verification — 2026-08-02

- [x] Run Notification compile, unit, and integration tests.
- [x] Verify canonical package, provider composition, tenant scope,
  idempotency, and unsupported-channel behavior.
- [ ] Add deeper transient-provider retry and credentialed provider contracts.
- [ ] Resolve shutdown-time PostgreSQL/Event Publication connection lifecycle
  warnings before final service-wide readiness.

## Studio nested capability reconciliation — 2026-08-02

- [x] Reconcile Documents plan status with the tenant-safe persistence and
  Assistant-facing search-port implementation.
- [x] Reconcile Subscriptions plan status with its canonical domain,
  application, persistence, and inbound adapter slices.
- [x] Remove the stale Studio-root wording that described both capabilities as
  deferred.
- [ ] Complete shared service-wide schema, recovery, Modulith, and boot-artifact
  evidence.

## Shared infrastructure reconciliation — 2026-08-02

- [x] Reconcile capability-owned persistence, time, identity, search, web, and
  JDBC connection boundaries.
- [x] Verify repository test, Shared integration, formatting, Checkstyle, and
  application Modulith gates.
- [ ] Add final repository-wide recovery/rollback evidence for Shared changes.

## Developer workflow and verification gates — 2026-08-03

### Goal

Make local developer commands and GitHub Actions use the same production-grade
backend gates: Spotless validation, Modulith/ArchUnit boundaries, JaCoCo,
security checks, integration tests, infrastructure validation, and packaging.

### Decisions

- [x] Keep Gradle as the service build and hook implementation; do not add a
      second Node-based Husky runtime to the Java repository.
- [x] Use committed `.githooks` scripts installed through Mise.
- [x] `spotlessApply` is an explicit formatter; hooks and CI validate with
      `spotlessCheck`.
- [x] Expose separate Mise tasks for formatting, architecture, coverage,
      security, tests, integration, and the complete quality lifecycle.
- [x] Keep Modulith and ArchUnit boundary tests blocking in local verification
      and pull-request Actions.
- [x] Keep JaCoCo thresholds honest and ratcheted from measured coverage; do
      not make a repository-wide 70% gate green by excluding business code.

### Implementation and verification

- [x] Add service-native pre-commit and pre-push hooks plus `mise run hooks-install`.
- [x] Add explicit `spotlessCheck`, Modulith/ArchUnit, JaCoCo report, and
      coverage-verification tasks.
- [x] Normalize Mise task names and make CI delegate to them where practical.
- [x] Improve backend workflow job and step names without weakening gates.
- [x] Add Dependabot/security metadata and preserve Gitleaks/OWASP controls.
- [x] Run the backend gates serially locally to avoid concurrent Gradle report
      writer collisions.
- [ ] Push and verify all required GitHub Actions checks.

### Follow-up observations

- [ ] Investigate non-failing Testcontainers prune and application-context
      shutdown warnings emitted by the integration suite.

## Selectable parallel CI refactor — 2026-08-04

- [x] Document the safe parallel-execution design and manual pipeline inputs.
- [x] Create service and web implementation plans.
- [x] Add the backend workflow contract validator.
- [x] Run backend tests and JaCoCo coverage in one Gradle job locally.
- [x] Apply the remaining service workflow input and container-trigger changes.
- [x] Add the reusable web Bun setup action and optional workflow inputs.
- [x] Document CI event modes, parallel job graphs, E2E flow, and reuse boundaries with Mermaid diagrams.
- [ ] Verify both repositories' changed workflows in GitHub Actions.

## Public contract naming normalization — 2026-08-04

- [x] Establish `Status` as the canonical lifecycle/current-condition suffix and
  `Type` as the canonical classification suffix; reject `State`, `Kind`, and
  `StatusView` for new public contracts.
- [x] Establish `Details`, `Summary`, and `Page` as the canonical public read
  shapes; reserve `Result` for non-resource operation outcomes and `View` for
  explicit CQRS/read projections.
- [x] Rename ambiguous public `*Info`/`*View` contracts across Assistant,
  Calendar, Catalog, Identity, Notification, Payment, Studio, and Tenancy.
- [x] Rename the Identity claims query from `GetUserInfoQuery` to
  `GetUserClaimsQuery` and align the use-case/port operation to `getUserClaims`.
- [x] Remove the duplicate Identity `BusinessProfileSummary`; Identity now
  consumes the Studio-owned public summary contract.
- [x] Add executable naming guards for public API result/type suffixes and
  verify module tests, Modulith/ArchUnit tests, Spotless, and stale-name scans.

### Results

The naming source of truth is
`docs/architecture/00-project/naming-conventions.md`. The module and application
templates now use the same vocabulary, so future modules do not need to choose
between `Info`, `View`, `StatusView`, `State`, or `Kind` on a case-by-case basis.

## Enterprise conformance inventory — 2026-08-04

- [x] Refresh production Java type and application-service counts after the
  public contract rename migration.
- [x] Record package metadata, named-interface, direct-connection, repository,
  and DDD/Hexagonal boundary evidence.
- [x] Document remaining inventory gaps separately from verified repository facts.
- [x] Link the evidence from the enterprise conformance plan:
  `docs/superpowers/reviews/2026-08-04-enterprise-module-template-conformance-inventory.md`.

## Named-interface architecture guardrail — 2026-08-04

- [x] Add a failing repository architecture test for named-interface structure.
- [x] Add reusable `NamedInterfaceRules` for non-empty declarations and
  canonical API named-interface suffixes.
- [x] Preserve deliberate technical named interfaces such as `persistence`,
  `persistence-jdbc`, `search`, `identity`, and `time`.
- [x] Verify the new guardrail with the platform test suite and Spotless.

## Cross-module dependency architecture guardrail — 2026-08-04

- [x] Add a failing platform test for business-module implementation imports.
- [x] Add reusable `ModuleDependencyRules` that permits cross-module API
  contracts while rejecting domain, application, adapter, and configuration
  imports.
- [x] Move Calendar's generic authenticated-subject lookup from Identity's web
  adapter into Shared web security infrastructure.
- [x] Verify the focused Shared and platform architecture tests pass.
- [x] Continue with per-service transaction-mode and dependency-count review;
  the completed audit is recorded below.

## Application-service transaction and dependency audit — 2026-08-04

- [x] Inventory all 123 application services and their transaction policies.
- [x] Add a red/green guardrail for one explicit transaction policy per
  application service, with documented external-only exemptions.
- [x] Add read-only transaction boundaries to `GetCurrentUserService` and
  `GetGoogleOAuthStatusService`.
- [x] Review constructor dependency hotspots without introducing artificial
  helper/manager classes or violating one-service-per-use-case.
- [x] Record evidence in
      `docs/superpowers/reviews/2026-08-04-application-service-audit.md`.

## Canonical E2E user extension and runtime verification — 2026-08-04

- [x] Review the former `studio-api` `@WithUser` lifecycle and precedence
      model.
- [x] Add repeatable `@WithUser` and `E2eUserExtension` support to the current
      `emme-platform` E2E source set.
- [x] Add immutable `E2eUsers` record support for multiple configured users.
- [x] Give each `UserSession` an explicit bearer token and canonical
      `API-Version: 1.0` header.
- [x] Derive the active tenant from the token claim instead of hard-coding a
      historical tenant UUID in E2E tests.
- [x] Prevent tenant RLS advice from intercepting the core tenant-registry
      repository outside a transaction.
- [x] Normalize stale E2E expectations for current API routes and configured
      mock providers.
- [x] Run authenticated service E2E against the local disposable stack:
      `45 tests completed, BUILD SUCCESSFUL`.
- [x] Run the tenancy convention guardrail and E2E source compilation.
- [x] Add the base i18n bundle used when a locale-specific bundle is not
      available, preventing runtime message-source warnings for locales such as
      `en-MX`.
- [ ] Replace remaining functional-wrapper test bodies with direct extension
      parameter injection in a separate cleanup slice; the single `e2eTest`
      task and runtime flow are already canonical.
## Google identity configuration tasks — 2026-08-10

- [x] Confirm the customer OAuth JSON and the separate tenant OAuth contract
- [x] Add a Gradle task for customer Google OIDC broker configuration
- [x] Add a Gradle task for tenant salon/studio Google OAuth preflight/configuration
- [x] Keep credential JSON and secret values outside the repository and logs
- [x] Add task documentation and safe usage examples
- [x] Run Gradle task discovery, focused tests, and formatting checks

### Working notes

- Customer login uses the Google Web OAuth client in `emme-customers` through
  Keycloak identity brokering and OIDC.
- Salon/studio Google OAuth is the backend Calendar/Workspace integration. Its
  client is injected as `GOOGLE_OAUTH_CLIENT_ID` and
  `GOOGLE_OAUTH_CLIENT_SECRET`; it is not a Keycloak customer identity
  provider.
- The downloaded customer file is available at
  `/Users/miguelangeldelgadillozarate/Downloads/client_secret_412839253574-a3s0vlp1fubvrtsh1s5bgbr64ro3vh9l.apps.googleusercontent.com.json`.
- The tenant file is available at
  `/Users/miguelangeldelgadillozarate/Downloads/client_secret_2_412839253574-0dq3rh025vl0mheecj6gb04bqjkntoq3.apps.googleusercontent.com.json`.
- The tenant file currently contains `https://emme-studio.com`, but the
  service callback is `http://localhost:8080/api/google/oauth/callback`; Google
  Cloud must authorize the exact callback before `configureTenantOAuth` can
  pass.
- Verification: `configureCustomerOidc`, build-logic tests, task discovery, and
  build-logic formatting pass. The repository-wide `spotlessCheck` remains
  blocked by the pre-existing `modules/subscriptions/.../SubscriptionProvisioningListener.java`
  violation; it was not changed.
- Preserve the pre-existing unstaged `TenantRegistryEntity.java` change.

## AI platform architecture and implementation — 2026-08-27

- [x] Inspect the existing Java, Spring Modulith, AI, tenancy, vector, Redis,
      messaging, and observability architecture.
- [x] Decide to keep the AI platform inside the `emme-service` deployable
      boundary.
- [x] Define semantic classification, semantic tool selection, and semantic
      caching as separate vector capabilities.
- [x] Define Java 25, ScopedValue, StructuredTaskScope, Joiner, executor, and
      Java-agent boundaries.
- [x] Define Spring AI and LangGraph4j responsibility boundaries.
- [x] Create the draft AI platform documentation index and master design.
- [x] Create draft PRD, requirements, NFRs, functional specification, TSPEC,
      data model, evaluation specification, runbook, ADRs, FCRs, and phased
      implementation plan.
- [x] Review and approve the complete documentation set.
- [x] Implement Phase 0 Java 25 baseline using TDD.
- [x] Add named AI executors: virtual-thread I/O, bounded platform pools, and
      scheduled maintenance with validated configuration.
- [x] Add the stable `ParallelTaskRunner` port and Java 25 structured runner
      with required, optional, and first-success Joiner policies.
- [x] Add the ScopedValue-to-ThreadLocal/MDC compatibility bridge and install
      it automatically for structured AI subtasks.
- [x] Add the framework-free semantic embedding/ranking core with model and
      dimension validation, confidence/margin abstention, and authorized-key
      filtering for future classifier/tool/cache adapters.
- [ ] Implement Phase 1–2 concurrency foundation using TDD.
- [ ] Implement the AI foundation, semantic capabilities, workflow, HITL,
      evaluation, and operational phases incrementally.

### Working notes

- The current worktree contains an unrelated unstaged tenancy edit; it must not
  be staged with AI documentation or implementation changes.
- Java 25 is declared in the repository and `mise run toolchain:jvm` now
  validates the selected runtime. A shell with Java 17 still fails fast with an
  actionable message when the validator is invoked directly.
- The explicit native lane correctly stops before Gradle when the selected JDK
  does not provide `native-image`; GraalVM Native Image 25 remains a host/CI
  prerequisite for that lane.
- `StructuredTaskScope` and `Joiner` are Java 25 preview APIs and must remain
  behind a stable Emme abstraction.
- Shared Compose and Testcontainers Redis use the pinned ARM64-compatible
  `redis:8.10.1-alpine3.23` image required by the opt-in Redis vector adapter;
  PostgreSQL/pgvector remains the durable semantic store.
- Java agents are intended for JVM observability and diagnostics, not global
  ForkJoinPool replacement.

## AI contracts and platform implementation plan — 2026-08-28

- [x] Validate the implementation plan against the approved AI architecture.
- [x] Save the detailed plan under `docs/superpowers/plans/`.
- [ ] Reconcile the existing AI documentation and legacy `ai-foundation` naming.
- [ ] Add and test the `ai-contracts` and `ai-platform` Gradle boundaries.
- [ ] Define framework-neutral AI contracts and migrate catalog consumers.
- [ ] Standardize Java 25 tenant and AI context propagation with ScopedValue,
      bridges, StructuredTaskScope, and Joiners.
- [ ] Persist durable conversations, workflow runs, checkpoints, approvals,
      quotes, tool calls, traces, and audit events in PostgreSQL.
- [ ] Move Spring AI providers, specialized clients, advisors, semantic routing,
      semantic tool selection, and semantic caching into `ai-platform`.
- [ ] Add the generic LangGraph4j runtime with PostgreSQL checkpoints and
      idempotent pause/resume.
- [ ] Implement quote-by-image, deterministic pricing, HITL review, and
      appointment workflows in `assistant`.
- [ ] Add tenant-safe pgvector RAG, Redis operations, channels, and async jobs.
- [ ] Add optional Apache AGE projection and curated tenant-scoped traversals.
- [ ] Add redacted observability traces, offline Ragas evaluation, E2E tests,
      operational documentation, and final verification.

### Working notes

- The detailed plan keeps Spring AI as the model/tool execution layer and
  LangGraph4j as the single durable workflow orchestration layer.
- The approved storage decision uses Redis Stack for bounded hot semantic
  intent/tool indexes and short-lived safe response caching, with PostgreSQL
  and pgvector as the durable canonical/fallback vector store.
- The initial local text embedding is Ollama `embeddinggemma:300m` at the
  verified 768-dimensional profile; vector indexes are never mixed across
  model versions. Gemma 4 `e4b-mlx` is the default local chat/vision profile.
- The first implementation slice is the module boundary and contracts; no
  production code should be moved before architecture tests protect the
  dependency direction.
- Existing unstaged tenancy changes remain outside this work and must not be
  included in AI commits.

## AI learning-candidate safety boundary — 2026-08-29

- [x] Reject common email, phone, and Bearer-token patterns from candidate
      reference text at the framework-free contract boundary.
- [x] Enforce candidate text lengths against the durable PostgreSQL schema.
- [x] Add focused regression tests for PII and length validation.
- [x] Run the focused `ai-contracts` test suite.

### Results

- `LearningCandidate` now fails closed before policy admission when reference
  text is not redacted or exceeds the persisted column limit.
- Verification: `:libraries:ai-contracts:test --tests
  com.emme.ai.contracts.learning.LearningCandidateTest` passed.

## AI learning-candidate lifecycle — 2026-08-29

- [x] Add typed candidate statuses and offline evaluation gates.
- [x] Add deterministic promotion/rollback lifecycle policy.
- [x] Add tenant-filtered, optimistic-lock state persistence.
- [x] Add lifecycle service and assistant configuration wiring.
- [x] Update AI implementation plan and technical specification.
- [x] Run focused lifecycle, JDBC, configuration, formatting, and contract
      checks.

### Results

- Candidate state transitions are now durable and concurrency-safe. Failed
  evaluation gates move candidates to `REJECTED`; canary failure leaves an
  approved candidate unchanged; promotion is an explicit later operation.
- Verification: lifecycle tests, JDBC state-store tests, assistant wiring test,
  and module `spotlessCheck` passed.

### Verification follow-up

- [x] Repair two provider-fallback fixtures to bind the required backend AI
      context after the broad assistant check exposed the regression.
- [x] Re-run `ChatServiceProviderFallbackTest` successfully.

### Lifecycle gate hardening

- [x] Add regression coverage for incomplete promotion evaluation results.
- [x] Recheck dataset, safety, regression, shadow, and canary gates before
      promotion.
- [x] Re-run the lifecycle policy tests successfully.

### Results

- Detailed task ordering, exact file areas, TDD checkpoints, dependencies,
  verification commands, risks, and definition of done are recorded in
  `docs/superpowers/plans/2026-08-28-ai-platform-implementation-plan.md`.

## Redis semantic acceleration — 2026-08-29

- [x] Add the official Spring AI Redis VectorStore as an opt-in hot projection
      for durable semantic-cache reads, with PostgreSQL hit confirmation.
- [x] Add the official Spring AI progressive tool-search advisor over a separate
      Redis vector index with composite tenant/principal/role session scoping.
- [x] Align partial embedding configuration defaults with the active
      EmbeddingGemma 768-dimensional schema contract.

### Results

- Redis acceleration is disabled by default and remains rebuildable. Durable
  PostgreSQL writes happen first; Redis failures are treated as optimization
  failures only.
- Spring AI's tool-search advisor replaces the default tool advisor only when
  the opt-in Redis index and backend-approved callback provider are present.
- Verification: focused Redis, tool-search, semantic-cache, chat-configuration,
  and embedding-property tests passed; Spotless passed.

## Redis vector runtime alignment — 2026-08-29

- [x] Pin the shared Compose Redis runtime to the Redis Query Engine image
      required by the opt-in Spring AI Redis vector adapters.
- [x] Align the Redis Testcontainers fixture with the local runtime.
- [x] Repair stale E2E Compose contract expectations exposed by validation.
- [x] Validate E2E/Kafka Compose contracts and merged Compose configuration.

### Results

- Compose and Testcontainers now use `redis:8.10.1-alpine3.23`, pinned for the
  Apple Silicon development path and Redis vector-search support.
- Semantic Redis remains disabled by default; PostgreSQL/pgvector remains the
  durable fallback and source of truth.
- Verification: E2E and Kafka Compose contracts passed; merged regression
  Compose configuration passed `config --quiet`.

## Redis projection expiry — 2026-08-29

- [x] Apply the durable semantic-cache expiry to the Redis JSON projection key.
- [x] Keep the metadata expiry filter and PostgreSQL hit confirmation as
      correctness safeguards.
- [x] Verify TTL projection and Redis semantic configuration tests.

### Results

- Redis hot-cache entries now receive a key TTL derived from the authoritative
  PostgreSQL cache expiry; the existing constructor remains available for
  infrastructure-free unit compositions.
- Verification: focused Redis hot-store/configuration tests and assistant
  Spotless checks passed under Java 25.

## pgvector runtime integration — 2026-08-29

- [x] Add a pinned pgvector Testcontainers integration harness.
- [x] Verify tenant-filtered semantic reference search against the live vector
      extension.
- [x] Verify durable semantic-cache write, similarity read, and hit accounting.
- [x] Keep the test independent of the full tenant/JPA application bootstrap.

### Results

- `PgVectorSemanticIntegrationTest` now runs against
  `pgvector/pgvector:0.8.6-pg16-trixie` and passes both tenant isolation and
  cache persistence scenarios.
- Verification: `:modules:assistant:integrationTest --tests
  com.emme.assistant.ai.PgVectorSemanticIntegrationTest` passed locally with
  Testcontainers.

## Authorized mutation idempotency — 2026-08-29

- [x] Add a typed idempotency port and gateway replay/concurrency behavior.
- [x] Release failed mutation claims while preserving the original failure if
      cleanup also fails.
- [x] Persist claims and authoritative results in tenant-scoped PostgreSQL
      with RLS and changelog coverage.
- [x] Select JDBC persistence when the tenant-aware JDBC boundary exists and a
      no-op implementation only for infrastructure-free compositions.
- [x] Run focused gateway, JDBC adapter, configuration, database contract, and
      formatting checks.

### Results

Authorized mutation tools now derive an operation key from the backend tool key,
authenticated principal, and `AiExecutionContext` idempotency key. Completed replays return the durable
result without running the handler again; concurrent claims are rejected;
handler failures release claims for retry. Existing appointment commands still
need an application-level idempotency-aware mutation contract before concrete
appointment mutation tools are registered.

## Task 5 reconciliation final-review remediation — 2026-08-31

- [x] Trace the durable job claim and scheduled reconciliation data flow.
- [x] Add failing tests for atomic reconciliation claims, tenant context, and duplicate prevention.
- [x] Implement the smallest durable claim transition and authoritative tenant iteration.
- [x] Add executable PostgreSQL/Testcontainers coverage, or document the exact runtime blocker.
- [x] Run Java 25 tests and Spotless; update `.superpowers/sdd/task-5-report.md`.
- [x] Commit, push, and verify remote hashes.

### Working Notes

- Existing reconciliation selected rows with `FOR UPDATE SKIP LOCKED` outside a transaction and without a tenant context; the lock therefore did not claim durable work.
- `modules/assistant` already applies the repository Testcontainers integration convention, so the remediation can add a focused executable PostgreSQL test.

### Results

- Reconciliation uses an atomic transactional claim transition and dispatches through an already-claimed worker path.
- Scheduled work is enumerated from active tenants and binds an authoritative backend AI execution context for each tenant; JDBC establishes the PostgreSQL RLS setting within each transaction.
- `AiJobReconciliationClaimIntegrationTest` passes against PostgreSQL 16 in Testcontainers using a non-superuser role with forced RLS, covering duplicate claim prevention and tenant isolation.
- Focused Java 25 unit tests and assistant Spotless checks pass. The full assistant test task remains blocked by the pre-existing unrelated `AiCapabilityConventionTest` storage-package metadata failure; no unrelated fix was included.

## Task 5 final-review closure — 2026-08-31

- [x] Execute only canonical durable job payload/context after claim.
- [x] Force row-level security in the job migration and verify it through the
      non-superuser Testcontainers runtime role.
- [x] Add live retry timing/progression and `DEAD_LETTER` coverage.
- [x] Add metadata for all new production packages and document unrelated
      package/application-context convention blockers.
- [x] Add minimal injected Micrometer job lifecycle, queue, and tenant fairness
      metrics while preserving deferred Redis and real handlers.
- [x] Verify focused Java 25 tests and Spotless; update the Task 5 report.

### Results

- Focused job unit, migration-contract, and PostgreSQL/Testcontainers tests pass.
- Full assistant tests remain blocked by the pre-existing storage package
  metadata violation and missing `TenantImageReader` application-context bean;
  neither unrelated issue was changed.

## Task 5 final review final closure — 2026-08-31

- [x] Remove unauthorized core-schema references from the assistant JDBC adapter and reconciliation integration test through the established core search-path boundary.
- [x] Add explicit Spring Boot constructor binding and binding regression coverage for `AiJobProperties`.
- [x] Run focused Java 25 architecture, configuration, job, migration, PostgreSQL/Testcontainers, compilation, and Spotless checks.
- [x] Update `.superpowers/sdd/task-5-report.md` with the final-review evidence.

### Results

- `SchemaOwnershipTest`, `AiJobPropertiesTest`, focused job/migration tests, and the three-test live reconciliation suite pass.
- Assistant production and integration-test compilation plus assistant Spotless pass.
- Unrelated existing worktree changes were not staged or modified.

## Task 5 final review remediation — 2026-08-31

- [x] Move `ai_job_state` to the core Liquibase changeset/run path with valid formatted SQL metadata and no studio duplication.
- [x] Make AI scheduling honor `spring.task.scheduling.enabled` through the central conditional configuration and profile/context coverage.
- [x] Atomically defer rejected reconciliation claims with durable next availability and verify tenant alternation/rejection behavior.
- [x] Add queue-lag and claim-duration metrics without tenant-cardinality labels.
- [x] Qualify job JDBC/transaction wiring to the core datasource and add a competing-bean context test.
- [x] Run focused Java 25 tests, integration tests, Spotless, update the Task 5 report, commit, and push.

### Working Notes

- The existing worktree has unrelated edits; only files required by this remediation will be staged.
- Existing canonical reload, RLS, retry/DLQ, Redis/live-event deferral, and deferred handler behavior are out of scope for redesign.

### Results

- Core-owned migration, conditional scheduling, rejected-claim deferral, queue/claim timers, and qualified core JDBC wiring are implemented and covered by focused tests.
- Java 25 focused unit, architecture, migration, context/DI, compilation, Spotless, and PostgreSQL/Testcontainers checks pass.
- Existing unrelated worktree edits remain unstaged and unchanged.

## AI platform simplification consolidation — 2026-09-03

- [x] Reconcile the in-progress Task 6/7 AI contract, RAG, tenant-security,
      learning, and durable-job changes with the simplification blueprint.
- [x] Keep framework-neutral provider contracts behind an active
      `AiExecutionContext` and preserve fail-closed tenant boundaries.
- [x] Remove architecture-test violations caused by misplaced job/configuration
      classes and expose only stable appointments, tenancy, and subscription APIs.
- [x] Run the final consolidated local lint, test, integration compilation,
      architecture, and regression validation pass.
- [ ] Run container-backed integration/startup validation when Docker and
      PostgreSQL are available; run deployed E2E when `EMME_E2E_BASE_URL` is set.

### Working notes

- Final validation was run after the implementation edits were written, per the
  requested workflow.
- `./gradlew check --no-parallel --no-configuration-cache` passes (251 tasks).
- `git diff --check` passes; focused provider, RAG, tenancy, appointment,
  assistant, architecture, migration, and contract checks pass.
- Container-backed integration is blocked by the unavailable Docker daemon and
  local PostgreSQL (`localhost:5432` refused). Deployed E2E is blocked because
  `EMME_E2E_BASE_URL` is not configured. The tests remain strict and unchanged.

## AI platform activation follow-up — 2026-09-04

- [x] Make workflow response ownership explicit so an enabled LangGraph workflow
      cannot execute a second chat-model request after producing a response.
- [x] Fail fast when the LangGraph feature is enabled without real conversation
      capability wiring; keep placeholder capabilities out of production types.
- [x] Move minimal workflow fixtures into test sources and remove the package-private
      production graph factory (`fea4b4b6`).
- [x] Define the bounded model/tool-agent policy before considering the optional
      `langgraph4j-spring-ai` bridge.
- [x] Run focused feedback after each slice; defer the full enterprise gate,
      container startup, and deployed E2E checks to the final phase.
- [x] Run the final enterprise gate after implementation; `./gradlew check
      --no-parallel --no-configuration-cache` passes with 251 tasks.

### Working Notes

- Existing uncommitted canonical-contract migration files are preserved and will
  be staged separately from the workflow response slice.
- LangGraph4j core remains the durable outer workflow owner. Spring AI remains
  the model, retrieval, and tool-mechanics owner.
- `b353e906` makes the durable workflow response authoritative and prevents a
  second chat-model execution. `1d155e27` makes incomplete enabled workflow
  wiring fail fast. `fea4b4b6` removes production placeholder capabilities and
  keeps explicit minimal capabilities in test sources only.
- Concurrent identity, tenancy, and subscriptions migration files remain
  unstaged and were not included in the AI workflow commit.
- The final enterprise gate passed on 2026-09-04. Docker-backed integration,
  local PostgreSQL startup, and deployed E2E remain blocked only by unavailable
  environment services/configuration.
