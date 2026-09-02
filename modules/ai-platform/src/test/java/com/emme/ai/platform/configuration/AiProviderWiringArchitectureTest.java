package com.emme.ai.platform.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class AiProviderWiringArchitectureTest {

  @Test
  void keepsActiveProviderWiringOnSpringAiAdaptersInsteadOfRawOkHttpComponents()
      throws IOException {
    Path root = sourcePath("modules/ai-platform/src/main/java/com/emme/ai/platform");

    assertThat(read(root.resolve("configuration/AiProviderConfiguration.java")))
        .contains("SpringAiChatModel")
        .contains("SpringAiEmbeddingModel");
    assertThat(Files.exists(root.resolve("adapter/out/provider/ollama/OllamaModelProvider.java")))
        .isFalse();
    assertThat(Files.exists(root.resolve("adapter/out/provider/groq/GroqModelProvider.java")))
        .isFalse();
    assertThat(Files.exists(root.resolve("configuration/AiProviderHttpClient.java"))).isFalse();
  }

  private static String read(Path path) throws IOException {
    return Files.readString(path);
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
