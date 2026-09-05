# Selective Lombok Adoption Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Introduce Lombok only where it removes substantial mechanical constructor boilerplate, while preserving explicit domain invariants, JPA behavior, and repository conventions.

**Architecture:** Add an opt-in `emme.lombok` Gradle convention plugin that configures Lombok as compile-only code with an explicit annotation processor. Apply it to three modules for a narrowly scoped `@RequiredArgsConstructor` pilot: the assistant conversation controller, appointment listing service, and catalog matching service. Do not use Lombok in domain models, JPA entities, records, API contracts, or classes with custom constructors/equality/lifecycle behavior.

**Tech Stack:** Java 25, Gradle Kotlin DSL, Spring Boot 4.1.0, JUnit 5, AssertJ, Spotless, Checkstyle, Project Lombok 1.18.48.

## Global Constraints

- Lombok is compile-time only: use `compileOnly` and `annotationProcessor`; it must not be present on runtime classpaths.
- Use only `@RequiredArgsConstructor` in the initial adoption; do not introduce `@Data`, type-level `@Setter`, generated equality, generated `toString`, or `@Builder` in production code.
- Do not modify `Conversation.java` or any other domain aggregate.
- Do not modify JPA entities, `PersistedEntity`, API records, request records, response records, or configuration records.
- Apply Lombok only to classes with one constructor whose parameters correspond exactly to `final` fields and whose constructor has no validation, defaults, compatibility overloads, or side effects.
- Preserve public constructor visibility and constructor parameter names (`-parameters` remains enabled).
- Preserve existing Spring bean wiring and behavior; this plan changes source boilerplate only.
- The dependency version and processor wiring must be centralized through the version catalog and opt-in convention plugin.

## Scope and Candidate Inventory

The initial pilot is deliberately limited to these classes:

| Module | Class | Annotation | Why eligible |
|---|---|---|---|
| `assistant` | `com.emme.assistant.adapter.in.web.controller.ConversationController` | `@RequiredArgsConstructor` | Eight `final` use-case dependencies and exactly one constructor with direct assignments. |
| `appointments` | `com.emme.appointments.application.service.ListAppointmentsService` | `@RequiredArgsConstructor` | Four `final` repository dependencies and exactly one constructor with direct assignments. |
| `catalog` | `com.emme.catalog.application.service.MatchCatalogItemsService` | `@RequiredArgsConstructor` | Five `final` capability/repository dependencies and exactly one constructor with direct assignments. |

Explicit exclusions:

- `modules/assistant/src/main/java/com/emme/assistant/domain/model/Conversation.java`: aggregate invariants, controlled rehydration, and state transition remain explicit.
- `modules/*/src/main/java/**/domain/**`: domain behavior and construction rules remain explicit.
- `modules/*/src/main/java/**/adapter/out/persistence/entity/**`: JPA constructors, accessors, identity, lifecycle, and mutation boundaries remain explicit.
- `modules/shared/src/main/java/com/emme/shared/persistence/PersistedEntity.java`: custom equality and persistence callbacks remain explicit.
- `ProcessWhatsAppMessageService`: overloaded constructors, optional dependencies, default collaborators, and `@Autowired` selection make it ineligible for `@RequiredArgsConstructor`.
- Records such as `GoogleOAuthProperties`, `CreateServiceRequest`, `ServiceDetails`, and response/result types: records already provide the relevant boilerplate reduction.

Lombok's official Gradle setup requires compile-only and annotation-processor configuration; JDK 23 and newer require explicit processor handling. The repository targets JDK 25, so the convention plugin must configure both scopes: [Project Lombok Gradle setup](https://projectlombok.org/setup/gradle), [Project Lombok JDK 25 support](https://projectlombok.org/changelog).

## Files to Create or Modify

