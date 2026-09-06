# Engineering lessons

## 2026-09-06 — Keep notification lifecycle enums through HTTP mapping

- Failure mode: Notification converted its public delivery status to a string
  in the HTTP response even though the API already owned `NotificationStatus`.
- Detection signal: a boundary convention test found the raw `String status`
  component in `NotificationResponse`.
- Prevention rule: preserve notification lifecycle enums through application
  and HTTP mapping, while stable enum names remain the serialized API values.

## 2026-09-06 — Normalize external payment statuses at the application edge

- Failure mode: Payment converted its internal lifecycle status to a string in
  the HTTP response, while provider results also use strings for external
  vocabularies.
- Detection signal: the boundary convention test found raw `String status` in
  `PaymentResponse`, while the provider port correctly required normalization.
- Prevention rule: preserve the internal `PaymentStatus` enum through public
  application and HTTP models; keep provider status strings isolated to the
  provider port and normalize them before domain mutation.

## 2026-09-06 — Keep Assistant state enums through response mapping

- Failure mode: Assistant conversation and pending-action response records
  converted finite API states to strings, and older cross-module fixtures still
  passed strings after the owning modules became enum-typed.
- Detection signal: the new response convention test failed, followed by test
  compilation errors in four Assistant fixtures.
- Prevention rule: preserve API-owned state enums through web mapping and update
  dependent fixtures in the same compatibility slice whenever a shared result
  contract becomes strongly typed.

## 2026-09-06 — Keep appointment lifecycle enums through API DTOs

- Failure mode: Appointments converted `AppointmentStatus` to a string in
  result mappers and the controller response DTO.
- Detection signal: a boundary convention test found raw status components in
  both appointment read models.
- Prevention rule: preserve the domain appointment enum through application and
  HTTP mapping, with stable-name serialization at the external edge.

## 2026-09-06 — Keep document lifecycle enums through read models

- Failure mode: Documents converted `DocumentStatus` to a string before
  returning application and HTTP models.
- Detection signal: a boundary convention test found raw status components
  despite an existing document lifecycle enum.
- Prevention rule: preserve the domain enum through Documents result and web
  mapping, serializing only at the external boundary.

## 2026-09-06 — Keep service-catalog lifecycle enums through mapping

- Failure mode: Services converted `ServiceStatus` and `ArtistStatus` to
  strings before returning public results and HTTP responses.
- Detection signal: the boundary convention test found raw status components
  despite both domain enums already existing.
- Prevention rule: retain owning lifecycle enums through application and web
  mapping; serialize stable names only at external boundaries.

## 2026-09-06 — Keep Catalog status typed through public mapping

- Failure mode: Catalog converted its finite item status to a string before
  returning application and HTTP models.
- Detection signal: a boundary convention test found raw status components even
  though `CatalogItemStatus` already existed in the domain.
- Prevention rule: preserve the domain enum through Catalog result and response
  mapping, serializing only at the external boundary.

## 2026-09-06 — Keep Calendar state enums at provider-facing read boundaries

- Failure mode: Calendar converted event-link and synchronization state enums
  to strings in public result models.
- Detection signal: provider adapter fixtures required raw `SYNCED` values even
  though Calendar already owned status enums.
- Prevention rule: retain domain state enums through Calendar application and
  response mapping; serialize stable names only at external edges.

## 2026-09-06 — Preserve customer lifecycle enums through read mapping

- Failure mode: the Clients mapper converted `CustomerStatus` to a string
  before returning the public customer result.
- Detection signal: a boundary convention test found raw status types in both
  the use-case result and HTTP response despite an existing domain enum.
- Prevention rule: keep finite lifecycle enums through application and web
  mappings, with stable names handled only by serialization.

## 2026-09-06 — Keep subscription lifecycle state typed through HTTP mapping

- Failure mode: subscription domain status was converted to a string in the
  application result before reaching the HTTP response.
- Detection signal: the public result and response records declared `String
  status` despite an existing `SubscriptionStatus` enum.
- Prevention rule: pass finite lifecycle enums through application and web
  mapping layers; let the serializer preserve their stable names.

## 2026-09-06 — Reuse domain enums in public read models

- Failure mode: Identity converted `MembershipStatus` to a string before it
  crossed application and HTTP boundaries, allowing a finite state to lose its
  type at the API edge.
- Detection signal: a boundary convention test found `String status` in all
  four membership read/response records.
- Prevention rule: keep the owning domain enum through application and response
  models; rely on stable enum-name serialization at external boundaries.

## 2026-09-06 — Encode finite lifecycle states as enums

- Failure mode: tenant provisioning state crossed the port, API, persistence,
  and listener layers as unvalidated strings such as `ACTIVE` and `FAILED`.
- Detection signal: the duplicate-activation guard needed a raw string
  comparison, and one spelling change could silently create an unknown state.
- Prevention rule: represent finite lifecycle states with enums or value
  objects at Java boundaries; serialize their stable names only at database or
  external protocol edges, and add a convention test for the boundary.

## 2026-09-06 — Make lifecycle listeners idempotent before republishing facts

- Failure mode: a duplicate tenant-realm-ready event would mark the tenant
  active and publish another activation fact.
- Detection signal: a focused listener test with an already `ACTIVE` registry
  reached event construction instead of returning a no-op.
- Prevention rule: lifecycle consumers must inspect authoritative control-plane
  state and classify already-completed transitions as no-ops before mutation or
  downstream publication; keep retryable failed/provisioning states executable.

## 2026-09-06 — Do not remove explicit tenant identity by naming pattern

- Failure mode: treating every `findByTenantId...` method as duplicated schema
  filtering would weaken control-plane authorization, provider-key resolution,
  callback claims, idempotency, cross-tenant jobs, or specialized projections.
- Detection signal: the operation-by-operation audit showed that the remaining
  methods either resolve a tenant before schema checkout or preserve an
  invariant that JPA schema-local CRUD cannot express.
- Prevention rule: remove tenant predicates only for ordinary CRUD/list reads
  after the tenant connection is selected; keep explicit tenant identity for
  shared/control-plane, authorization, provider/business-key, callback,
  idempotency, cross-tenant, vector/full-text, JSONB, and atomic operations.

## 2026-09-06 — Name concrete decorators after their behavior

- Failure mode: a concrete tracing decorator retained `Port` in its name after
  the canonical embedding service contract replaced the old port family.
- Detection signal: the class implemented `EmbeddingService` directly and had
  no port callers, but its name still suggested an application boundary.
- Prevention rule: reserve `Port` for provider-neutral contracts; name concrete
  wrappers and decorators after the service behavior they implement.

## 2026-09-06 — Give durable event listeners explicit stable identities

- Failure mode: relying on generated listener identity makes publication and
  retry diagnostics coupled to class or method naming changes.
- Detection signal: existing durable listeners mixed explicit IDs with default
  generated IDs across provisioning and calendar boundaries.
- Prevention rule: every durable Modulith listener that participates in
  provisioning, provider synchronization, or replay must declare a stable
  module-qualified ID; do not add a second delivery mechanism to solve this.

## 2026-09-06 — Reconstruct database routing before tenant-schema replay

- Failure mode: a replayed Calendar event restored only `tenantId`, leaving the
  database routing context empty and allowing the default database to be
  selected for a tenant that belongs to a dedicated database.
- Detection signal: the event contract had tenant identity but no database
  identity, while `TenantRoutingDataSource` selects pools from the current
  database context.
- Prevention rule: control-plane consumers must resolve and restore both tenant
  and database routing context before touching tenant-schema repositories;
  never assume request-local context exists during replay.

## 2026-09-06 — Cross-module consumers must use public APIs

- Failure mode: Calendar restored replay routing by importing Tenancy's
  internal `TenantRepository` port directly across the module boundary.
- Detection signal: `CrossModuleDependencyArchitectureTest` rejected the
  dependency even though focused Calendar and Tenancy tests passed.
- Prevention rule: expose cross-module business operations as provider-neutral
  use cases in the owning module's public API; keep internal repository ports
  behind that API and let the architecture test enforce the boundary.

## 2026-09-06 — Duplicate provisioning must return the authoritative tenant

- Failure mode: a repeated tenant provisioning request skipped insertion for an
  existing slug but returned the newly generated request ID.
- Detection signal: a focused duplicate-slug test with distinct requested and
  existing IDs exposed the mismatch between the returned identity and the
  control-plane registry.
- Prevention rule: idempotent control-plane requests must return the existing
  authoritative tenant ID when their business key already exists; never expose
  a transient ID that has no schema-routing record.

## 2026-09-05 — Run full Spring context checkpoints after constructor changes

- Failure mode: adding a guardrail-aware constructor left the previous required
  `@Autowired` constructor annotated, so unit tests passed but every Spring
  application context failed to create `ChatService`.
- Detection signal: the full Assistant test checkpoint reported
  `Invalid autowire-marked constructor` while focused service tests were green.
- Prevention rule: every class with compatibility constructors must have an
  explicit one-`@Autowired` constructor test and a Spring context checkpoint
  after changing dependency injection signatures.

## 2026-09-05 — Preserve established blank-message compatibility at service boundaries

- Failure mode: applying the new direct input guard to blank messages changed
  the existing mock chat HTTP contract from a graceful response to an
  exception.
- Detection signal: the full Assistant checkpoint failed only
  `AiModuleTest.shouldHandleEmptyMessageGracefully` with `input.blank`.
- Prevention rule: keep legacy compatibility behavior at the application
  service boundary while enforcing blank-input policy in the provider advisor
  and standalone guard tests for real model paths.

## 2026-09-05 — Scope Gradle test filters to the owning module

- Failure mode: a multi-module Gradle invocation included Assistant test
  filters for tests that only exist in AI Platform, causing the Assistant task
  to fail with “No tests found” after the actual changed-module tests passed.
- Detection signal: the failing task was `:modules:assistant:test`, not a
  production compilation or assertion failure.
- Prevention rule: apply each `--tests` selector only to the module that owns
  the matching class, then run dependent modules with compile-only tasks when
  they have no matching test source.

## 2026-09-05 — Verify source-set and package paths before writing plans

- Failure mode: an implementation plan initially placed the existing Twilio
  contract test under a provider subpackage that does not exist.
- Detection signal: comparing the plan’s file inventory with `find` output
  during the self-review caught the mismatch before implementation began.
- Prevention rule: resolve every existing test and source path with `rg --files`
  or `find` before committing a multi-file implementation plan; mark only truly
  new files as `Create`.

## 2026-09-05 — Externalized event consumers cannot depend on request security

- Failure mode: An externalized appointment consumer read `SecurityContext` to
  decide whether to establish customer membership.
- Detection signal: A replay-style test with no authentication performed no
  membership action even though the event carried the customer identity.
- Prevention rule: Durable event consumers must use authoritative event data
  and provider-neutral idempotent use cases; request-local authentication is
  not available during Kafka/Modulith replay.

## 2026-09-05 — Do not walk mutable build output in source contract tests

- Failure mode: A repository-wide contract test walked a whole project tree
  while Spring Modulith documentation generation concurrently created or
  removed `build/spring-modulith-docs`.
- Detection signal: The push gate failed with a transient
  `NoSuchFileException` under an ignored build directory, although the test's
  intended source files were unchanged.
- Prevention rule: Build source-contract file lists from stable source and
  build-script roots; never traverse mutable `build/` output directories.

## 2026-09-04 — Trigger persistence lifecycle setup in adapter fixtures

- Failure mode: an adapter test returned a new JPA entity without the ID that
  Hibernate normally assigns during `@PrePersist`.
- Detection signal: domain rehydration failed with `id must not be null` after
  the repository lookup itself was correctly scoped.
- Prevention rule: when mocking a saved JPA entity, invoke its public lifecycle
  setup or otherwise provide the persisted identity required by the mapper.

## 2026-09-04 — Reuse domain enum vocabulary in adapter tests

- Failure mode: a focused persistence test used the familiar JDK day-of-week
  constant instead of the domain enum's abbreviated value.
- Detection signal: test compilation failed before reaching the intended
  missing tenant-scoped repository methods.
- Prevention rule: inspect domain value objects and enums before constructing
  adapter fixtures; tests must use the domain vocabulary exactly.

