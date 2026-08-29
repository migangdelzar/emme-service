# Engineering lessons

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
