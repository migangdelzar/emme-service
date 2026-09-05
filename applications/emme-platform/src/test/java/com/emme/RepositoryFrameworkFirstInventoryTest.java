package com.emme;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** Ensures every active project and JDBC-related production file enters the migration ledger. */
class RepositoryFrameworkFirstInventoryTest {

  private static final List<String> ACTIVE_PROJECTS =
      List.of(
          ":platform",
          ":applications:emme-platform",
          ":modules:shared",
          ":modules:tenancy",
          ":modules:identity",
          ":modules:clients",
          ":modules:staffing",
          ":modules:services",
          ":modules:appointments",
          ":modules:salon",
          ":modules:subscriptions",
          ":modules:documents",
          ":modules:catalog",
          ":modules:booking",
          ":modules:calendar",
          ":modules:notification",
          ":modules:payment",
          ":modules:assistant",
          ":modules:audit",
          ":modules:ai-platform",
          ":libraries:functional",
          ":libraries:kernel",
          ":libraries:testing",
          ":libraries:test-containers",
          ":libraries:ai-contracts",
          ":database",
          ":libraries:observability-support",
          ":tools:e2e-provisioner");

  @Test
  void everyActiveGradleProjectIsNamedInTheMigrationLedger() throws IOException {
    String ledger =
        Files.readString(
            sourcePath("docs/superpowers/migrations/framework-first-migration-ledger.md"));

    assertThat(ACTIVE_PROJECTS).allSatisfy(project -> assertThat(ledger).contains(project));
  }

  @Test
  void everyProductionJdbcRelatedFileIsNamedInTheMigrationLedger() throws IOException {
    String ledger =
        Files.readString(
            sourcePath("docs/superpowers/migrations/framework-first-migration-ledger.md"));
    Path repository = sourcePath("");

    try (Stream<Path> files = Files.walk(repository.resolve("modules").getParent())) {
      files
          .filter(path -> path.toString().contains("src/main/java"))
          .filter(path -> path.toString().endsWith(".java"))
          .filter(this::containsJdbcReference)
          .forEach(path -> assertThat(ledger).contains(repository.relativize(path).toString()));
    }
  }

  @Test
  void knownModuleBuildsDeclareSharedDependenciesOnlyOnce() throws IOException {
    assertThat(countExactDependency("modules/booking/build.gradle.kts", "libraries:kernel"))
        .isEqualTo(1);
    assertThat(countExactDependency("modules/catalog/build.gradle.kts", "libraries:kernel"))
        .isEqualTo(1);
    assertThat(
            countExactDependency("modules/assistant/build.gradle.kts", "libs.spring.security.test"))
        .isEqualTo(1);
  }

  @Test
  void modulesUsingTestingConventionDoNotRedeclareItsSharedFixtures() throws IOException {
    List<String> modules =
        List.of(
            "modules/appointments",
            "modules/audit",
            "modules/booking",
            "modules/clients",
            "modules/documents",
            "modules/salon",
            "modules/services",
            "modules/staffing",
            "modules/subscriptions");

    for (String module : modules) {
      String build = Files.readString(sourcePath(module + "/build.gradle.kts"));
      assertThat(build)
          .as("the emme.testing convention owns the shared test fixture for %s", module)
          .doesNotContain("testImplementation(testFixtures(project(\":libraries:testing\")))");
    }
  }

  @Test
  void springModulesDoNotReapplyTestingConvention() throws IOException {
    List<String> modules =
        List.of(
            "modules/appointments",
            "modules/booking",
            "modules/clients",
            "modules/documents",
            "modules/salon",
            "modules/services",
            "modules/subscriptions");

    for (String module : modules) {
      String build = Files.readString(sourcePath(module + "/build.gradle.kts"));
      assertThat(build)
          .as("the spring-module convention already applies emme.testing for %s", module)
          .contains("id(\"emme.spring-module\")")
          .doesNotContain("id(\"emme.testing\")");
    }
  }

  @Test
  void springApplicationDoesNotReapplyModulithConvention() throws IOException {
    String build = Files.readString(sourcePath("applications/emme-platform/build.gradle.kts"));

    assertThat(build)
        .contains("id(\"emme.spring-application\")")
        .doesNotContain("id(\"emme.modulith\")")
        .doesNotContain("testImplementation(testFixtures(project(\":libraries:testing\")))");
  }

  @Test
  void modulesWithoutFixtureSourcesDoNotApplyFixtureConvention() throws IOException {
    String build = Files.readString(sourcePath("modules/subscriptions/build.gradle.kts"));

    assertThat(build)
        .contains("id(\"emme.spring-module\")")
        .doesNotContain("id(\"emme.test-fixtures\")");
  }

  private boolean containsJdbcReference(Path path) {
    try {
      String source = Files.readString(path);
      return source.contains("JdbcTemplate")
          || source.contains("JdbcClient")
          || source.contains("JdbcOperations")
          || source.contains("NamedParameterJdbcTemplate")
          || source.contains("BootstrapConnectionExecutor");
    } catch (IOException exception) {
      throw new IllegalStateException("Cannot read " + path, exception);
    }
  }

  private static long countExactDependency(String relativePath, String dependency)
      throws IOException {
    return Files.readString(sourcePath(relativePath))
        .lines()
        .filter(line -> line.contains(dependency))
        .count();
  }

  private static Path sourcePath(String relativePath) {
    Path current = Path.of("").toAbsolutePath();
    while (current != null) {
      Path candidate = current.resolve(relativePath);
      if (Files.exists(candidate)) {
        return candidate;
      }
      current = current.getParent();
    }
    throw new IllegalStateException("Cannot locate source path: " + relativePath);
  }
}