- Modify: `gradle/libs.versions.toml` — add the Lombok version and library alias.
- Modify: `build-logic/src/main/kotlin/com/emme/buildlogic/core/dependency/Dependencies.kt` — expose the catalog alias to convention plugins.
- Create: `build-logic/src/main/kotlin/emme.lombok.gradle.kts` — opt-in compile-only and annotation-processor wiring.
- Modify: `modules/assistant/build.gradle.kts` — opt into the convention plugin.
- Modify: `modules/appointments/build.gradle.kts` — opt into the convention plugin.
- Modify: `modules/catalog/build.gradle.kts` — opt into the convention plugin.
- Create: `modules/shared/src/test/java/com/emme/shared/architecture/LombokUsagePolicyTest.java` — enforce the allowlist and exclusions across production sources.
- Modify: `modules/assistant/src/main/java/com/emme/assistant/adapter/in/web/controller/ConversationController.java` — replace its explicit dependency constructor with `@RequiredArgsConstructor`.
- Modify: `modules/appointments/src/main/java/com/emme/appointments/application/service/ListAppointmentsService.java` — replace its explicit dependency constructor with `@RequiredArgsConstructor`.
- Modify: `modules/catalog/src/main/java/com/emme/catalog/application/service/MatchCatalogItemsService.java` — replace its explicit dependency constructor with `@RequiredArgsConstructor`.
- Existing verification tests: `modules/assistant/src/test/java/com/emme/assistant/adapter/in/web/controller/ConversationControllerTest.java`, `modules/assistant/src/test/java/com/emme/conversations/web/ConversationWebTest.java`, plus the appointments and catalog module test suites.

## Task 1: Add Opt-In Lombok Build Support

**Files:**

- Modify: `gradle/libs.versions.toml`
- Modify: `build-logic/src/main/kotlin/com/emme/buildlogic/core/dependency/Dependencies.kt`
- Create: `build-logic/src/main/kotlin/emme.lombok.gradle.kts`
- Modify: `modules/assistant/build.gradle.kts`
- Modify: `modules/appointments/build.gradle.kts`
- Modify: `modules/catalog/build.gradle.kts`

**Interfaces:**

- Produces the opt-in Gradle plugin id `emme.lombok`.
- Produces `compileOnly(org.projectlombok:lombok:1.18.48)` and `annotationProcessor(org.projectlombok:lombok:1.18.48)` for each opted-in module.
- Produces matching test compile-only and test annotation-processor configurations for future Lombok test fixtures without adding Lombok at runtime.

- [ ] **Step 1: Add the catalog entry.** Add the following entries to `gradle/libs.versions.toml`:

  ```toml
  [versions]
  lombok = "1.18.48"

  [libraries]
  lombok = { module = "org.projectlombok:lombok", version.ref = "lombok" }
  ```

- [ ] **Step 2: Expose the catalog dependency.** Add this property to `Dependencies` under the utilities section:

  ```kotlin
  val lombok get() = lib("lombok")
  ```

- [ ] **Step 3: Create the convention plugin.** Create `emme.lombok.gradle.kts` with this exact wiring:

  ```kotlin
  import com.emme.buildlogic.core.dependency.Dependencies
  import org.gradle.api.artifacts.VersionCatalogsExtension

  val catalog = extensions.getByType<VersionCatalogsExtension>().named("libs")
  val dependencies = Dependencies(catalog)

  dependencies {
    add("compileOnly", dependencies.lombok)
    add("annotationProcessor", dependencies.lombok)
    add("testCompileOnly", dependencies.lombok)
    add("testAnnotationProcessor", dependencies.lombok)
  }
  ```

- [ ] **Step 4: Opt the three modules into the plugin.** Add `id("emme.lombok")` to the existing `plugins` block in each of these files, without adding Lombok to `emme.java-library.gradle.kts` or `emme.spring-module.gradle.kts`:

  ```text
  modules/assistant/build.gradle.kts
  modules/appointments/build.gradle.kts
  modules/catalog/build.gradle.kts
  ```

- [ ] **Step 5: Verify dependency scope and compilation.**

  Run:

  ```bash
  ./gradlew :modules:assistant:dependencies --configuration annotationProcessor
  ./gradlew :modules:assistant:dependencies --configuration runtimeClasspath
  ./gradlew :modules:appointments:dependencies --configuration annotationProcessor
  ./gradlew :modules:catalog:dependencies --configuration annotationProcessor
  ./gradlew :modules:assistant:compileJava :modules:appointments:compileJava :modules:catalog:compileJava
  ```

  Expected: Lombok appears in each opted-in `annotationProcessor` configuration, does not appear as a runtime dependency, and all three compile successfully on Java 25.