## 2026-09-04 — Add metadata for every new package level

- Failure mode: adding a nested production adapter package caused the module
  architecture test to fail because the intermediate package lacked
  `package-info.java`.
- Detection signal: `AiCapabilityConventionTest` reported missing package
  metadata for `adapter/out/messaging`.
- Prevention rule: when introducing a nested Java package, add metadata for
  each materialized package level before running the full module suite.

## 2026-09-04 — Keep inbound event mechanics out of application services

- Failure mode: removing the event publisher dependency still left the
  application service coupled to `@ApplicationModuleListener` and responsible
  for event-context reconstruction.
- Detection signal: the service retained a Modulith import and a listener
  method after publication had already moved behind a port.
- Prevention rule: place event listener annotations and transport/context
  restoration in inbound adapters; keep the application service focused on its
  use-case operations.

## 2026-09-04 — Include tenant identity in aggregate updates

- Failure mode: an existing customer update selected by UUID alone could load
  a record outside the aggregate's tenant scope.
- Detection signal: the adapter used `JpaRepository.findById` while the domain
  object already supplied `tenantId`.
- Prevention rule: derived JPA update lookups must include tenant identity when
  the aggregate is tenant-owned; cover the lookup choice with an adapter test.

## 2026-09-04 — Propagate shared port contract changes deliberately

- Failure mode: tightening a shared customer repository port immediately
  broke dependent appointment compilation because callers still used the old
  unscoped method.
- Detection signal: the Gradle dependency graph reported the exact downstream
  call sites during compile before any tests could run.
- Prevention rule: after changing a shared port, search all modules for the
  old method, update each caller in the same vertical slice, and run the
  affected module tests together.

## 2026-09-04 — Remove forwarding mappers only after contract coverage

- Failure mode: A persistence mapper duplicated entity conversion methods and
  required a dedicated Spring configuration bean without adding provider
  substitution or behavior.
- Detection signal: Every mapper method was a one-line delegation to the JPA
  entity, and the application port already isolated the adapter.
- Prevention rule: Add adapter-level characterization coverage first, then
  remove forwarding mappers only when the application port and tenant scope
  remain unchanged.

## 2026-09-04 — Assert rehydrated domain contracts, not object identity

- Failure mode: A persistence adapter test compared a rehydrated domain object
  with the original instance even though the aggregate intentionally has no
  value-based equality.
- Detection signal: The adapter returned the correct fields but AssertJ
  reported different object identities.
- Prevention rule: Persistence tests must assert identity and business fields
  explicitly unless the domain type defines value equality as part of its
  contract.

## 2026-09-04 — Keep event mechanics in outbound adapters

- Failure mode: An application service imported Spring's event publisher
  directly even though the module already used provider-neutral outbound ports.
- Detection signal: The service layer contained a framework import for an
  implementation detail, while the event payload itself was already a stable
  module API record.
- Prevention rule: Define a capability port for event publication, inject it
  into the application service, and keep Modulith/Kafka mechanics in an
  outbound adapter.

## 2026-09-04 — Add package metadata with every new production package

- Failure mode: A new publisher package compiled successfully but failed the
  repository architecture gate because it had no `package-info.java`.
- Detection signal: `PackageMetadataArchitectureTest` reported the exact
  materialized package after the broad push gate.
- Prevention rule: Whenever a production package is created, add its package
  metadata in the same patch and run the repository package-convention test.

## 2026-09-04 — Keep provider substitution at the port boundary

- Failure mode: A naming proposal treated every PostgreSQL/JDBC adapter as a
  candidate for a `Postgres...` rename, which could imply provider coupling even
  though the application already depends on stable capability ports.
- Detection signal: The proposed implementation names were being evaluated as
  if they were public contracts instead of infrastructure details.
- Prevention rule: Name domain/application ports by capability, keep accurate
  adapter mechanism names when they are useful, and use provider names only for
  intentionally provider-locked implementations. Never add a rename or wrapper
  that weakens provider substitution merely for stylistic consistency.

## 2026-09-04 — Reuse one qualified client per data source

- Failure mode: A specialized AGE configuration introduced a second
  `JdbcClient` bean for a tenant data source that already had the canonical AI
  tenant client.
- Detection signal: Composition-root inspection found two bean names wrapping
  the same `tenantScopedDataSource`.
- Prevention rule: Before adding a framework client in a feature configuration,
  search existing bean qualifiers for the same data source and reuse one
  canonical client unless different transaction or connection settings are
  proven necessary.

## 2026-09-05 — Validate dynamic schema identifiers at every boundary

- Failure mode: The normal tenant checkout path validated schema names, but a
  direct Hibernate multi-tenant provider call could reach `Connection.setSchema`
  without that validation.
- Detection signal: A provider-level regression test reached the tenant pool
  and failed with an unrelated null connection instead of rejecting the input.
- Prevention rule: Validate dynamic tenant schema identifiers immediately at
  every provider boundary before acquiring a connection; do not rely only on
  upstream resolver validation.

## 2026-09-05 — Centralize cross-layer authorization vocabulary

- Failure mode: Equivalent AI staff-role sets were copied into application
  services and persistence/workflow adapters.
- Detection signal: A role representation could be added to one path while
  another path continued rejecting the same authenticated principal.
- Prevention rule: Keep the authorization vocabulary in one provider-neutral
  application policy and reuse it for defense-in-depth checks; leave transport
  annotations at their adapter boundary.

## 2026-09-05 — Keep database-specific provisioning out of H2 fixtures

- Failure mode: Generic H2 module tests scanned the real tenant provisioning
  listener and attempted PostgreSQL-only Liquibase DDL asynchronously.
- Detection signal: Tests were green but emitted unfinished event publications
  and migration errors, masking real failures in the logs.
- Prevention rule: Supply a primary no-op provisioning port in H2 fixtures and
  reserve the real Liquibase adapter for PostgreSQL/Testcontainers integration
  tests.

## 2026-09-04 — Keep test and composition-root signatures synchronized

- Failure mode: A focused wiring test was changed to require a `JdbcClient`
  before the production configuration method had the matching dependency shape.
- Detection signal: The intended red test stopped at Java compilation with an
  incompatible argument type instead of exercising the boundary behavior.
- Prevention rule: When a TDD red test changes a composition-root signature,
  inspect and update the complete production dependency graph in the same
  slice, then rerun the red check before implementing behavior.

## 2026-09-04 — Keep workflow test fixtures out of production APIs

- Failure mode: A generic workflow capability record exposed a default factory
  intended only to make isolated graph tests easy, leaving a placeholder path
  visible from production code.
- Detection signal: A production-boundary test found `defaults()` and a
  package-private graph factory even though enabled runtime wiring already failed
  fast without real capabilities.
- Prevention rule: Keep test-only capability builders under test sources and make
  production configuration accept explicit capability wiring only.

## 2026-09-04 — Keep one composition root per AI tool

- Failure mode: Moving an assistant tool configuration into the canonical
  assistant configuration package left an older application-level registration
  active, causing duplicate Spring bean names at application startup.
- Detection signal: The application integration context failed with a
  `BeanDefinitionOverrideException` before external containers initialized.
- Prevention rule: Before moving or adding a configuration class, search every
  composition root for the bean name and keep exactly one authoritative
  registration; verify startup wiring separately from compilation.

## 2026-09-03 — Name classes for one responsibility

- Failure mode: A rescheduling application service implemented both the ordinary
  and authorized use cases, while a durable job executor carried a `Service`
  suffix despite being a worker.
- Detection signal: Architecture checks reported multiple use cases and service
  naming violations in the same class boundary.
- Prevention rule: Give each application service one use-case responsibility;
  name worker/executor classes after the mechanism they run and use a separate,
  explicit service for each authorized workflow boundary.

## 2026-08-29 — Configure Spring AI Redis return metadata explicitly

- Failure mode: A Redis vector search found the projected document, but the
  adapter discarded it because the durable id and response payload were not
  configured as returned metadata fields.
- Detection signal: A live Redis `FT.SEARCH *` returned one document while the
  adapter returned zero candidates.
- Prevention rule: For every Spring AI Redis projection, configure every field
  required by candidate mapping as a metadata field and cover the complete
  mapping with a real Redis integration test.

## 2026-08-29 — Encode arbitrary values before Redis tag filtering

- Failure mode: Raw context fingerprints and model versions containing `:`
  produced Redis Query Engine syntax errors or empty matches.
- Detection signal: The same vector and scope matched with simple values but
  failed for `context-v1:...` and `ollama-embeddinggemma:300m`.
- Prevention rule: Use Spring AI's typed filter builder and a reversible URL-safe
  encoding for projection tag values; keep original values in PostgreSQL.

## 2026-08-29 — Validate Liquibase include indentation immediately

- Failure mode: A new migration include was initially nested under the prior
  include's `relativeToChangelogFile` field during a patch.
- Detection signal: The diff showed `- include` indented below a scalar YAML
  property rather than at the changelog list level.
- Prevention rule: After every changelog edit, inspect the surrounding YAML
  list and run the database migration contract before committing.

## 2026-08-29 — Idempotency must be scoped to the authenticated principal

- Failure mode: A tenant-only mutation key could let a user replay another
  user's completed command when the request key was guessed or reused.
- Detection signal: Reviewing the gateway key derivation showed that the
  PostgreSQL predicate had tenant scope but no principal scope.
- Prevention rule: Derive mutation operation identity from tool, authenticated
  principal, and request key, then repeat both tenant and principal predicates
  in the durable store and its uniqueness constraint.

## 2026-08-29 — Custom integration source sets need explicit runtime dependencies

- Failure mode: The pgvector integration test could not compile an existing
  Jackson type because this repository's custom `integrationTest` configuration
  does not inherit every main-source dependency.
- Detection signal: `compileIntegrationTestJava` failed with a missing
  `com.fasterxml.jackson.databind` package while main compilation passed.
- Prevention rule: When an integration test uses a main adapter's transitive
  dependency directly, declare the existing artifact on the integration test
  configuration and compile the source set before running the container test.

## 2026-08-29 — Pin transitive Ragas compatibility explicitly

- Failure mode: the latest Ragas release resolved a newer `langchain-community`
  package that no longer contained the optional Vertex AI module imported by
  Ragas at startup.
- Detection signal: importing `ragas` failed before an evaluation could run,
  even though the Ragas dependency itself was installed.
- Prevention rule: lock the tested Ragas major/minor range and explicitly pin
  compatible transitive integrations; verify the real interpreter can import
  the evaluator before committing the offline worker.

## 2026-08-29 — Reuse the bound context in asynchronous assertions

- Failure mode: an executor propagation test compared the observed tenant to a newly generated
  random context instead of the context submitted to the executor.
- Detection signal: the task completed successfully, but the assertion showed two different valid
  UUIDs.
- Prevention rule: assign the context fixture once, bind that exact instance, and assert against its
  fields after the asynchronous task completes.

## 2026-08-29 — Enforce AI context at the application use-case boundary

- Failure mode: the web controller bound AI context, but a direct caller could invoke `ChatService`
  without a tenant/principal scope and still reach a model provider.
- Detection signal: a service-level test could call the legacy provider without `AiExecutionContext`.
- Prevention rule: every AI application use case must require the backend execution scope before
  any cache, semantic router, provider, or tool operation; adapters still bind the scope at ingress.

## 2026-08-29 — Update direct-call fixtures when enforcing context

- Failure mode: adding the fail-closed context guard correctly broke older unit tests that invoked
  the use case directly without the production ingress wrapper.
- Detection signal: existing tests failed only after the guard was enabled, while HTTP paths already
  bound context.
- Prevention rule: when an application boundary gains a security precondition, update direct-call
  fixtures to bind the same trusted scope used in production and retain a dedicated missing-context
  regression test.

## 2026-08-28 — Explicitly bind records with compatibility constructors

- Failure mode: Adding a convenience constructor to a `@ConfigurationProperties` record made
  Spring Boot choose no bind constructor and broke every context that injected the record.
- Detection signal: Context startup reported `NoSuchMethodException` for the record's no-argument
  constructor.
- Prevention rule: When a configuration-properties record has more than one constructor, mark the
  canonical binding constructor explicitly with `@ConstructorBinding` and run a context test.

