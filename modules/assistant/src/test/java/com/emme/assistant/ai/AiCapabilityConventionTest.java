package com.emme.assistant.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class AiCapabilityConventionTest {
  @Test
  void keepsProviderImplementationsBehindApplicationPorts() {
    Path root = sourcePath("modules/assistant/src/main/java/com/emme/assistant/ai");
    assertThat(Files.exists(root.resolve("application/port/out/ModelProvider.java"))).isTrue();
    assertThat(Files.exists(root.resolve("adapter/out/provider/MockModelProvider.java"))).isTrue();
    assertThat(Files.exists(root.resolve("adapter/out/provider/GroqModelProvider.java"))).isTrue();
    assertThat(Files.exists(root.resolve("application/service/DetectIntentService.java"))).isTrue();
    assertThat(Files.exists(root.resolve("api/usecase/DetectIntentUseCase.java"))).isTrue();
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