- [ ] **Step 6: Commit the build support.**

  ```bash
  git add gradle/libs.versions.toml build-logic/src/main/kotlin/com/emme/buildlogic/core/dependency/Dependencies.kt build-logic/src/main/kotlin/emme.lombok.gradle.kts modules/assistant/build.gradle.kts modules/appointments/build.gradle.kts modules/catalog/build.gradle.kts
  git commit -m "build: add opt-in Lombok convention"
  ```

## Task 2: Add and Satisfy the Repository Lombok Policy Test

**Files:**

- Create: `modules/shared/src/test/java/com/emme/shared/architecture/LombokUsagePolicyTest.java`

**Interfaces:**

- Produces a repository-level test that permits Lombok only in the three named pilot classes.
- Produces a permanent guard that rejects Lombok imports from domain models and JPA entity packages.

- [ ] **Step 1: Write the policy test.** Create a test with these exact rules:

  1. Walk `modules` and `libraries` production Java sources.
  2. Collect files containing `import lombok.` or `lombok.` annotations.
  3. Assert every collected relative path belongs to this allowlist:
     `modules/assistant/src/main/java/com/emme/assistant/adapter/in/web/controller/ConversationController.java`,
     `modules/appointments/src/main/java/com/emme/appointments/application/service/ListAppointmentsService.java`, or
     `modules/catalog/src/main/java/com/emme/catalog/application/service/MatchCatalogItemsService.java`.
  4. Assert no source under a `domain` directory or a `persistence/entity` directory contains `import lombok.`.
  5. Assert no production source contains `@Data`, `@Setter`, `@EqualsAndHashCode`, `@ToString`, or `@Builder`.

  Use `Files.walk`, `Path.relativize`, and AssertJ, matching the existing repository convention tests. Resolve the repository root by walking upward from `Path.of("").toAbsolutePath()` so the test works from Gradle module execution. The test must pass when the allowlist is empty, so it protects against unauthorized future usage without requiring all pilot conversions to land in one commit.

  Use this implementation shape, adjusting only package formatting required by Spotless:

  ```java
  package com.emme.shared.architecture;

  import static org.assertj.core.api.Assertions.assertThat;

  import java.io.IOException;
  import java.nio.file.Files;
  import java.nio.file.Path;
  import java.util.List;
  import java.util.Set;
  import java.util.stream.Stream;
  import org.junit.jupiter.api.Test;

  class LombokUsagePolicyTest {

    private static final Set<String> APPROVED_FILES =
        Set.of(
            "modules/assistant/src/main/java/com/emme/assistant/adapter/in/web/controller/ConversationController.java",
            "modules/appointments/src/main/java/com/emme/appointments/application/service/ListAppointmentsService.java",
            "modules/catalog/src/main/java/com/emme/catalog/application/service/MatchCatalogItemsService.java");

    @Test
    void onlyUsesLombokInApprovedProductionFiles() throws IOException {
      Path root = sourcePath(".");

      Set<String> lombokFiles =
          productionSources(root).stream()
              .filter(this::containsLombok)
              .map(path -> root.relativize(path).toString().replace('\\', '/'))
              .collect(java.util.stream.Collectors.toSet());

      assertThat(lombokFiles).containsOnlyElementsOf(APPROVED_FILES);
    }

    @Test
    void forbidsBroadLombokAnnotationsAndDomainOrEntityUsage() throws IOException {
      Path root = sourcePath(".");

      for (Path source : productionSources(root)) {
        String normalized = source.toString().replace('\\', '/');
        String contents = Files.readString(source);

        if (normalized.contains("/domain/") || normalized.contains("/persistence/entity/")) {
          assertThat(contents).as("forbidden Lombok source: %s", source).doesNotContain("import lombok.");
        }
        assertThat(contents)
            .as("forbidden Lombok annotation: %s", source)
            .doesNotContain("@Data")
            .doesNotContain("@Setter")
            .doesNotContain("@EqualsAndHashCode")
            .doesNotContain("@ToString")
            .doesNotContain("@Builder");
      }
    }

    private static List<Path> productionSources(Path root) throws IOException {
      try (Stream<Path> modules = Files.walk(root.resolve("modules"));
          Stream<Path> libraries = Files.walk(root.resolve("libraries"))) {
        return Stream.concat(modules, libraries)
            .filter(
                path -> {
                  String normalized = path.toString().replace('\\', '/');
                  return normalized.endsWith("src/main/java")
                      || normalized.contains("/src/main/java/");
                })
            .filter(path -> path.toString().endsWith(".java"))
            .toList();
      }
    }

    private boolean containsLombok(Path source) {
      try {
        String contents = Files.readString(source);
        return contents.contains("import lombok.") || contents.contains("lombok.");
      } catch (IOException exception) {
        throw new IllegalStateException("Cannot read source: " + source, exception);
      }
    }

    private static Path sourcePath(String relativePath) {
      Path current = Path.of("").toAbsolutePath();
      while (current != null) {
        Path candidate = current.resolve(relativePath);
        if (Files.exists(candidate.resolve("modules")) && Files.exists(candidate.resolve("libraries"))) {
          return candidate;
        }
        current = current.getParent();
      }
      throw new IllegalStateException("Cannot locate repository root");
    }
  }
  ```