## 2026-08-28 — Preserve restored security context while extending AI runtime

- Failure mode: An intermediate change removed a newly restored tenant execution-context slice
  while trying to simplify the AI runtime.
- Detection signal: The user explicitly required that the tenant scope and bridge remain present.
- Prevention rule: Before simplifying AI infrastructure, verify the canonical tenant/auth context
  files and keep them as the security boundary; extend existing integrations around them.

## 2026-08-28 — Do not overlap Gradle test/report writers during scheduler debugging

- Failure mode: a second AI-platform test was started while a previous Gradle
  check was still running, which made the fairness test appear to hang and
  obscured its actual result.
- Detection signal: multiple Gradle test workers were writing the same module's
  test outputs at once, and the first session stopped producing output.
- Prevention rule: serialize Gradle commands that write test reports in this
  checkout; wait for or terminate the prior session before starting another.

## 2026-08-28 — Run formatter after documentation comments

- Failure mode: a long Javadoc line and a constructor delegation were accepted
  by compilation but failed the repository's Spotless check.
- Detection signal: `spotlessJavaCheck` reported only formatting violations in
  newly added trace classes.
- Prevention rule: run the module formatter immediately after adding or editing
  Javadocs and before the final verification command.

## 2026-08-28 — Count record constructor arguments after schema changes

- Failure mode: adding nullable usage fields to a trace record left extra
  positional arguments in new model-call adapters.
- Detection signal: Java reported constructor arity/type errors at the adapter
  call sites.
- Prevention rule: after changing a record schema, count every updated call
  site against the declaration before running the full suite.

## 2026-08-27 — Java regex escaping is a test implementation concern

- Failure mode: a migration contract test used regex parentheses with a single
  backslash, so Java rejected the test source before the intended red state.
- Detection signal: `illegal escape character` during `compileTestJava`.
- Prevention rule: keep regex escaping valid in the Java source first, then run
  the contract test to observe the expected missing-resource failure.

## 2026-08-27 — Worker-thread tests must evaluate thread state inside the task

- Failure mode: a method reference created from `Thread.currentThread()` observed
  the submitting test thread instead of the executor worker thread.
- Detection signal: executor tests reported `Test worker` and platform-thread
  state even though the configured factories were correct.
- Prevention rule: use a lambda that calls `Thread.currentThread()` inside the
  submitted task whenever a test asserts worker identity, name, or kind.

## 2026-08-27 — Verify preview API generic return types before adapter coding

- Failure mode: the initial StructuredTaskScope adapter assumed `awaitAll()`
  returned subtasks, while Java 25 returns `Void`; it also modeled record
  accessors as `Optional` values with incompatible component return types.
- Detection signal: the Java compiler reported generic-bound and invalid-record-
  accessor errors before any runtime test was attempted.
- Prevention rule: inspect the selected JDK’s generic signatures and keep record
  component accessors raw; expose optional views through differently named methods
  when needed.

## 2026-08-05 — Confirm the owning repository before deployment changes

- Failure mode: deployment design work was started in `emme-modulith` even
  though the requested owning repository was `emme-service`.
- Detection signal: the user corrected the repository scope before deployment
  implementation began.
- Prevention rule: before changing deployment or release configuration, verify
  the target repository, its branch, and ownership of frontend source versus
  deployment manifests; keep the frontend Vite/Nginx source with the web app
  and place service-owned Kubernetes/Compose/secret orchestration with the
  service repository.

## 2026-08-04 — Read-only transactions also require proxyable services

- Failure mode: Adding `@Transactional(readOnly = true)` to the final
  `GetCurrentUserService` caused every Spring context test to fail during bean
  creation.
- Detection signal: Spring reported `Cannot subclass final class` from the
  configured CGLIB transaction proxy.
- Prevention rule: Treat every class-level transaction annotation as an AOP
  change; keep Spring-managed services non-final when the application uses
  class-based proxies, and run a full context test after changing transaction
  boundaries.

## 2026-08-03 — Do not run overlapping Gradle test writers

- Failure mode: Several Gradle test commands were started concurrently against
  the same checkout and build directories.
- Detection signal: Independent tests reported missing
  `test-results/test/binary/in-progress-results-*.bin` files even though the
  same focused test passed when rerun alone.
- Prevention rule: Run Gradle commands that write test reports serially in one
  checkout; parallelize only isolated worktrees or non-overlapping report
  directories.

## 2026-08-03 — Test profiles must use the framework-owned property namespace

- Failure mode: The Kafka integration profile placed datasource and JPA values
  under `app.*`, so Spring Boot ignored them and the tenant routing datasource
  attempted to connect to the local production PostgreSQL endpoint.
- Detection signal: The integration context first lacked
  `JdbcConnectionDetails`, then failed against `localhost:5432`, and finally
  failed schema validation after only the datasource key was corrected.
- Prevention rule: Keep framework configuration under its canonical `spring.*`
  namespace and reserve `app.*` for application-owned typed properties. Add a
  real profile-context integration test whenever a custom composition root
  replaces a framework auto-configuration.

## 2026-08-03 — Optional service-connection metadata

- Failure mode: A bootstrap database adapter required
  `JdbcConnectionDetails` even in H2/Kafka contexts where no Testcontainers
  database service connection is intentionally created.
- Detection signal: The Kafka integration context failed before tests ran with
  `NoSuchBeanDefinitionException: JdbcConnectionDetails`.
- Prevention rule: Treat service-connection metadata as optional at framework
  boundaries and fall back to the validated typed connection properties when a
  local or in-memory profile does not provide the optional bean.

## 2026-08-03 — Isolated integration contexts must override public contracts

- Failure mode: An integration test attempted to replace an internal Identity
  port from the application composition root, but the application intentionally
  exposes only the module API and compilation failed.
- Detection signal: The platform integration-test classpath could not resolve
  `identity.application` types.
- Prevention rule: Test compositions may override only public module contracts;
  add an explicit feature toggle when an external listener must be disabled in
  an isolated context instead of widening module visibility.

## 2026-08-02 — Identifier-only lookups are a tenant-isolation defect

- Failure mode: Assistant commands and repository ports addressed existing
  conversations and pending actions by ID alone, allowing a caller with a valid
  identifier to bypass the module's tenant boundary.
- Detection signal: A package-boundary audit found `repository.findById(...)`
  in outbound adapters and HTTP routes that did not call `withCurrentTenant`.
- Prevention rule: Every application operation that addresses an existing
  tenant-owned record must carry the tenant ID explicitly, and every persistence
  predicate must include that tenant ID. Add a source-boundary regression test
  when the contract is structural.

## 2026-08-01 — Unreleased architecture has no compatibility legacy

- Failure mode: a boundary refactor initially preserved a legacy public API and
  implementation because compatibility was treated as the default.
- Detection signal: the user clarified that the service is unreleased and wants
  the latest canonical architecture everywhere.
- Prevention rule: for unreleased services and modules, remove obsolete names,
  packages, wrappers, and compatibility aliases in the same migration. Keep a
  compatibility layer only when an external released consumer, persisted
  contract, or explicitly approved migration window requires it.

## 2026-08-01 — Patch target validation

- Failure mode: an edit was accidentally targeted at a similarly named path
  outside the active repository.
- Detection signal: the patch result referenced a path outside the current
  workspace even though the intended repository file was unchanged.
- Prevention rule: validate every absolute `apply_patch` target against the
  active repository root before editing, then verify the target with `git
  status` immediately afterward.

## 2026-08-01 — Shared-context integration test isolation

- Failure mode: a new permission integration test reused the subject from an
  existing current-user test, leaving two active memberships in the shared
  application context.
- Detection signal: the existing current-user assertion expected one result
  but received two after the new test was added.
- Prevention rule: integration fixtures that persist state across test methods
  must use dedicated subject/resource identifiers unless the test explicitly
  owns cleanup or rollback.

## 2026-08-01 — Cross-module technical type renames

- Failure mode: renaming a persistence entity/repository left shared fixtures
  and a downstream module test importing the old technical names.
- Detection signal: targeted production compilation passed, but downstream test
  compilation failed on stale imports.
- Prevention rule: after a persistence type rename, search the entire repository
  for both the old fully-qualified type and simple type name, then compile all
  known consumers before declaring the slice complete.

## 2026-07-31 — GitHub Actions job conditions and secrets

- Failure mode: referencing a job-level `env` value in `jobs.<job>.if` caused
  GitHub Actions to reject the workflow before any job started.
- Detection signal: a workflow run completed with no jobs and no job logs.
- Prevention rule: evaluate optional secrets in an explicit first step, expose
  only a boolean through `$GITHUB_OUTPUT`, and apply `if` to later steps. Never
  expose or print the secret value itself.

## 2026-07-31 — JPA managed identity in persistence adapters

- Failure mode: mapping a domain object to a new JPA entity and passing it to
  `save()` while an entity with the same identifier was already managed caused
  `DuplicateKeyException`/multiple-object identity failures.
- Detection signal: the Calendar persistence adapter test failed when the
  application loaded an entity, changed the domain representation, and saved it
  in the same transaction.
- Prevention rule: when an identifier already exists, load and mutate the
  managed entity in place; only create a new entity for new identifiers. Keep a
  persistence adapter regression test for both paths.

## 2026-07-31 — ArchUnit guardrail checkpoints

- Failure mode: a newly added architecture rule initially failed at test
  compilation because its static rule factory import was missing.
- Detection signal: the test task failed before executing the architecture
  assertions with an unresolved `noClasses()` symbol.
- Prevention rule: compile and execute each new ArchUnit guardrail immediately
  after adding it; keep intentionally red architectural assertions local until
  the corresponding production boundary is implemented.

## 2026-07-31 — Application result models at inbound boundaries

- Failure mode: migrating an application service away from JPA entities left the
  controller without the related display names required by the existing HTTP
  response.
- Detection signal: the service compiled only after the adapter response was
  changed to an application-owned read model carrying IDs, names, and status.
- Prevention rule: when a use case needs transport-friendly denormalized data,
  assemble an application result from domain models and ports; never return a
  persistence entity merely to preserve response fields.

## 2026-07-31 — Spring transaction proxies and service naming refactors

- Failure mode: renaming a transactional service and making it `final` prevented
  Spring's configured CGLIB proxy from being created during context startup.
- Detection signal: full application-context tests failed with
  `Cannot subclass final class` even though compilation and focused source-tree
  tests passed.
- Prevention rule: after moving or renaming Spring-managed transactional classes,
  run at least one full context test; keep proxied services non-final unless the
  project explicitly uses interface-based transaction proxies.

## 2026-08-01 — Boundary validation must run before resource lookup

- Failure mode: a web test tried to prove an oversized update name using an
  unknown tenant ID, so the request reached the resource lookup and returned
  `404` before the intended validation assertion.
- Detection signal: the test failed with `Status expected:<400> but was:<404>`.
- Prevention rule: test record constraints with a validator unit test, or use a
  valid resource fixture when verifying the HTTP mapping. Do not rely on an
  unrelated not-found path to prove boundary validation.

## 2026-08-01 — Rename-aware architecture tests

- Failure mode: a package-convention test retained the old source path after a
  technical type was moved and renamed.
- Detection signal: the test failed with `NoSuchFileException` while reading the
  legacy path, even though compilation and the new package checks were correct.
- Prevention rule: update source-tree assertions in the same red-green slice as
  a rename, and run the focused convention test before the broader module gate.

## 2026-08-01 — Source-tree tests must distinguish empty legacy directories

- Failure mode: a migration removed every Java source from a legacy package, but
  the filesystem directory remained and a `Files.exists(directory)` assertion
  incorrectly reported the legacy package as present.
- Detection signal: the canonical package test failed only on the legacy-directory
  assertion after all source moves and compilation succeeded.
- Prevention rule: architecture tests should assert the absence of legacy source
  files (`hasJavaSources`) rather than the incidental existence of directories.

## 2026-08-01 — Verify BOM module availability before dependency alignment

- Failure mode: aligning the Testcontainers version to Spring Boot's resolved
  core version assumed that the PostgreSQL and JUnit integration modules existed
  at the same version.
