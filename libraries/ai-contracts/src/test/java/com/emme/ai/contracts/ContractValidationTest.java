package com.emme.ai.contracts;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class ContractValidationTest {

  @Test
  void contractsDoNotImportFrameworksTransportsOrProviderSdkTypes() throws IOException {
    Path root = sourcePath("libraries/ai-contracts/src/main/java/com/emme/ai/contracts");

    try (Stream<Path> sources = Files.walk(root)) {
      for (Path source : sources.filter(path -> path.toString().endsWith(".java")).toList()) {
        assertThat(Files.readString(source))
            .as("framework-independent contract source: %s", source)
            .doesNotContain("org.springframework")
            .doesNotContain("com.fasterxml")
            .doesNotContain("okhttp3")
            .doesNotContain("langgraph4j")
            .doesNotContain("org.bsc.langgraph4j");
      }
    }
  }

  @Test
  void providerIntentResultPreservesStructuredFallbackData() {
    var result =
        new com.emme.ai.contracts.model.AiModelProvider.IntentResult(
            "BOOK", 0.95, java.util.Map.of("action", "schedule_appointment"));

    assertThat(result.intent()).isEqualTo("BOOK");
    assertThat(result.confidence()).isEqualTo(0.95);
    assertThat(result.parameters()).containsEntry("action", "schedule_appointment");
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