- [ ] **Step 2: Run the policy test against the baseline.**

  ```bash
  ./gradlew :modules:shared:test --tests com.emme.shared.architecture.LombokUsagePolicyTest
  ```

  Expected: `PASS` on the current repository because there is no Lombok usage yet.

- [ ] **Step 3: Confirm the test is narrow.** Before converting production classes, inspect the implementation and confirm it does not require Lombok in any domain, entity, record, or unrelated class.

- [ ] **Step 4: Commit the policy guard.**

  ```bash
  git add modules/shared/src/test/java/com/emme/shared/architecture/LombokUsagePolicyTest.java
  git commit -m "test: constrain Lombok usage to approved classes"
  ```

## Task 3: Convert `ConversationController`

**Files:**

- Modify: `modules/assistant/src/main/java/com/emme/assistant/adapter/in/web/controller/ConversationController.java`
- Verify: `modules/assistant/src/test/java/com/emme/assistant/adapter/in/web/controller/ConversationControllerTest.java`
- Verify: `modules/assistant/src/test/java/com/emme/conversations/web/ConversationWebTest.java`
- Verify: `modules/shared/src/test/java/com/emme/shared/architecture/LombokUsagePolicyTest.java`

**Interfaces:**

- Consumes the existing eight `final` fields: `StartConversationUseCase`, `ListConversationsUseCase`, `GetConversationUseCase`, `CloseConversationUseCase`, `GetConversationHistoryUseCase`, `ProposePendingActionUseCase`, `ConfirmPendingActionUseCase`, and `RejectPendingActionUseCase`.
- Produces one public constructor with the same parameter order and visibility as the current explicit constructor.

- [ ] **Step 1: Run characterization tests before editing.**

  ```bash
  ./gradlew :modules:assistant:test --tests com.emme.assistant.adapter.in.web.controller.ConversationControllerTest --tests com.emme.conversations.web.ConversationWebTest
  ```

  Expected: both existing tests pass on the baseline implementation.

- [ ] **Step 2: Replace only the constructor boilerplate.** Add `import lombok.RequiredArgsConstructor;`, annotate the class with `@RequiredArgsConstructor`, and remove the explicit constructor. Keep all fields, endpoint methods, annotations, and field order unchanged:

  ```java
  @RestController
  @RequestMapping(path = "/api/conversations", version = "1.0")
  @Tag(name = "Conversations")
  @RequiredArgsConstructor
  public class ConversationController {
  ```

- [ ] **Step 3: Run the focused tests.**

  ```bash
  ./gradlew :modules:assistant:test --tests com.emme.assistant.adapter.in.web.controller.ConversationControllerTest --tests com.emme.conversations.web.ConversationWebTest
  ```

  Expected: both tests pass, proving constructor injection and HTTP behavior are unchanged.

