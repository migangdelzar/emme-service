# Paketo, JVM AOT Cache, and Native Image Delivery Design

| Field | Detail |
|---|---|
| Status | Proposed for implementation planning |
| Date | 2026-09-05 |
| Repository | `emme-service` |
| Application | `applications:emme-platform` |
| Supersedes | Image-packaging portions of `2026-08-03-service-runtime-deployment.md` |

## 1. Purpose and decision boundary

This design makes the service's JVM container packaging, Java 25 JVM AOT
cache, and GraalVM Native Image lanes explicit and comparable. It is limited to
build logic, OCI image creation, runtime resource policy, image healthchecks,
CI contracts, and deployment documentation. It does not change business
modules, application behavior, provider selection, or production promotion
policy beyond adding evidence gates for runtime variants.

The default artifact remains a JVM image. A JVM image with an embedded AOT
cache and a Native Image are opt-in variants until runtime evidence shows that
either variant is appropriate for deployment.

## 2. Current baseline

The application applies `emme.spring-application` and `emme.container`.
Spring Boot creates the `bootBuildImage` task, and `mise` plus GitHub Actions
invoke that task for image creation. The image therefore uses Spring Boot's
Cloud Native Buildpacks integration. The repository does not currently pin a
Paketo builder or configure Buildpacks cache/image options explicitly.

The current runtime lanes are:

| Lane | Current behavior | Intended role |
|---|---|---|
| JVM | `bootBuildImage`, normal executable Boot JAR | Default development, CI, and rollback artifact |
| JVM + AOT cache | Not configured | Optional startup/memory experiment |
| Native | Application-edge `emme.native-image`, `BP_NATIVE_IMAGE=true` in manual CI | Optional low-memory/fast-start experiment |

The custom `emme.container` convention registers Docker/Podman-oriented tasks,
but its image properties are not the source of truth for `bootBuildImage`.
The implementation must reconcile these responsibilities so image creation is
not configured in two competing ways.

## 3. Alternatives considered

### Option A — Configure Spring Boot's `BootBuildImage` through existing build logic

Extend the existing repository container convention with a typed image policy
that configures Spring Boot's `BootBuildImage` task. Paketo remains the image
builder and owns its supported Java runtime behavior.

Advantages:

- Keeps the supported Spring Boot and Cloud Native Buildpacks path.
- Reuses Paketo's Java memory calculator, runtime launcher, SBOM, and layer
  behavior.
- Allows one repository-owned place to select builder, cache, AOT-cache, and
  native-image modes.
- Avoids a custom Dockerfile and a second runtime assembly implementation.

Cost: the existing `emme.container` convention must be clarified and tested;
its generic Docker/Podman tasks must not silently diverge from
`bootBuildImage`.

### Option B — Add a separate `emme.buildpack-image` convention

Create a new convention dedicated to Spring Boot Buildpacks and leave the
existing generic container convention unchanged.

Advantages:

- Clearer ownership if generic container tasks are needed by other projects.
- Smaller changes to the existing container implementation.

Cost: introduces another application-edge plugin and two image configuration
surfaces. It is justified only if the existing convention cannot be safely
split or extended.

### Option C — Replace Buildpacks with custom Dockerfiles

Build an exploded JVM application image, train and package the AOT cache
manually, and maintain a separate native Dockerfile.

Advantages: maximum control over filesystem layout, launch command, and image
contents.

Cost: duplicates Paketo's memory/runtime behavior, increases security and
maintenance work, and makes JVM/native parity harder to validate.

### Decision

Use Option A as the default implementation direction. Do not create a
Paketo-specific plugin or reimplement the Buildpacks lifecycle. A new
repository convention is allowed only if implementation discovery proves that
the existing `emme.container` plugin cannot own the configuration without
retaining conflicting behavior.

## 4. Runtime modes and data flow

```text
                   ┌─────────────────────────┐
                   │ applications:emme-     │
                   │ platform                 │
                   └────────────┬────────────┘
                                │ bootBuildImage
                                ▼
                   ┌─────────────────────────┐
                   │ Paketo builder           │
                   │ pinned by build policy  │
                   └───────┬─────────┬────────┘
                           │         │
                 JVM       │         │ BP_NATIVE_IMAGE=true
                           │         │
                           ▼         ▼
                   JVM run image   Native run image
                           │
             BP_JVM_AOTCACHE_ENABLED=true
                           │
                           ▼
                    JVM + AOT cache
```

