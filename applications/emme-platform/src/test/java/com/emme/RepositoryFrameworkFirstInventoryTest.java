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