- Detection signal: Gradle could resolve `testcontainers:2.0.5` transitively but
  could not resolve `postgresql:2.0.5` or `junit-jupiter:2.0.5`.
- Prevention rule: inspect the published module set before changing a dependency
  BOM; revert speculative alignment changes when the artifact coordinates do not
  exist and record the shared-infrastructure issue separately.

## 2026-08-01 — Spring proxyability applies to persistence adapters

- Failure mode: a Spring-managed persistence adapter declared `final` and could
  not be proxied by the configured class-based AOP infrastructure during a full
  application-context test.
- Detection signal: context startup failed with `Cannot subclass final class`
  and the failing adapter changed as each earlier final adapter was corrected.
- Prevention rule: keep Spring-managed adapters non-final when class-based
  proxies are enabled, and verify the complete context after package migrations.

## 2026-08-01 — Forwarded headers require an explicit trust boundary

- Failure mode: using the first `X-Forwarded-For` value unconditionally allowed
  a direct caller to rotate the rate-limit key.
- Detection signal: a focused filter regression test could submit a second
  request with a different forwarded address while keeping the same socket peer.
- Prevention rule: use forwarded client identity only when the immediate peer
  matches configured proxy networks; default to the socket peer and keep the
  proxy list typed and validated.

## 2026-08-01 — Result-model accessors must match transport vocabulary

- Failure mode: a web mapper migrated from the domain `isEnabled()` accessor to
  the API result model but retained the domain accessor name, causing compilation
  to fail.
- Detection signal: the focused Identity build reported that `FeatureFlagDetails`
  had no `isEnabled()` method.
- Prevention rule: when introducing a public result record, update adapters to
  use its exact record component names (`enabled()` here) and run the focused
  compile immediately after the boundary move.

## 2026-08-01 — One application service per use case

- Failure mode: application services grouped multiple unrelated public use
  cases behind a convenience facade.
- Detection signal: a source-level architecture guard found more than one
  `*UseCase` interface on the same application service.
- Prevention rule: every public use-case interface has one focused concrete
  application-service implementation, and every application service implements
  exactly one use-case interface. Move shared behavior into a named mapper,
  policy, evaluator, loader, or factory; do not retain a multi-use-case facade
  as a workaround for circular dependencies.

## 2026-08-01 — Repository-root source assertions

- Failure mode: a source-boundary test used a repository-relative path directly,
  but Gradle executed the module test with that module as the working directory.
- Detection signal: the test failed with `NoSuchFileException` even though the
  production source existed and compiled.
- Prevention rule: source-tree tests must locate the repository root by walking
  upward to `settings.gradle.kts`, then resolve repository-relative paths from
  that root. Re-run the focused test after adding every source assertion.

## 2026-08-01 — Typed properties must replace every configuration access path

- Failure mode: replacing one `@Value` field left a second constructor or
  outbound implementation reading the same setting through a separate field or
  environment variable.
- Detection signal: a repository-wide search still found direct `@Value` or
  `System.getenv` usage in the capability being migrated.
- Prevention rule: model a capability's complete configuration as one typed
  properties object, inject it into every consumer, and search the whole
  production tree before marking the slice complete. Keep provider-specific
  secret migration as an explicit follow-up when its existing property model
  needs redesign.

## 2026-08-01 — Move source-boundary assertions with package refactors

- Failure mode: production packages were moved while source-boundary tests
  continued asserting the old paths.
- Detection signal: focused architecture tests failed on paths that no longer
  represented the canonical package layout.
- Prevention rule: move or update source-boundary tests in the same slice as
  production package moves, then run the focused package guard before commit.

## 2026-08-01 — Replace cross-module facades with focused public use cases

- Failure mode: a module-level service or API facade bundled lifecycle commands,
  reads, and cross-module coordination behind one injectable type.
- Detection signal: callers imported a concrete `*Service` or a legacy API
  interface with several unrelated methods, making package visibility and
  responsibility unclear.
- Prevention rule: expose one command/query/use-case contract per capability,
  implement each with one application service, and update test fixtures to use
  the public contract rather than retaining a compatibility facade.

## 2026-08-01 — Avoid domain/annotation simple-name collisions

- Failure mode: a domain type named `Service` collided with Spring's
  `@Service` annotation while splitting application services.
- Detection signal: Java compilation reported an ambiguous `Service` reference
  in annotations and local variables.
- Prevention rule: use a fully qualified domain type or a named mapper at the
  boundary, and run the module compile immediately after introducing a new
  application service.

## 2026-08-01 — Split public integration facades by operation

- Failure mode: a public calendar synchronization facade bundled unrelated
  link queries and lifecycle mutations, forcing every provider adapter to
  depend on all operations.
- Detection signal: adapters imported one API with six methods even though
  each adapter used a different subset.
- Prevention rule: expose one public use-case interface per operation and
  inject only the contracts required by each adapter.

## 2026-08-01 — Name technical coordinators by their operation

- Failure mode: a technical audit coordinator was named `AuditService` even
  though Audit is not a business use-case module and the class records one
  event operation.
- Detection signal: the generic service name obscured whether the class owned
  an aggregate, a public use case, or an infrastructure boundary.
- Prevention rule: name non-domain coordinators after the operation they own,
  such as `AuditEventRecorder`, and keep reserved metadata-only modules
  free of speculative implementation layers.

## 2026-08-03 — Keep non-use-case collaborators out of application/service

- Failure mode: a repository-wide one-use-case-per-service check initially
  treated `RecordAuditEventService` as a use-case service even though it was an
  internal audit transaction coordinator.
- Detection signal: the class lived under `application/service` but implemented
  no matching public use-case interface.
- Prevention rule: reserve `application/service` for one
  `<Verb><Subject>Service` per matching `<Verb><Subject>UseCase`; place internal
  policy, authorization, audit, and process collaborators in their semantic
  application subpackages.

## 2026-08-01 — Avoid repeated responsibility nouns in type names

- Failure mode: service-catalog use cases initially produced names such as
  `CreateCatalogServiceService`.
- Detection signal: the class name repeated the generic technical suffix and
  obscured that the operation targeted a catalog entry.
- Prevention rule: name the business target explicitly (`CreateServiceCatalogEntryService`)
  and apply the same normalized vocabulary to its use-case interface.

## 2026-08-01 — Provider webhook signatures are provider manifests

- Failure mode: treating a provider's `x-signature` header as a Base64 HMAC of
  the raw body accepted the wrong protocol and could reject valid deliveries or
  authenticate the wrong message.
- Detection signal: official MercadoPago documentation defines a `ts`/`v1`
  signature over `id`, `request-id`, and timestamp manifest fields.
- Prevention rule: model each provider's documented signature manifest in a
  focused inbound verifier, compare digests in constant time, and test malformed
  metadata plus valid/invalid signatures before wiring the controller.

## 2026-08-01 — Read queries must carry tenant scope

- Failure mode: a notification lookup by UUID alone could return another
  tenant's record when row-level security was not active in a test or support
  context.
- Detection signal: a red application-service test returned a record for a
  mismatched tenant.
- Prevention rule: require tenant identity in module read queries and repository
  ports; adapters must resolve current tenant context before invoking them.

## 2026-08-01 — Webhook secrets must fail closed

- Failure mode: an empty WhatsApp app secret previously disabled signature
  verification and allowed a forged callback path.
- Detection signal: a source-boundary test found the explicit
  `skipping signature verification` branch and a properties test accepted
  placeholder verify-token and tenant defaults.
- Prevention rule: disable the webhook until real provider credentials and tenant
  routing are configured; reject malformed signatures and compare digests with
  a constant-time primitive.

## 2026-08-01 — Provider account metadata is the tenant boundary

- Failure mode: assigning every WhatsApp message to a configured fallback tenant
  allowed an unknown provider account to cross tenant boundaries.
- Detection signal: the webhook parser ignored `phone_number_id` and returned a
  zero/default tenant for every account.
- Prevention rule: resolve tenant identity from authenticated provider account
  metadata through an application-owned port and reject unknown account IDs;
  never use a placeholder tenant in production routing.
- 2026-08-01 — Cross-module consumers must use published use-case contracts,
  never application-owned provider ports. When a provider capability is needed
  by another module, expose a focused public use case and keep the provider port
  private to its owning module. Verify Modulith dependency names against the
  exact `@NamedInterface` value.

- 2026-08-01 — Tenant isolation must cover every mutating use case, not only
  HTTP reads. Commands for delivery, cancellation, retry, and similar actions
  carry the tenant identity, and application ports must not retain unscoped
  lookup methods that make accidental cross-tenant access possible.

- 2026-08-01 — Payment reads and lifecycle mutations are equally sensitive to
  tenant scope. A controller that scopes list/create but not get/refund still
  exposes a cross-tenant object reference. Make tenant identity part of every
  payment command/query and remove bare-ID repository ports.

- 2026-08-01 — A public use-case interface must never return a type from
  `application.result`, even when that type is technically immutable. Public
  results belong under `api/result`; application services may assemble them but
  must not own the public package.

- 2026-08-01 — The repository CI task is the formatting source of truth across
  fixtures and modules. Run it after structural refactors because shared test
  fixtures can fail Spotless even when the changed module itself is clean.

- 2026-08-01 — Capability-specific transport beans must not be exposed as a
  generic shared type when another module already publishes the same type.
  The initial AI `OkHttpClient` bean broke the service context because Identity
  already owns an `OkHttpClient`. Keep capability transport dependencies behind
  a capability-owned wrapper or explicit port so Spring resolution remains
  unambiguous.

- 2026-08-01 — A generic `adapter/out/provider` directory hides which external
  system changes together. Group capability implementations by technology or
  channel (`provider/email`, `provider/sms`, `provider/push`, or
  `provider/stripe`) and keep the application port in `application/port/out`.
  Reserve `adapter/out/client` for transport-only wrappers and wire DTOs that
  do not implement the application capability port.

- 2026-08-01 — Provider classes that construct `OkHttpClient` or `ObjectMapper`
  internally violate the composition-root boundary and make deterministic
  contract tests harder. Detection signal: source-boundary tests find
  `new OkHttpClient()` or `new ObjectMapper()` under provider packages.
  Prevention rule: create one capability-owned transport wrapper in
  configuration and inject both transport and serialization dependencies into
  every provider.

- 2026-08-01 — Google Calendar adapters had the same hidden transport
  construction problem as payment and notification providers. Prevention rule:
  every external integration capability owns one named transport wrapper and
  one composition-root bean; adapters receive it through constructor injection.

## 2026-08-02 — Scheduled provisioning reads must fail safely

- Failure mode: the Tenancy provisioning scheduler allowed a transient registry
  lookup failure to escape from the scheduled method and produced an uncaught
  scheduler error while the application context was still initializing.
- Detection signal: the Studio module check logged a `BadSqlGrammarException`
  for the not-yet-created provisioning registry table even though the tests
  completed successfully.
- Prevention rule: scheduled process managers must catch failures while loading
  work, log a bounded diagnostic, and return so the next scheduled invocation
  can retry; they must not leak infrastructure failures through Spring's
  scheduler boundary.

## 2026-08-02 — Modulith named interfaces follow capability package moves

- Failure mode: moving a shared primitive from the module root into a
  capability package caused Modulith violations even though consumers still
  declared a dependency on the `shared` module.
- Detection signal: `ModularityTest` reported dependencies through
  `shared.persistence` and `shared.identity` as inaccessible package targets.
- Prevention rule: every cross-module capability package must declare an exact
  `@NamedInterface` value, and each consumer must use the matching
  `module :: named-interface` dependency entry.

## 2026-08-02 — Integration assertions must follow explicit SQL ordering

- Failure mode: a bounded PostgreSQL query ordered rows by UUID, while the
  integration test asserted the first inserted UUID and failed nondeterministically.
- Detection signal: the tenant-scoped search integration test returned a valid
  same-tenant row, but not the insertion-order fixture ID.
- Prevention rule: when a query defines ordering, derive the expected fixture
  from that ordering; never use insertion order as an implicit database contract.

## 2026-08-02 — Inbound adapters must share use cases, not controllers

- Failure mode: `AuthController` reused `/api/me` behavior by invoking
  `CurrentUserController`, coupling two inbound adapters and making login
  enrichment depend on an HTTP controller implementation.
