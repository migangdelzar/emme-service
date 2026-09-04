package com.emme.assistant.ai.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class AiJdbcMigrationClassificationTest {

  private static final List<String> ALLOWED_REASONS =
      List.of(
          "JPA candidate",
          "dynamic identifier",
          "atomic claim",
          "atomic idempotency",
          "JSONB",
          "pgvector/FTS/RRF",
          "AGE",
          "LangGraph checkpoint",
          "RLS/session lifecycle",
          "measured lower complexity");

  @Test
  void recordsEveryAssistantJdbcAdapterWithAConcreteMigrationReason() throws IOException {
    String ledger =
        readRepositoryFile("docs/superpowers/migrations/framework-first-migration-ledger.md");
    String classification = classificationSection(ledger);
    assertThat(classification)
        .as("AI migration ledger must include the operational classification table")
        .contains(
            "| File | Category | Data shape | Concurrency / transaction | Tenant / security behavior | Proposed name | Equivalence test |");
    Path adapterRoot =
        repositoryFile("modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out");

    try (Stream<Path> sources = Files.walk(adapterRoot)) {
      sources
          .filter(path -> path.getFileName().toString().startsWith("Jdbc"))
          .filter(path -> path.getFileName().toString().endsWith(".java"))
          .forEach(
              source -> {
                String className = source.getFileName().toString().replace(".java", "");
                String row = classificationLine(classification, className);
                assertThat(row)
                    .as("AI JDBC adapter must have a detailed ledger row: %s", className)
                    .isNotEmpty();
                assertThat(ALLOWED_REASONS)
                    .as("AI JDBC adapter must have an approved reason: %s", className)
                    .anyMatch(row::contains);
              });
    }
  }

  private static String classificationLine(String ledger, String className) {
    return ledger
        .lines()
        .filter(line -> line.startsWith("|") && line.contains(className + ".java`"))
        .findFirst()
        .orElse("");
  }

  private static String classificationSection(String ledger) {
    String marker = "## 3.4 Detailed AI persistence classification";
    int start = ledger.indexOf(marker);
    if (start < 0) return "";
    int end = ledger.indexOf("## 4. Other framework-first inventories", start);
    return end < 0 ? ledger.substring(start) : ledger.substring(start, end);
  }

  private static String readRepositoryFile(String relativePath) throws IOException {
    return Files.readString(repositoryFile(relativePath));
  }

  private static Path repositoryFile(String relativePath) {
    Path current = Path.of("").toAbsolutePath();
    while (current != null) {
      Path candidate = current.resolve(relativePath);
      if (Files.exists(candidate)) return candidate;
      current = current.getParent();
    }
    throw new IllegalStateException("Cannot locate repository file: " + relativePath);
  }
}