- [ ] **Step 4: Run formatting and the policy test.**

  ```bash
  ./gradlew :modules:assistant:spotlessCheck :modules:shared:test --tests com.emme.shared.architecture.LombokUsagePolicyTest
  ```

  Expected: formatting passes and the policy test remains green while only the controller is using an approved Lombok annotation.

- [ ] **Step 5: Commit the first conversion.**

  ```bash
  git add modules/assistant/src/main/java/com/emme/assistant/adapter/in/web/controller/ConversationController.java modules/shared/src/test/java/com/emme/shared/architecture/LombokUsagePolicyTest.java
  git commit -m "refactor(assistant): reduce controller constructor boilerplate"
  ```

## Task 4: Convert `ListAppointmentsService` and `MatchCatalogItemsService`

**Files:**

- Modify: `modules/appointments/src/main/java/com/emme/appointments/application/service/ListAppointmentsService.java`
- Modify: `modules/catalog/src/main/java/com/emme/catalog/application/service/MatchCatalogItemsService.java`
- Verify: appointments module unit and integration tests.
- Verify: catalog module unit and integration tests.
- Verify: `modules/shared/src/test/java/com/emme/shared/architecture/LombokUsagePolicyTest.java`.

**Interfaces:**

- `ListAppointmentsService` retains its four constructor parameters in declaration order.
- `MatchCatalogItemsService` retains its five constructor parameters in declaration order.
- Neither class receives setters, generated equality, generated `toString`, or a second constructor.

- [ ] **Step 1: Run characterization tests before editing.**

  ```bash
  ./gradlew :modules:appointments:test :modules:catalog:test
  ```

  Expected: the existing appointments and catalog test suites pass on the explicit-constructor baseline.

- [ ] **Step 2: Convert `ListAppointmentsService`.** Add `import lombok.RequiredArgsConstructor;`, add `@RequiredArgsConstructor` immediately above `@Service`, and remove only its explicit constructor. Leave repository calls, transaction annotations, and mapping logic unchanged.

- [ ] **Step 3: Verify the first service conversion.**

  ```bash
  ./gradlew :modules:appointments:test :modules:appointments:spotlessCheck :modules:appointments:compileJava
  ```

  Expected: all appointments tests, formatting, and compilation pass.

- [ ] **Step 4: Convert `MatchCatalogItemsService`.** Add `import lombok.RequiredArgsConstructor;`, add `@RequiredArgsConstructor` immediately above `@Service`, and remove only its explicit constructor. Leave search ordering, score aggregation, and transaction behavior unchanged.

- [ ] **Step 5: Verify the second service conversion.**

  ```bash
  ./gradlew :modules:catalog:test :modules:catalog:spotlessCheck :modules:catalog:compileJava
  ```

  Expected: all catalog tests, formatting, and compilation pass.

- [ ] **Step 6: Run the policy test.**

  ```bash
  ./gradlew :modules:shared:test --tests com.emme.shared.architecture.LombokUsagePolicyTest
  ```

  Expected: PASS; the only Lombok production sources are the three approved classes and no forbidden annotation is present.

- [ ] **Step 7: Commit the service conversions.**

  ```bash
  git add modules/appointments/src/main/java/com/emme/appointments/application/service/ListAppointmentsService.java modules/catalog/src/main/java/com/emme/catalog/application/service/MatchCatalogItemsService.java
  git commit -m "refactor: reduce selected service constructor boilerplate"
  ```

## Task 5: Full Verification and Adoption Gate

**Files:**

- Verify all files listed in Tasks 1–4.
- Modify: `tasks/todo.md` only if the current-session task tracker is being used by the executing agent.

- [ ] **Step 1: Run focused module verification.**

  ```bash
  ./gradlew :modules:shared:test :modules:assistant:test :modules:appointments:test :modules:catalog:test
  ```

  Expected: zero failures and zero skipped tests in the selected modules.