- Detection signal: a controller field or constructor parameter referenced
  another controller instead of an application use-case interface.
- Prevention rule: extract shared workflows into one focused application service
  behind a public use-case contract; every inbound adapter delegates to that
  contract and owns only transport translation.

## 2026-08-02 — Routing adapters must not own pool lifecycle

- Failure mode: a routing DataSource can accidentally become responsible for
  tenant pool construction, cache policy, and shutdown when those concerns are
  added directly to the Spring routing adapter.
- Detection signal: routing tests need to inspect pool creation or the routing
  class instantiates Hikari/Caffeine infrastructure itself.
- Prevention rule: keep routing responsible only for selecting the database key
  and delegating target resolution to the capability-owned pool provider; test
  both boundaries independently.

## 2026-08-02 — Unsupported search capabilities must degrade to absence

- Failure mode: the Groq AI provider returned a synthetic zero embedding even
  though it does not support embedding generation.
- Detection signal: a provider implementation returned a fixed-size zero vector
  while its public contract specified an empty result for unavailable embeddings.
- Prevention rule: unsupported embedding providers return an empty result and
  callers skip persistence; never store fabricated vectors that make cosine
  search produce misleading results.

## 2026-08-02 — API version source must be centralized

- Failure mode: allowing each controller or module to choose its own version
  resolver would make the same API contract behave differently across the
  service; retaining a version in both the URI and header would create two
  competing sources of truth.
- Detection signal: multiple `WebMvcConfigurer` implementations configure API
  version resolution or controllers mix headers, query parameters, and paths.
- Prevention rule: configure one header resolver and default in the global MVC
  boundary; keep public routes version-neutral and use Spring mapping versions
  only for real parallel representations. Because the service is pre-release,
  do not retain legacy path aliases unless explicitly required.

## 2026-08-02 — Green Gradle checks can still expose test-context shutdown defects

- Failure mode: module `check` tasks can complete successfully while Spring
  test contexts emit shutdown-time SQL errors for missing `event_publication`
  tables or already-closed schemas.
- Detection signal: `ionShutdownHook` output contains
  `InvalidDataAccessResourceUsageException`, `Table "event_publication" not
  found`, or failed schema-drop statements even though Gradle reports
  `BUILD SUCCESSFUL`.
- Prevention rule: preserve this output in verification reports, isolate the
  failing test profile/context, and close the lifecycle defect before claiming
  final service-wide readiness. Do not suppress the warnings merely to make
  CI output quiet.

## 2026-08-02 — Removing a path version can expose route collisions

- Failure mode: removing `/v1` from public routes caused the tenant resource
  controller and tenant-provisioning request controller to claim the same
  `POST /api/tenants` mapping.
- Detection signal: Spring application-context startup failed with an
  ambiguous handler mapping after the route normalization.
- Prevention rule: after changing a shared route prefix, run an application
  context or MockMvc mapping check and give distinct workflows explicit
  resource/action route names instead of relying on the old version segment to
  separate them.

## 2026-08-02 — Test broker restarts must model application restarts

- Failure mode: a Kafka Testcontainers stop/start test attempted to prove
  Spring Modulith publication recovery inside one cached Spring context. The
  managed `@ServiceConnection` producer remained bound to the test lifecycle,
  so the test timed out even though normal publication worked.
- Detection signal: the broker publication test passed normally, while the
  same-context restart test left an incomplete publication and repeatedly
  reconnected against the managed test bootstrap.
- Prevention rule: verify application-restart recovery through Spring
  Modulith's configured resubmission policy, and run broker-outage chaos with
  independently restarted application and broker processes. Do not use a
  same-context managed-container restart as a production recovery assertion.

## 2026-08-03 — Database UUID ordering must not use Java signed ordering

- Failure mode: a PostgreSQL query ordered UUIDs by the database UUID
  comparator, while the integration test derived the expected first row with
  Java `UUID::compareTo`, which compares signed 64-bit segments.
- Detection signal: the query returned a valid tenant-scoped row, but the test
  expected a different UUID whenever the most-significant bit changed sign.
- Prevention rule: when an integration contract depends on database ordering,
  derive the expected value using the database-compatible canonical UUID text
  ordering or assert only the documented ordering-independent contract.

## 2026-08-03 — Serialize Testcontainers integration contexts when cleanup is global

- Failure mode: running several PostgreSQL-backed Gradle integration tasks in
  parallel caused Testcontainers' shared resource reaper to race, producing
  prune conflicts and shutting down a database while Spring contexts were
  still closing.
- Detection signal: the same isolated integration test passed, while the
  parallel aggregate run reported resource-reaper conflicts and shutdown-only
  connection errors.
- Prevention rule: use `--max-workers=1` for the repository-wide PostgreSQL
  integration gate unless the test-container cleanup strategy is explicitly
  isolated per test task.

## 2026-08-03 — Application failures must not use persistence exception types

- Failure mode: Studio application services constructed JPA's
  `EntityNotFoundException`, leaking persistence vocabulary into use-case
  orchestration and coupling the application layer to Jakarta Persistence.
- Detection signal: the Studio ArchUnit boundary test reported application
  methods depending on `jakarta.persistence` constructors.
- Prevention rule: expected use-case failures belong to the owning module's
  grouped API/domain exception vocabulary; adapters may translate those stable
  exceptions into transport-specific responses.

## 2026-08-03 — Do not ship ineffective container lifecycle changes

- Failure mode: suppressing the Spring bean destroy method on a
  `@ServiceConnection` PostgreSQL container did not remove shutdown-hook
  connection warnings and would have weakened explicit test-resource ownership.
- Detection signal: the focused PostgreSQL integration test still emitted the
  same warnings after the fixture change, while its assertions remained green.
- Prevention rule: reproduce the lifecycle warning, verify the causal owner,
  and revert fixture changes that change ownership without improving shutdown
  ordering; retain the limitation as explicit verification evidence when it is
  caused by the external test harness.

## 2026-08-03 — Disposable test schemas must outlive event callbacks

- Failure mode: H2 `create-drop` removed the Spring Modulith publication table
  before the event registry's shutdown callback queried outstanding
  publications.
- Detection signal: green module tests emitted `event_publication` missing-table
  and failed-schema-drop diagnostics during `ionShutdownHook`.
- Prevention rule: ephemeral test profiles use `ddl-auto: create`; isolate each
  test database at startup and let the framework close connections without a
  competing schema-drop phase.

### Isolate disposable PostgreSQL containers from framework shutdown

- Failure mode: reusable Testcontainers PostgreSQL state could be terminated by
  the resource reaper before Spring Modulith's JDBC publication registry
  completed its shutdown callback.
- Detection signal: successful PostgreSQL integration tests emitted
  `SQLSTATE(08006)`/`57P01` connection-termination diagnostics during
  `eventPublicationRegistry` destruction.
- Prevention rule: shared disposable integration containers must not enable
  `.withReuse(true)`, and the publication registry must be destroyed before
  tenant pool providers and the container bean; verify all dependency contracts
  with focused PostgreSQL tests.

## 2026-08-03 — Publication cleanup must outlive tenant pools

- Failure mode: ordering Spring Modulith publication cleanup only before the
  PostgreSQL container still allowed `TenantDatabasePoolProvider` to close the
  tenant-routed pool first.
- Detection signal: Identity integration passed but logged a broken
  `emme-pool-default` connection during `eventPublicationRegistry.destroy()`.
- Prevention rule: the publication registry must depend on every managed
  connection owner it uses, not only the external container; verify the
  dependency graph and a real PostgreSQL context shutdown.

## 2026-08-03 — Validate test selectors before interpreting Gradle output

- Failure mode: a focused Gradle invocation used the wrong fully qualified test
  package and reported `No tests found`, which initially looked like an
  implementation failure.
- Detection signal: Gradle completed compilation but failed before executing a
  test class, explicitly reporting that no tests matched the include filter.
- Prevention rule: resolve the test package from the source declaration before
  using `--tests`; when the selector is wrong, correct it and rerun before
  diagnosing production code.

## 2026-08-03 — Validation changes require contract-aligned status assertions

- Failure mode: adding `@Valid` changed malformed login input from an internal
  server error to the correct client error, while an existing module test still
  expected a 5xx response.
- Detection signal: the full Identity suite failed only at the old status
  assertion while the boundary test correctly passed.
- Prevention rule: when moving validation to the transport boundary, update
  endpoint tests to assert rejection before use-case invocation and the correct
  4xx status; do not preserve an invalid legacy 5xx contract.

## 2026-08-03 — Condition infrastructure, not required application ports

- Failure mode: conditioning `DatabaseRegistryAdapter` on
  `JdbcConnectionDetails` removed the required `DatabaseRegistryPort` from H2
  and lightweight Spring contexts, so the tenant pool provider could not start.
- Detection signal: fourteen Tenancy application-context tests failed with
  `NoSuchBeanDefinitionException` for `DatabaseRegistryPort`.
- Prevention rule: keep required application ports materialized in every
  supported context; condition only the production infrastructure needed to
  implement the port, and verify both production and H2 wiring explicitly.

## 2026-08-03 — Qualify bootstrap infrastructure explicitly

- Failure mode: an optional JDBC executor dependency selected Shared's default
  tenant-routed executor instead of the dedicated bootstrap executor, recreating
  the datasource initialization cycle during PostgreSQL integration startup.
- Detection signal: the integration context failed while creating
  `dataSourceScriptDatabaseInitializer` through `JdbcTemplate`.
- Prevention rule: bootstrap adapters must depend on the named bootstrap
  executor; optionality must never be used as a substitute for a qualifier at a
  circular infrastructure boundary.

## 2026-08-03 — Disable database-backed schedulers in lightweight tests

- Failure mode: `TenantProvisioningProcessManager` ran from H2/lightweight
  contexts and queried the PostgreSQL-only tenant registry table, producing
  application errors inside otherwise green test runs.
- Detection signal: `Unable to load pending tenant provisioning requests` with
  a bad-grammar error for `emme_core.tenant_registry` during CI.
- Prevention rule: database-backed scheduled components must be guarded by an
  explicit production-default property, and shared ephemeral profiles must
  disable both the scheduler and the guarded component. Production scheduling
  remains enabled and gets its own integration/recovery evidence.

## 2026-08-03 — Exclude generated sources from architecture scans

- Failure mode: a source convention test scanned generated Spotless output
  under a module's `build/` directory and reported a stale controller mapping.
- Detection signal: the test failed against a path containing
  `build/spotless-clean`, even though the corresponding `src/main` controller
  was already normalized.
- Prevention rule: repository source scans must exclude generated/build trees
  before asserting package or naming conventions.

## 2026-08-03 — Recheck suite-order failures before changing persistence

- Failure mode: a full Tenancy suite reported one optimistic-lock failure while
  the isolated test and a clean class execution passed.
- Detection signal: the failure disappeared when the target test was selected
  directly and when the class was rerun with fresh tasks.
- Prevention rule: reproduce a persistence failure in isolation and from a
  clean test execution before changing production mapping or transaction code;
  retain the failure as a service-wide test-stability follow-up when it cannot
  be reproduced deterministically.

## 2026-08-03 — Compose plugin availability differs by Docker installation

- Failure mode: the local Docker CLI accepted `docker` but did not provide the
  `docker compose` subcommand, so an initial validation command did not execute
  Compose parsing.
- Detection signal: Docker reported `unknown shorthand flag: 'f' in -f`, while
  the separately installed `docker-compose` binary was available.
- Prevention rule: fail fast on the command's exit status and validate with the
  repository-supported Compose executable; CI should install or invoke the
  modern Compose plugin explicitly rather than relying on a local fallback.
## 2026-08-03 — Guard optional Gradle task wiring

- **Failure mode:** A convention plugin used `tasks.named("jacocoTestCoverageVerification")`
  even when a functional-test fixture applied the quality convention without a
  Java component, causing configuration to fail before task discovery.
- **Detection signal:** The build-logic functional test failed with `Task with
  name 'jacocoTestCoverageVerification' not found` while the production modules
  passed.
- **Prevention rule:** Convention plugins must configure optional capability
  tasks with `withType<T>().configureEach` or `matching { ... }.configureEach`;
  use `named(...)` only when the plugin contract guarantees that task exists.

