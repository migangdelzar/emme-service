# Engineering lessons

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
- Detection signal: the focused Identity build reported that `FeatureFlagInfo`
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
  such as `RecordAuditEventService`, and keep reserved metadata-only modules
  free of speculative implementation layers.

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
  the container bean; verify both contracts with focused PostgreSQL tests.

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