The Gradle application edge selects one mode. The same image task and image
naming contract are used for all modes, with distinct immutable tags for JVM,
JVM+AOT-cache, and native artifacts.

The buildpack environment has two separate classes of configuration:

- Build-time `BP_*` variables select the Java version, native image behavior,
  and AOT-cache generation.
- Launch-time `BPL_*` variables and standard JVM variables control runtime
  behavior. Deployment manifests provide these values and the cgroup limits.

## 5. Memory and resource policy

Container isolation is enforced by Docker or Kubernetes memory cgroups. Paketo
does not reserve memory for the JVM or enforce the container limit; its Java
memory calculator sizes the JVM within the enforced limit.

For JVM images, Paketo calculates heap approximately as:

```text
heap = container memory limit - calculated non-heap - configured headroom
```

Non-heap includes direct memory, metaspace, reserved code cache, and thread
stacks. The implementation will expose the relevant Paketo runtime controls,
including `BPL_JVM_HEAD_ROOM`, `BPL_JVM_THREAD_COUNT`,
`BPL_JVM_LOADED_CLASS_COUNT`, and `BPL_JVM_CLASS_ADJUSTMENT` where needed.

The deployment resource limit is the authoritative cap. Requests remain
scheduling hints. Existing limits must be retained and tested:

- Compose default: `512M`.
- Compose E2E: `1G`.
- Kubernetes backend: `2Gi`.

The application may select a garbage collector such as ZGC through
`JAVA_TOOL_OPTIONS`, but heap sizing must not be duplicated with a hard-coded
`-Xmx`. The current `-XX:MaxRAMPercentage=75` setting must be evaluated against
Paketo's generated flags and either removed or documented as an intentional,
measured override. The default policy is Paketo-owned heap/non-heap sizing with
explicit, environment-specific headroom.

Native images do not use the JVM memory calculator. Their RSS, native heap,
thread, and allocator behavior must be measured independently under the same
container limit.

## 6. JVM AOT-cache variant

The JVM AOT-cache variant is enabled by a repository-owned opt-in property,
for example `-Pemme.jvm-aot-cache=true`, which configures
`BP_JVM_AOTCACHE_ENABLED=true` for `bootBuildImage`. The exact property name is
part of the implementation contract and must be centralized in build logic.

The Paketo Spring Boot support performs a training run during image creation,
stores the cache in the image, and launches the application with the cache.
The image must use Java 25 or newer. The cache is valid only for the matching
application contents and Java version, so the immutable image tag is the cache
invalidation boundary.

The training run must not contact production or mutable external services.
The implementation must provide safe training-run configuration for database,
Redis, identity, messaging, and AI/provider connections. If the application
cannot safely train with its current startup graph, the AOT-cache variant must
fail clearly rather than silently producing a cache with incomplete coverage.

Spring JVM AOT processing (`org.springframework.boot.aot`) is a separate,
optional experiment. It may be combined with the JVM AOT cache, but it must
not become the default without testing profile, conditional-bean, and
environment-specific configuration restrictions.

## 7. Native Image variant

The existing application-edge opt-in remains the Native Image switch. Applying
`org.graalvm.buildtools.native` causes Spring Boot to configure AOT tasks and
connect `processAot` output to Native Image compilation. The native image
Buildpacks lane sets `BP_NATIVE_IMAGE=true`. Direct `nativeCompile` remains a
developer/diagnostic path requiring a GraalVM toolchain.

Native configuration must:

- use a Java 25-compatible, explicitly selected builder;
- retain `fallback=false` / no JVM fallback semantics;
- validate Spring AOT output and reachability metadata;
- run the critical health, authentication, tenancy, persistence, and core
  workday smoke suite;
- publish only an immutable native tag when manually approved;
- retain the JVM artifact as rollback until evidence is accepted.

The Native Image buildpack and direct Gradle Native Build Tools lane are
related but not interchangeable acceptance paths: the former proves OCI image
creation, while the latter proves local native compilation and test task
integration.

## 8. Build-logic ownership

The implementation should introduce one typed repository-owned image policy,
either as an extension on `emme.container` or as a narrowly scoped convention
extracted from it. Its responsibilities are:

- select the image mode and reject incompatible combinations;
- configure `BootBuildImage` lazily;
- select/pin builder and optional run image;
- configure image name, platform, and Buildpacks cache behavior;
- set build-time AOT/native environment variables;
- expose stable task names and descriptions;
- avoid instantiating Docker or Podman during Gradle configuration;
- leave image scanning, publishing, and deployment providers behind their
  existing boundaries.