- [ ] **Step 2: Run compile, formatting, and Checkstyle gates.**

  ```bash
  ./gradlew :modules:shared:compileJava :modules:assistant:compileJava :modules:appointments:compileJava :modules:catalog:compileJava
  ./gradlew :modules:shared:spotlessCheck :modules:assistant:spotlessCheck :modules:appointments:spotlessCheck :modules:catalog:spotlessCheck
  ./gradlew :modules:assistant:checkstyleMain :modules:appointments:checkstyleMain :modules:catalog:checkstyleMain
  ```

  Expected: all tasks pass on Java 25.

- [ ] **Step 3: Verify runtime dependency absence.**

  ```bash
  ./gradlew :modules:assistant:dependencies --configuration runtimeClasspath | rg "org.projectlombok" && exit 1 || true
  ./gradlew :modules:appointments:dependencies --configuration runtimeClasspath | rg "org.projectlombok" && exit 1 || true
  ./gradlew :modules:catalog:dependencies --configuration runtimeClasspath | rg "org.projectlombok" && exit 1 || true
  ```

  Expected: each command exits successfully because no runtime classpath contains Lombok. If the shell output is ambiguous, inspect the full dependency report and correct the convention plugin before proceeding.

- [ ] **Step 4: Inspect the generated constructors.** Use `javap` against the compiled classes or the existing constructor call sites to verify that each generated constructor is public and preserves the existing parameter order. Do not add reflection tests solely to test Lombok generation.

- [ ] **Step 5: Apply the adoption gate.** Keep the pilot only if all of these are true:

  - The three selected classes compile and pass their module tests.
  - The policy test passes with exactly three approved production files.
  - No domain model, JPA entity, record, or persistence base class changes.
  - Runtime dependency reports contain no Lombok artifact.
  - The diff removes meaningful constructor boilerplate without hiding validation or wiring behavior.

  If any gate fails, remove the annotations and opt-in plugin declarations, revert the version-catalog and convention changes, and keep the existing explicit constructors. Do not expand the pilot to additional classes in the same session.

- [ ] **Step 6: Commit and push the completed implementation.**

  ```bash
  git status --short
  git add gradle/libs.versions.toml build-logic/src/main/kotlin/com/emme/buildlogic/core/dependency/Dependencies.kt build-logic/src/main/kotlin/emme.lombok.gradle.kts modules/assistant/build.gradle.kts modules/appointments/build.gradle.kts modules/catalog/build.gradle.kts modules/shared/src/test/java/com/emme/shared/architecture/LombokUsagePolicyTest.java modules/assistant/src/main/java/com/emme/assistant/adapter/in/web/controller/ConversationController.java modules/appointments/src/main/java/com/emme/appointments/application/service/ListAppointmentsService.java modules/catalog/src/main/java/com/emme/catalog/application/service/MatchCatalogItemsService.java
  git commit -m "refactor: adopt Lombok selectively"
  git push origin feat/ai-platform-foundation
  git log --oneline origin/feat/ai-platform-foundation -1
  ```

  Expected: no uncommitted implementation files remain, the push succeeds, and the remote branch points to the new commit.

## Dependency and Ordering Notes

1. Task 1 must precede any production annotation because Java 25 requires explicit annotation-processor configuration.
2. Task 2 defines the usage boundary before all three target classes are converted.
3. Task 3 establishes the first real Spring MVC wiring checkpoint and commits the policy test with a passing allowlist.
4. Task 4 converts only classes whose constructors are structurally safe for `@RequiredArgsConstructor`; it must not include overloaded or compatibility constructors.
5. Task 5 is the final adoption decision. No repository-wide Lombok migration follows automatically from a passing pilot.

## Definition of Done

- [ ] Lombok is declared once in the version catalog at `1.18.48`.
- [ ] Opt-in build wiring uses compile-only and annotation-processor configurations and is centralized in `emme.lombok.gradle.kts`.
- [ ] Exactly three production classes use `@RequiredArgsConstructor`.
- [ ] `Conversation.java`, all domain models, JPA entities, persistence base classes, and records remain explicit.
- [ ] The policy test passes and prevents future forbidden usage.
- [ ] Focused tests, compilation, Spotless, and Checkstyle pass with zero failures.
- [ ] Lombok is absent from all opted-in runtime classpaths.
- [ ] Changes are committed atomically and pushed to `origin/feat/ai-platform-foundation`.