## 2026-08-04 — Verify container scans against resolved runtime dependencies

- **Failure mode:** The JVM container built successfully, but the blocking
  Trivy scan found a HIGH PostgreSQL driver CVE that was not visible in the
  source version declaration because dependency management resolved a newer
  runtime version.
- **Detection signal:** The container workflow failed at `Scan image with
  Trivy` while backend and frontend CI remained green.
- **Prevention rule:** Treat the container scan as an independent release gate;
  inspect the resolved runtime dependency graph, update the fixed dependency,
  and refresh Gradle verification metadata before declaring image CI green.

## 2026-08-04 — Mark secondary test constructors explicitly

- **Failure mode:** Adding a package-private constructor for deterministic
  Caffeine-clock tests made Spring application contexts fail with “No default
  constructor found” because the component now had multiple constructors.
- **Detection signal:** The focused unit tests passed, but service-wide module
  context tests failed while instantiating `TenantDatabasePoolProvider`.
- **Prevention rule:** When a Spring component has more than one constructor,
  explicitly annotate the production injection constructor with `@Autowired`;
  keep test-only constructors package-private and verify at least one real
  application context after adding them.

## 2026-08-04 — Make evicted resource cleanup deterministic

- **Failure mode:** A Caffeine tenant-pool entry expired, but its asynchronous
  removal listener had not closed the Hikari datasource when the replacement
  pool was returned.
- **Detection signal:** The pool lifecycle test passed in isolation and failed
  in the full suite/CI with `firstPool.isClosed()` false; the test log showed
  the removal callback running on `ForkJoinPool.commonPool-worker-1`.
- **Prevention rule:** Resource-owning cache removal callbacks must use a
  deterministic executor (inline for this small lifecycle action) so callers
  cannot observe a replacement before the evicted resource is released.

## 2026-08-04 — Match repository time-bound tests to database precision

- **Failure mode:** An appointment saved at an `Instant` with nanoseconds was
  truncated by the database, then excluded by an inclusive lower-bound query
  using the original higher-precision value.
- **Detection signal:** CI intermittently returned one appointment instead of
  two from the artist/date-range repository test; the failure was isolated to
  the exact boundary value.
- **Prevention rule:** Time-based persistence tests must use the precision
  supported by the target database (microseconds for PostgreSQL `TIMESTAMPTZ`)
  when asserting inclusive boundaries.
## 2026-08-04 — Disable asynchronous provider listeners in generic integration contexts

- **Failure mode:** The remote Tenancy module test intermittently failed with an optimistic-lock exception while patching a tenant.
- **Detection signal:** `TenantCreatedConsumer` was active in the shared L4 test context because `app.keycloak.provisioning.enabled` defaulted to `true`; its asynchronous Modulith listener updated the same tenant aggregate during the HTTP assertion.
- **Prevention rule:** Shared in-memory integration profiles must disable asynchronous external-provider listeners by default. Cover the provider workflow with focused tests and enable it only in an explicit provider-integration context.

## 2026-08-04 — Fail closed for external provisioning conditions

- **Failure mode:** A missing `app.keycloak.provisioning.enabled` property enabled an asynchronous external identity listener through `matchIfMissing = true`.
- **Detection signal:** A generic test profile had to override the listener after it raced a tenant update; the same omission could activate provisioning in an incomplete deployment profile.
- **Prevention rule:** External-provider listeners must require an explicit `true` property (`matchIfMissing = false`). Production profiles must opt in deliberately, while test profiles must opt out explicitly.

## 2026-08-05 — Modulith 2.1 cross-module access requires @NamedInterface on every access path

- **Failure mode:** After splitting the monolithic `studio` module into 6 bounded contexts, `ModularityTest.verify()` rejected cross-module access to `domain.model.*` and `application.port.out.*` packages even when the consumer declared bare module access (`"services"`) in `allowedDependencies`.
- **Detection signal:** `Violations: Module 'appointments' depends on module 'services' via ... -> com.emme.services.domain.model.Service. Allowed targets: ... services, ...`
- **Prevention rule:** Modulith 2.1 requires `@NamedInterface` annotations on every leaf package accessed cross-module. Bare module names in `allowedDependencies` do not grant access to sub-packages. Place `@NamedInterface` on `domain/`, `domain/model/`, `application/`, and `application/port/out/` for internal module sharing. Architecture tests must allow `@NamedInterface` in domain packages (it is metadata, not business logic).

## 2026-08-05 — Architecture test per-module boundary requires every consumed package to be exposed

- **Failure mode:** Consumer modules reference named interfaces like `"appointments :: appointments-api"` but the target module's `api/usecase/package-info.java` had no `@NamedInterface` declaration.
- **Detection signal:** `ModularityTest` reported dependencies through unregistered named interfaces.
- **Prevention rule:** Every package consumed cross-module must declare `@NamedInterface` matching the consumer's `allowedDependencies` reference. Add `@NamedInterface` to the leaf package (not just a parent) so Modulith can resolve the access path.

## 2026-08-05 — Migration script file-path heuristics fail on shared suffixes

- **Failure mode:** The migration script used `includes('Service')` to route service-related files, but `ListCustomersService.java` matched the `Service` check before the `Customer` check, sending CRM files to the wrong module.
- **Detection signal:** Files like `ListCustomersService.java` and `ListTenantCustomersService.java` were routed to `services/` instead of `clients/`.
- **Prevention rule:** File-path heuristics must check more specific patterns first (`Customer`, `FindAvailableSlot`) before generic ones (`Service`, `Appointment`). Verify a sample of edge-case filenames before running the full migration.

## 2026-08-05 — git mv into pre-existing ignored directory

- **Failure mode:** `git mv modules/customer modules/clients` nested the tracked module under `modules/clients/customer/` because ignored Gradle build output had already created the destination directory.
- **Detection signal:** The tracked `build.gradle.kts` appeared at `modules/clients/customer/build.gradle.kts` instead of the module root.
- **Prevention rule:** Before directory renames, inspect the destination with `git status --ignored --short` and require it to be absent of source files. When ignored build output exists, move tracked files explicitly.

- **Failure mode:** `git mv modules/customer modules/clients` nested the tracked
  module under `modules/clients/customer` because ignored Gradle build output had
  already created the destination directory.
- **Detection signal:** The tracked `build.gradle.kts` appeared at
  `modules/clients/customer/build.gradle.kts` instead of the module root.
- **Prevention rule:** Before directory renames, inspect the destination with
  `git status --ignored --short` and require it to be absent of source files;
  when ignored build output exists, move tracked files explicitly and verify
  `modules/<new-name>/build.gradle.kts` plus `src/` are at the module root.

## 2026-08-28 — Preserve existing infrastructure before adding AI primitives

- **Failure mode:** A new tenant context abstraction was started before fully
  reconciling it with the existing Spring filter, AI execution scope, Redis,
  and provider infrastructure.
- **Detection signal:** The user explicitly required reuse of existing
  artifacts and asked for the attempted slice to be restored rather than
  replaced or discarded.
- **Prevention rule:** Inventory and reuse existing framework capabilities and
  adapters first. Add a new abstraction only when a documented gap remains,
  and keep it as a thin boundary around the existing implementation.

## 2026-08-29 — Keep contract tests independent of formatting and record accessors

- **Failure mode:** A multiline SQL assertion expected a single-line fragment,
  and a record factory named `admitted()` collided with the generated boolean
  accessor of the same name.
- **Detection signal:** The focused Gradle suite failed during assertion and
  compilation before any unrelated module was changed.
- **Prevention rule:** Assert SQL structure with whitespace-tolerant patterns,
  and avoid static record factories that reuse component accessor names.

## 2026-08-29 — Context-required services need context-aware unit fixtures

- **Failure mode:** The broad assistant check failed in two provider-fallback
  tests because `ChatService` correctly rejected calls without a bound backend
  `AiExecutionContext`.
- **Detection signal:** `IllegalStateException: No AI execution context` in
  `ChatServiceProviderFallbackTest`.
- **Prevention rule:** When a service enforces backend context, all direct unit
  fixtures must bind a minimal valid context with `AiExecutionContextScope`;
  never relax the production guard to accommodate a test.

## 2026-08-29 — Promotion must recheck all evaluation gates

- **Failure mode:** The first promotion policy checked only the canary flag,
  allowing a newly supplied evaluation result to claim canary success while
  earlier safety and regression gates were false.
- **Detection signal:** A regression test promoted an `APPROVED` candidate with
  an incomplete evaluation object.
- **Prevention rule:** Promotion requests must be self-contained and revalidate
      every prerequisite gate plus the canary result; do not rely only on the
      candidate's current status.

## 2026-08-29 — Verify dependency metadata after adding Spring AI modules

- **Failure mode:** Gradle dependency verification blocked the new Redis
  VectorStore and tool-search artifacts before compilation could run.
- **Detection signal:** The compile task reported missing SHA-256 verification
  metadata for newly resolved transitive artifacts.
- **Prevention rule:** After adding a dependency, run the repository's approved
  verification-metadata writer once, review the XML diff, then rerun compilation
  and tests with verification enabled.

## 2026-08-29 — Keep Redis semantic acceleration rebuildable

- **Failure mode:** Treating a Redis vector result as authoritative would allow
  stale or role-inappropriate data to bypass PostgreSQL policy checks.
- **Detection signal:** The Redis projection has no durable transaction or
  complete role history and can survive independently of application state.
- **Prevention rule:** Write PostgreSQL first, project to Redis best effort, and
  re-confirm every cache hit through the durable tenant/principal resolver.

## 2026-08-29 — Preserve the canonical constructor for bound record properties

- **Failure mode:** Adding an overloaded constructor to a record annotated with
  `@ConfigurationProperties` made Spring Boot fail to bind the bean because it
  could no longer select the canonical constructor.
- **Detection signal:** Full application-context tests failed with `No default
  constructor found` before any controller assertions ran.
- **Prevention rule:** Keep configuration records to one bindable canonical
  constructor; update direct test fixtures to pass all fields instead of adding
  convenience constructors.

## 2026-08-29 — Keep vector integration tests narrower than the full application

- **Failure mode:** A full `TestApplication` pgvector test re-entered the
  repository's existing tenant-routing/JPA datasource-initialization cycle.
- **Detection signal:** Spring context startup failed before the vector adapter
  was exercised, with `BeanCurrentlyInCreationException` for the datasource
  initializer.
- **Prevention rule:** For infrastructure operators, use the smallest real
  integration harness that still instantiates the production adapter. Keep full
  application bootstrap tests separate from focused PostgreSQL/vector tests.

## 2026-08-29 — Validate integration fixtures against the production schema

- **Failure mode:** The first cache fixture omitted a production default for
  generated IDs and required columns, causing false database failures.
- **Detection signal:** PostgreSQL rejected the test fixture before the adapter
  query ran (`null value in column ... violates not-null constraint`).
- **Prevention rule:** When a test creates a minimal table, copy every column
  default and constraint used by the production SQL, then exercise write/read
  behavior against the live extension.

## 2026-08-29 — Treat Apache AGE as a fixed-query PostgreSQL boundary

- **Failure mode:** The first AGE JDBC call used a bound parameter cast to
  PostgreSQL `name`, and Spring's named-parameter parser was incompatible with
  Cypher labels and relationship syntax. AGE rejected the call with `a name
  constant is expected`.
- **Detection signal:** The real AGE Testcontainers test failed before graph
  projection, while the same SQL worked only after using a validated SQL
  literal for the graph name and a dollar-quoted Cypher string.
- **Prevention rule:** Keep AGE graph names backend-derived and validate them
  before safely rendering a SQL literal; use positional JDBC for AGE queries,
  dollar-quote Cypher, load AGE, and set its search path inside the same
  transaction. Never accept model-generated Cypher.

## 2026-08-29 — Export reused tenant infrastructure explicitly

- **Failure mode:** The optional AGE configuration reused tenancy’s internal
  JDBC adapters but added new Modulith violations because the package was not a
  declared public boundary.
- **Detection signal:** The architecture test reported the new AGE config as a
  dependency on unnamed tenancy internals, while the existing tenant JDBC
  configuration had the same latent issue.
