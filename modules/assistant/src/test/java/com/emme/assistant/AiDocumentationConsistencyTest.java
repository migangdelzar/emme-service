package com.emme.assistant;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class AiDocumentationConsistencyTest {

  private static final List<String> BOUNDARY_DOCUMENTS =
      List.of(
          "README.md",
          "technical-specification.md",
          "implementation-plan.md",
          "fcr/FCR-002-semantic-capabilities.md");

  @Test
  void usesTheCanonicalAiContractsAndPlatformBoundaries() throws Exception {
    Path aiDocumentation = sourcePath("docs/ai-platform");

    for (String relativePath : BOUNDARY_DOCUMENTS) {
      Path document = aiDocumentation.resolve(relativePath);
      assertThat(Files.exists(document)).as("AI boundary document exists: %s", document).isTrue();
      String content = Files.readString(document);
      assertThat(content)
          .as("AI boundary document uses canonical module names: %s", document)
          .contains("ai-contracts")
          .contains("ai-platform")
          .doesNotContain("ai-foundation");
    }
  }

  private static Path sourcePath(String relativePath) {
    Path current = Path.of("").toAbsolutePath();
    while (current != null) {
      Path candidate = current.resolve(relativePath);
      if (Files.exists(candidate)) return candidate;
      current = current.getParent();
    }
    throw new IllegalStateException("Cannot locate source path: " + relativePath);
  }
}
