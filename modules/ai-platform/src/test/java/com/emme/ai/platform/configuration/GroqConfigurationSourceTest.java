package com.emme.ai.platform.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class GroqConfigurationSourceTest {

  @Test
  void groqProviderUsesTypedAiConfigurationInsteadOfDirectEnvironmentAccess() throws IOException {
    Path root = sourcePath("modules/ai-platform/src/main/java/com/emme/ai/platform");

    assertThat(Files.readString(root.resolve("adapter/out/provider/groq/GroqModelProvider.java")))
        .doesNotContain("System.getenv(")
        .contains("props.chat().apiKey()")
        .doesNotContain("new OkHttpClient()")
        .doesNotContain("new ObjectMapper()");
  }

  @Test
  void ollamaProviderReceivesTransportDependencies() throws IOException {
    Path root = sourcePath("modules/ai-platform/src/main/java/com/emme/ai/platform");

    assertThat(
            Files.readString(root.resolve("adapter/out/provider/ollama/OllamaModelProvider.java")))
        .doesNotContain("new OkHttpClient()")
        .doesNotContain("new ObjectMapper()");
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