- **Prevention rule:** When another module must reuse an existing adapter,
  expose that package as a named interface and list the exact named interface
  in the consumer’s `allowedDependencies`; do not suppress the architecture
  test or duplicate the infrastructure.

## 2026-08-31 — Pair PostgreSQL update queries with RETURNING when reading outcomes

- **Failure mode:** A JDBC metrics transition changed `UPDATE` to `query` so it
  could classify retry versus dead-letter outcomes, but initially omitted
  PostgreSQL's `RETURNING` clause.
- **Detection signal:** The live Testcontainers retry test failed with
  `No results were returned by the query` before the state transition could be
  observed.
- **Prevention rule:** When a PostgreSQL `UPDATE` is executed through a JDBC
  read API, include an explicit `RETURNING` projection and cover the returned
  state in a live database test.
# Task 5 final-review verification lesson — 2026-08-31

- Failure mode: concurrent Gradle invocations corrupted the local incremental build outputs/cache and produced misleading missing-class failures.
- Detection signal: one build attempted to delete `modules/assistant/build/classes` while another was compiling, followed by an incomplete cached `compileJava` result.
- Prevention rule: run Gradle verification commands serially; if build outputs are incomplete, force a clean/`--rerun-tasks` Java 25 compilation before diagnosing source failures.

## 2026-09-04 — Do not overlap explicit Gradle verification with push hooks

- **Failure mode:** An explicit broad test run overlapped with the repository-wide Gradle hook started by `git push`, creating duplicate Gradle workers and making completion status harder to attribute.
- **Detection signal:** Two Gradle wrapper processes and workers for the same repository were visible simultaneously.
- **Prevention rule:** After a commit, let the push hook finish before starting another Gradle command; use `jps -lv` to confirm the previous invocation has exited.

## 2026-09-04 — Keep provider substitution behind ports

- **Failure mode:** A gradual persistence refactor started prescribing `Postgres*` classes as canonical implementation names.
- **Detection signal:** The active design, plan, ledger, and configuration-test references named a concrete provider instead of only the application capability.
- **Prevention rule:** Define and test the stable application port first; keep JPA, `JdbcClient`, Redis, Spring AI, LangGraph, and provider implementations behind that port and select them at the composition root.

## 2026-09-04 — Assert successful dependency-path behavior, not only failures

- **Failure mode:** A boundary test for replacing `JdbcTemplate` with
  `JdbcClient` passed because both the intended and old dependency paths
  produced the same failure.
- **Detection signal:** The failure test did not assert the selected bean/client
  or a successful returned value.
- **Prevention rule:** Every dependency-boundary replacement test must include a
  successful behavior assertion and verify the new dependency path; failure
  tests alone are insufficient.

## 2026-09-04 — Keep classification guardrails aligned with documented reasons

- **Failure mode:** A valid JDBC survivor classification used “atomic review
  transition,” but the guardrail test accepted only a narrower list of reason
  phrases.
- **Detection signal:** The full assistant suite failed in the migration-ledger
  classification test after the documentation was made more precise.
- **Prevention rule:** When adding a new architectural classification reason,
  update the executable vocabulary test and the plan’s reason list in the same
  change.

## 2026-09-04 — Verify the active branch before applying restored work

- **Failure mode:** Restored appointment files were applied while the workspace was on an unrelated branch whose tracked source tree did not contain the appointment module.
- **Detection signal:** The module compiler reported missing appointment packages and Git showed an active branch change with untracked appointment paths.
- **Prevention rule:** Before resuming preserved work, verify branch, merge state, and tracked source layout; preserve restored files temporarily, switch to the intended feature branch, and only then apply changes.

## 2026-09-04 — Keep collision pre-checks separate from concurrency enforcement

- **Failure mode:** A bounded JPA existence query could be mistaken for a complete concurrent-booking fix even though two transactions can pass the pre-check.
- **Detection signal:** The optimized query improved allocation, but no PostgreSQL constraint and live concurrent-write assertion existed.
- **Prevention rule:** Treat query optimization and database invariant enforcement as separate plan gates; do not mark collision safety complete until a real PostgreSQL concurrency test proves the invariant.

## 2026-09-04 — Configure dynamic tenant scope at connection checkout

- **Failure mode:** Tenant-qualified JPA methods were added to ordinary CRUD
  even though the tenant schema was already selected by the persistence boundary.
- **Detection signal:** The Hikari cache is keyed by `databaseId`, while a shared
  database can serve multiple tenant schemas; a pool initialization SQL value
  therefore cannot safely represent the current tenant.
- **Prevention rule:** Apply the validated schema and tenant session setting when
  a connection is acquired. Use ID-only JPA CRUD inside that boundary; retain
  explicit tenant parameters only for shared/control-plane, cross-tenant, or
  operation-specific invariants.

## 2026-09-04 — Close existing test blocks before inserting new cases

- **Failure mode:** A new repository guard test was inserted before the closing brace of an existing `try` block, so compilation failed before the intended assertion ran.
- **Detection signal:** The compiler reported `';' expected` at the new test method signature.
- **Prevention rule:** After inserting a test into an existing method, inspect the surrounding numbered lines and compile the focused test before interpreting behavioral failures.

## 2026-09-04 — Reuse Spring-managed infrastructure instances

- **Failure mode:** A Spring-managed tenant resolver was added while the tenant
  data-source factory continued constructing a second resolver directly.
- **Detection signal:** The composition root had both a component/customizer
  instance and a `new TenantIdentifierResolver(...)` call, which would split
  schema-cache state and make Hibernate use a different instance.
- **Prevention rule:** When replacing service-locator construction with Spring
  DI, search for all direct constructions and inject the managed component
  through every composition root before committing.

## 2026-09-05 — Keep feature setup in feature-owned test fixtures

- **Failure mode:** A generic testing fixture accumulated tenant, subscription,
  salon, and identity production dependencies to support full-context tests.
- **Detection signal:** The fixture dependency graph showed feature setup in
  `libraries/testing`, and a concrete Keycloak adapter subclass was used as a
  test double.
- **Prevention rule:** Keep generic MockMvc/security/repository helpers in the
  shared testing library; move tenant-aware setup to tenancy fixtures and
  provider fakes to the owning provider module, consuming only stable ports.

## 2026-09-05 — Explicitly import feature fixtures for direct consumers

- **Failure mode:** Removing tenant bootstrap configuration from the generic
  web fixture left a tenancy-owned web test with asynchronous Liquibase
  failures.
- **Detection signal:** The full verification hook logged a missing
  `db/emme-studio/changelog.yaml` while the direct `TenantWebTest` still
  extended only the generic web base.
- **Prevention rule:** Every direct feature-specific test must import its
  owning fixture explicitly; generic test bases must not carry feature
  configuration implicitly.

## 2026-09-05 — Keep verification commands aligned with Gradle task ownership

- **Failure mode:** The framework-first plan referenced a root
  `dependencyAnalysis` task that this multi-project build does not expose.
- **Detection signal:** Gradle task discovery and the successful per-project
  `computeAdvice` runs showed dependency analysis is registered by module.
- **Prevention rule:** Validate plan commands against `./gradlew tasks` and use
  the owning project task path; do not assume a root aggregate exists.

## 2026-09-05 — Validate active deployment overlays before deleting backups

- **Failure mode:** A stale Compose backup can obscure which environment
  configuration is authoritative and can be removed without proving the active
  overlay still contains the required dependencies.
- **Detection signal:** The active and backup overlays differed in migration,
  Keycloak, tenant-seeding, and healthcheck configuration.
- **Prevention rule:** Run the active Compose contract and inspect the diff
  before deleting a backup; retain only the tested authoritative overlay.

## 2026-09-05 — Resolve changelog includes relative to their owning catalog

- **Failure mode:** A migration-catalog test treated changelog-relative include
  paths as repository-relative paths and reported every valid migration as
  missing.
- **Detection signal:** The failure listed real `emme-core` and `emme-studio`
  files with the catalog directory omitted from the resolved path.
- **Prevention rule:** Resolve each Liquibase include against the parent
  directory of the changelog that declares it, then assert existence and
  uniqueness.

## 2026-09-05 — Use qualified names for deletion inventory checks

- **Failure mode:** A deletion guard treated an Identity replacement with the
  same simple class name as a caller of the deleted tenancy implementation.
- **Detection signal:** The guard failed on `EnsureTenantMembershipService` in
  Identity even though the ledger path belonged to Tenancy.
- **Prevention rule:** Resolve candidate references using the implementation's
  package-qualified name and only use the simple name inside its own package.

## 2026-09-05 — Remove only redundant tenant predicates

- **Failure mode:** A tenant-schema JPA lookup could retain `tenantId` as a
  query key even though the connection had already selected the tenant schema.
- **Detection signal:** Calendar sync state was tenant-local, but its repository
  method was still named `findByTenantIdAndProvider`.
- **Prevention rule:** Remove tenant IDs from schema-local lookup signatures while
  retaining them in domain state, creation, response, RLS, and any shared or
  business-key operation that still needs explicit scope.

## 2026-09-05 — Use the domain vocabulary in persistence regressions

- **Failure mode:** A new CatalogItem adapter regression used an `INACTIVE`
  status that the domain enum does not define.
- **Detection signal:** Test compilation failed before exercising the intended
  managed-update behavior.
- **Prevention rule:** Read the aggregate enum and existing transition tests
  before writing fixtures; use the actual domain vocabulary so a red test proves
  behavior rather than a fixture typo.

## 2026-09-05 — Reset Mockito beans in cached Spring integration contexts

- **Failure mode:** A later integration test inherited accepted-retrieval stubs
  from an earlier test and incorrectly invoked the grounded answer port.
- **Detection signal:** The rejection test received the prior test's accepted
  documents despite configuring an empty retrieval result.
- **Prevention rule:** Reset mutable Mockito beans in `@BeforeEach` when a
  `SpringJUnitConfig` context is cached across test methods.

## 2026-09-05 — Verify the regression test is genuinely red

- **Failure mode:** The first proposed checkpoint authorization test passed
  before the production change because that branch already rejected
  same-tenant but unauthorized runs.
- **Detection signal:** The focused test command was green before any
  production edit, contradicting the required Red phase.
- **Prevention rule:** Always run the exact new test before implementation and
  inspect the mocked branch when it passes unexpectedly; revise the test to
  represent the uncovered behavior before changing production code.

## 2026-09-05 — Keep one Spring constructor autowired

- **Failure mode:** Adding a new optional dependency path left two
  `RagQueryService` constructors annotated with `@Autowired`, preventing every
  web application context from starting.
- **Detection signal:** The full Assistant checkpoint failed with Spring's
  `Invalid autowire-marked constructor` error before any web test executed.
- **Prevention rule:** When retaining convenience constructors for unit tests,
  annotate only the single composition-root constructor and add a reflection
  guard for constructor selection.

## 2026-09-05 — Include the RestClient auto-configuration dependency

- **Failure mode:** A capability-scoped `RestClient` bean compiled and passed
  isolated builder tests but prevented the Notification module context from
  starting because no managed `RestClient.Builder` existed.
- **Detection signal:** The full module checkpoint failed with
  `NoSuchBeanDefinitionException` for `RestClient.Builder`.
- **Prevention rule:** When adding a Spring-managed RestClient boundary to a
  module, verify the module's full application context and declare the Boot
  RestClient dependency that supplies the managed builder.

## 2026-09-05 — Retain compatibility beans until all callers migrate

- **Failure mode:** Removing Calendar's legacy Google HTTP bean during the
  RestClient client migration broke Spring contexts while sync adapters still
  injected the legacy wrapper.
- **Detection signal:** Full Calendar tests failed with
  `NoSuchBeanDefinitionException` for `GoogleHttpClient`, even though the new
  provider contract tests passed.
- **Prevention rule:** Keep the compatibility bean through the planned caller
  migration slice, then delete it only after production callers, tests, and
  dependency searches are clean.
## 2026-09-05 — Prepared-query migrations must move failure injection to the query boundary

- **Failure mode:** After removing cache-owned embedding, a cache test kept a
  mocked resolver and embedding failure, so the intended durable-cache
  failure/trace path was never exercised.
- **Detection signal:** Focused tests either produced identical cache keys for
  different embedding versions or reported zero trace interactions.