The policy must not make native mode implicit, enable AOT cache for ordinary
JVM builds, or leak native-specific dependencies into library modules.

The generic `containerBuild` task must either delegate to the same image policy
or be explicitly documented as a different Dockerfile-oriented capability. It
must not claim to build the application image while invoking a nonexistent or
unconfigured Dockerfile path.

## 9. Healthcheck and deployment contract

Runtime healthchecks must not depend on Paketo's internal layer path, such as
`/layers/paketo-buildpacks_bellsoft-liberica/...`. The JVM and native overlays
may continue to differ, but each must use a stable application or image
launcher contract that survives builder layer layout changes.

Deployment manifests continue to select runtime images through
`EMME_SERVICE_IMAGE` or immutable Kustomize image substitutions. JVM,
JVM+AOT-cache, and native image tags must be distinguishable. Production must
not consume `latest`.

## 10. Verification strategy

### Build-logic and configuration tests

- Gradle TestKit verifies the convention registers/configures the expected
  `BootBuildImage`, AOT, and native tasks.
- Contract tests verify default JVM behavior, explicit AOT-cache opt-in,
  explicit native opt-in, incompatible-mode rejection, builder configuration,
  image naming, and no fallback.
- Source contract tests verify that no module convention applies native image
  implicitly.

### Image-backed tests

When Docker is available:

- Build the JVM image and inspect labels, process types, image size, and
  generated runtime environment.
- Build the JVM+AOT-cache image and verify the cache is present and used.
- Build the native image and verify the image starts without a JVM fallback.
- Run each image under the Compose memory limits and execute the health and
  critical smoke suite.

### Runtime comparison

Record, for equivalent application configuration and workload:

- image size;
- build duration and cache reuse;
- cold-start time to readiness, using repeated measurements;
- peak RSS and container limit;
- health/readiness stability;
- critical request and authentication smoke results.

The plan will collect evidence before selecting a deployment default. It will
not invent a performance threshold before the baseline is measured; adoption
requires the selected variant to preserve critical behavior and demonstrate a
material operational benefit.

## 11. Planned implementation sequence

1. Reconcile the current container convention and verify the actual Gradle
   task graph in JVM and native modes.
2. Add the typed Buildpacks image policy and explicit builder/cache
   configuration.
3. Standardize memory-limit and Paketo runtime-variable contracts across
   Compose, Kubernetes, and CI.
4. Add the opt-in JVM AOT-cache lane and safe training-run configuration.
5. Harden the existing Native Image lane and its OCI/direct-build checks.
6. Replace builder-internal healthcheck assumptions.
7. Add CI/manual image lanes, runtime evidence collection, and documentation.
8. Run the complete verification suite and update the older deployment-plan
   references without rewriting its already-completed historical tasks.

## 12. Open implementation constraints

- The exact Paketo builder and run-image digest must be selected from the
  Java-25-compatible builder set available at implementation time.
- The headroom value must be chosen from measured JVM RSS/non-heap behavior per
  environment; it must remain configurable.
- AOT-cache training requires a deterministic, side-effect-free startup
  profile. Any provider that cannot be disabled for training is a release
  blocker for the AOT-cache variant.
- Native Image support for the current dependency graph must be demonstrated
  by the native smoke suite before native promotion is enabled.

## 13. References

- [Spring Boot AOT cache](https://docs.spring.io/spring-boot/reference/packaging/aot-cache.html)
- [Spring Boot AOT cache with Buildpacks](https://docs.spring.io/spring-boot/how-to/aot-cache.html)
- [Spring Boot Gradle AOT integration](https://docs.spring.io/spring-boot/gradle-plugin/aot.html)
- [Spring Boot Gradle OCI image packaging](https://docs.spring.io/spring-boot/gradle-plugin/packaging-oci-image.html)
- [Paketo Java Buildpack memory calculator](https://paketo.io/docs/reference/java-reference/)
- [Paketo Java runtime configuration](https://paketo.io/docs/howto/java/)
- [Paketo Java Native Image Buildpack](https://paketo.io/docs/reference/java-native-image-reference/)
- [GraalVM Native Build Tools Gradle plugin](https://graalvm.github.io/native-build-tools/latest/gradle-plugin.html)
