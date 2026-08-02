package com.emme.assistant.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class AiCapabilityConventionTest {
  @Test
  void keepsProviderImplementationsBehindApplicationPorts() {
    Path root = sourcePath("modules/assistant/src/main/java/com/emme/assistant/ai");
    assertThat(read(root.resolve("package-info.java"))).doesNotContain("@NamedInterface");
    assertThat(Files.exists(root.resolve("application/port/out/ModelProvider.java"))).isTrue();
    assertThat(Files.exists(root.resolve("adapter/out/provider/mock/MockModelProvider.java")))
        .isTrue();
    assertThat(Files.exists(root.resolve("adapter/out/provider/groq/GroqModelProvider.java")))
        .isTrue();
    assertThat(Files.exists(root.resolve("adapter/out/provider/ollama/OllamaModelProvider.java")))
        .isTrue();
    assertThat(Files.exists(root.resolve("adapter/in/web/request/ChatRequest.java"))).isTrue();
    assertThat(Files.exists(root.resolve("adapter/in/web/request/IntentRequest.java"))).isTrue();
    assertThat(Files.exists(root.resolve("adapter/in/web/request/RagRequest.java"))).isTrue();
    assertThat(Files.exists(root.resolve("application/service/DetectIntentService.java"))).isTrue();
    assertThat(Files.exists(root.resolve("api/usecase/DetectIntentUseCase.java"))).isTrue();
    assertThat(Files.exists(root.resolve("api/usecase/CaptionImageUseCase.java"))).isTrue();
    assertThat(Files.exists(root.resolve("api/usecase/EmbedTextUseCase.java"))).isTrue();
    assertThat(Files.exists(root.resolve("application/service/CaptionImageService.java"))).isTrue();
    assertThat(Files.exists(root.resolve("application/service/EmbedTextService.java"))).isTrue();
  }

  private static String read(Path path) {
    try {
      return Files.readString(path);
    } catch (Exception exception) {
      throw new IllegalStateException("Cannot read " + path, exception);
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