- **Prevention rule:** When ownership moves across a boundary, update fixtures
  to inject failures at the new owner and use distinct prepared values for
  identity-sensitive assertions.

## 2026-09-05 — Preserve compatibility edge cases during canonical contract migration

- **Failure mode:** A canonical chat request initially rejected blank user
  messages, changing the existing mock endpoint behavior from a graceful 200
  response to a 404 handled by the shared exception mapper.
- **Detection signal:** The full Assistant test checkpoint failed in
  `AiModuleTest.shouldHandleEmptyMessageGracefully` after focused adapter tests
  were already green.
- **Prevention rule:** Before replacing a compatibility path, run its boundary
  tests, including empty and provider-specific graceful-degradation cases; keep
  validation strict for required metadata while preserving documented input
  semantics.

## 2026-09-05 — Remove interface-only members with the compatibility family

- **Failure mode:** Removing the deprecated composite interface left an
  `@Override` annotation on a mock helper that no longer implemented a parent
  method.
- **Detection signal:** Focused compilation failed with “method does not
  override or implement a method from a supertype.”
- **Prevention rule:** When deleting an interface family, inspect each
  implementer for inherited methods and annotations in the same slice before
  running the broader gate.

## 2026-09-05 — Import contract records from their owning package

- **Failure mode:** A new cross-package contract test referenced appointment and payment records without importing their owning packages.
- **Detection signal:** Focused test compilation reported every record type as missing even though production sources had compiled.
- **Prevention rule:** When a contract test lives in a shared workflow package, import each record from its owning bounded-context package before interpreting compiler failures as production defects.
## 2026-09-05 — Separate schema routing from row-level tenant defense

- Failure mode: treating a tenant-schema table's `tenant_id` column as if it selected the schema.
- Detection signal: review question about whether `appointment_hold` belongs to the tenant schema.
- Prevention rule: document that connection checkout selects the tenant schema; retain a tenant column only when it is an intentional existing RLS/uniqueness defense, and never pass it through ordinary schema-local repository methods.

## 2026-09-06 — Run architecture gates after adding application services

- **Failure mode:** New hold, release, and payment-workflow application services
  initially lacked the repository-required transaction annotation, and Assistant
  composition referenced Payment internals across a module boundary.
- **Detection signal:** The application architecture tests reported missing
  `@Transactional` policy and cross-module dependencies on `payment.application`.
- **Prevention rule:** Add transaction policy and public API contracts in the
  same TDD slice as every new application service; run the application
  architecture/convention gate before committing the slice.

## 2026-09-06 — Gate graph-only adapters as one composition boundary

- **Failure mode:** New payment workflow adapters were component-scanned in
  ordinary Assistant contexts even though their qualified `tenantJdbcClient`
  exists only in the LangGraph composition path.
- **Detection signal:** Full Assistant tests failed with missing
  `tenantJdbcClient`, then with missing graph-only ports after the first adapter
  was gated.
- **Prevention rule:** Apply the same `app.ai.langgraph.enabled` condition to
  every graph-only adapter and its source adapter; run the full Assistant suite
  after focused configuration tests. Also verify `@Transactional` services are
  proxyable and not final.

## 2026-09-06 — Keep Gradle test filters off compile tasks

- **Failure mode:** A combined Gradle invocation applied `--tests` to a
  `compileJava` task and failed before executing the requested gate.
- **Detection signal:** Gradle reported `Unknown command-line option '--tests'`
  while configuring `:modules:appointments:compileJava`.
- **Prevention rule:** Run filtered test tasks separately from compile tasks, or
  place test filters only on the test invocation; never assume task-specific
  options apply uniformly across a multi-task Gradle command.

## 2026-09-06 — Validate tenant identifiers before registry writes

- **Failure mode:** The migration script accepted a digit-starting or overlong
  seed slug, wrote provisioning state, and only rejected the derived schema in
  a later loop.
- **Detection signal:** The migration script contract lacked a PostgreSQL-safe
  start-character and length assertion.
- **Prevention rule:** Validate tenant slug shape and PostgreSQL identifier
  length before any registry insert; keep the later schema validation as a
  defense in depth.

## 2026-09-06 — Keep workflow composition on public Modulith APIs

- **Failure mode:** Assistant appointment/payment workflow composition imported
  appointment repositories/domain objects directly, and Payment API leaf
  packages were not all registered under the named interface consumed by
  Assistant.
- **Detection signal:** `ModularityTest.moduleStructureIsValid()` reported
  dependencies through appointment internals and Payment API classes even
  though the modules declared named-interface access.
- **Prevention rule:** Cross-module workflow composition must consume public
  use cases and result records owned by the target module. Annotate every
  consumed leaf package with the declared `@NamedInterface`, then run the
  Modulith and full application architecture gates before committing.

## 2026-09-06 — Keep transactional application services proxyable

- **Failure mode:** The new `GetAppointmentHoldService` was final, so Spring
  could not create the CGLIB proxy required by its class-level
  `@Transactional(readOnly = true)` annotation.
- **Detection signal:** Existing appointment web/module contexts failed during
  startup with `Cannot subclass final class GetAppointmentHoldService`.
- **Prevention rule:** Any class-based Spring transactional application service
  must remain proxyable, or use an interface-based proxy configuration; run a
  real application context test after adding the service bean.

## 2026-09-06 — Migrate compatibility constructors across all test source sets

- **Failure mode:** Removing semantic-cache identity fallback constructors made
  unit tests pass but left pgvector integration-test sources uncompilable.
- **Detection signal:** `compileIntegrationTestJava` reported the old
  `Lookup`/`Put` arities after the focused unit suite was green.
- **Prevention rule:** Before deleting a compatibility constructor, search and
  compile production, unit-test, integration-test, and fixture source sets;
  migrate every caller to the explicit contract before the deletion commit.

## 2026-09-06 — Keep cache invalidation targets structured

- **Failure mode:** Semantic-cache invalidation accepted a raw cache-kind string
  and reconstructed tenant/principal/manual policy inside a default port method.
- **Detection signal:** The compatibility inventory found one production caller
  and test doubles still invoking `invalidate(String)` despite the structured
  `SemanticCacheInvalidation` contract already being available.
- **Prevention rule:** Cross-cutting invalidation ports must accept explicit
  tenant, principal, dependency, and version data; callers at the authenticated
  boundary should construct that value before invoking the adapter.
## 2026-09-06 — Canonical bridges must preserve compatibility semantics while callers migrate

- Failure mode: the first canonical selector implementation unconditionally
  required an execution context and broke existing temporary string callers
  that intentionally run without a scheduler/context.
- Detection signal: the focused selector suite failed with `No AI execution
  context` before reaching provider behavior.
- Prevention rule: when adding a canonical boundary beside a compatibility
  boundary, test both contracts explicitly and preserve the old contract's
  documented behavior until every caller has migrated.

## 2026-09-06 — Canonical capability composition must identify the policy owner

- Failure mode: exposing a canonical provider capability from both the raw mock
  provider and the selector made Spring injection ambiguous.
- Detection signal: web-context tests failed with two `AiChatCompletion` beans
  (`mockModelProvider` and `aiLegacyChatCompletion`).
- Prevention rule: when a raw provider and a policy/composition adapter share a
  capability, mark the policy owner as primary and add an application-context
  test proving consumers resolve the selector.

## 2026-09-06 — Canonical consumers must preserve response identity explicitly

- Failure mode: migrating a consumer to a canonical completion port while
  retaining an identified-port branch allowed cache writes to fall back to
  synthetic provider/model names and the old overloaded cache API.
- Detection signal: the source-architecture test found the deprecated port,
  `IdentifiedChatCompletionPort`, and `legacy-provider` / `legacy-model` still
  present in `ChatService`; the focused behavior test found a three-argument
  cache-write assertion after the production call had become structured.
- Prevention rule: canonical consumers must send the bound execution context
  and provider policy in one request, use provider/model metadata from the
  canonical response, and construct explicit cache identity for every write.

## 2026-09-06 — Delete compatibility ports only after source-set migration

- **Failure mode:** Production adapters had moved to the canonical chat
  capability, but Assistant tests still depended on the deprecated
  string-returning port and its identity wrapper.
- **Detection signal:** A repository-wide source search found legacy imports in
  ChatService, RagQueryService, and contract tests after focused composition
  tests were green.
- **Prevention rule:** Before deleting a compatibility interface, search and
  compile every production, unit-test, integration-test, fixture, bean, and
  source-inventory caller; migrate test doubles to the canonical request/result
  shape, then let a deletion test prove the old files are absent.

## 2026-09-06 — Distinguish default composition from legacy compatibility

- **Failure mode:** A pre-release fallback configuration was named `Legacy`,
  even though it is the default mock/basic provider path when enhanced Spring
  AI chat is disabled.
- **Detection signal:** The application profile sets
  `EMME_SPRING_AI_CHAT_ENABLED=false`, so deleting the configuration outright
  would remove the default selector, scheduler, and tracing composition.
- **Prevention rule:** Before deleting a configuration called legacy, inspect
  active defaults and deployment properties. If the behavior is still required,
  rename it after its runtime role and remove historical terminology instead of
  deleting a necessary composition root.

## 2026-09-06 — Delete isolated contract families only after source inventory

- **Failure mode:** Deprecated routing contracts and a compatibility tool-risk
  enum remained in the framework-neutral library after Assistant had moved to
  its own semantic routing and tool policy boundaries.
- **Detection signal:** A repository-wide Java source search found references
  only in the contracts' own test and the deprecated source files; no
  production, bean, reflection, or build caller remained.
- **Prevention rule:** Add a failing source-path deletion assertion, remove the
  obsolete contract test with the family, and compile downstream consumers
  before recording the family as deleted in the migration ledger.

## 2026-09-06 — Remove test-only compatibility constructors separately

- **Failure mode:** A public controller constructor remained only to support a
  unit test for a pre-durable composition, making the supported construction
  surface broader than the active Spring composition.
- **Detection signal:** Reflection showed the four-argument constructor had no
  production or bean caller; only one test instantiated it.
- **Prevention rule:** Prove constructor usage across all source sets, add an
  API-surface test first, migrate the isolated test to the active composition,
  and preserve endpoint behavior before deleting the shortcut.

## 2026-09-06 — Measure JPA list queries after flushing writes

- **Failure mode:** A repository list test could report zero queries because
  Hibernate statistics were disabled, leaving N+1 regressions invisible.
- **Detection signal:** The first client query-count assertion returned zero
  prepared statements even though `findAll()` had executed.
- **Prevention rule:** Enable Hibernate statistics only in the repository-test
  profile, flush pending writes before clearing statistics, and assert the
  exact read-query count for each aggregate list path.

## 2026-09-06 — Keep generic fixture boundaries executable

- **Failure mode:** Fixture ownership can regress silently after a feature setup
  is moved out of the generic testing library.
- **Detection signal:** The repository had tenancy-owned boundary tests but no
  generic-library test scanning fixture source and build dependencies together.
- **Prevention rule:** Keep a source and build-file architecture test in the
  generic testing library so feature package/provider leakage fails at the
  owning boundary.

## 2026-09-06 — Make deployment assumptions part of CI validation

- **Failure mode:** Workflow and manifest contracts were validated by separate
  checks, leaving probe, non-root, and migration-secret wiring assumptions
  undocumented in the backend quality job.
- **Detection signal:** No CI step asserted that Kubernetes deployment and
  migration manifests matched the health and secret assumptions used by the
  application.
- **Prevention rule:** Keep one lightweight deployment-contract validator in
  the quality lane and reserve Kubernetes/Compose execution for the infrastructure
  phase gate.

## 2026-09-06 — Treat dependency-analysis advice as evidence

- **Failure mode:** Automated dependency advice can suggest removing framework
  transitives or source-set-specific test dependencies that are still required
  by conventions or affected tests.
- **Detection signal:** Java 25-compatible analysis completed successfully, but
  remaining recommendations mapped to convention-owned transitives and test or
  integration classpaths rather than proven dead production dependencies.
- **Prevention rule:** Review advice against source usage and every affected
  classpath; apply only measured removals and record when retaining a dependency
  is the safer decision.
