package com.emme.assistant.ai.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class SpringAiAdapterConsolidationArchitectureTest {

  @Test
  void keepsOnlyTheCanonicalSpringAiTransportAdaptersInAssistantProductionCode()
      throws IOException {
    Path assistantRoot = sourcePath("modules/assistant/src/main/java/com/emme/assistant/ai");
    Path platformRoot = sourcePath("modules/ai-platform/src/main/java/com/emme/ai/platform");

    assertThat(
            Files.exists(
                assistantRoot.resolve(
                    "adapter/out/provider/springai/SpringAiChatClientAdapter.java")))
        .isFalse();
    assertThat(
            Files.exists(
                assistantRoot.resolve(
                    "adapter/out/provider/springai/SpringAiEmbeddingAdapter.java")))
        .isFalse();
    assertThat(
            Files.exists(
                assistantRoot.resolve(
                    "adapter/out/provider/springai/SpringAiEmbeddingModelAdapter.java")))
        .isTrue();

    assertThat(read(assistantRoot.resolve("configuration/SpringAiChatProviderRegistry.java")))
        .contains("SpringAiChatModel")
        .doesNotContain("SpringAiChatClientAdapter");
    assertThat(read(assistantRoot.resolve("configuration/SpringAiEmbeddingProviderRegistry.java")))
        .contains("SpringAiEmbeddingModel")
        .doesNotContain("SpringAiEmbeddingAdapter");
    assertThat(read(assistantRoot.resolve("configuration/SpringAiRedisSemanticConfiguration.java")))
        .contains("SpringAiEmbeddingModelAdapter");

    assertThat(
            Files.exists(
                platformRoot.resolve("adapter/out/provider/springai/SpringAiChatModel.java")))
        .isTrue();
    assertThat(
            Files.exists(
                platformRoot.resolve("adapter/out/provider/springai/SpringAiEmbeddingModel.java")))
        .isTrue();
  }

  @Test
  void hasNoRedundantSpringAiTransportReferencesInAssistantProductionCode() throws IOException {
    Path root = sourcePath("modules/assistant/src/main/java/com/emme/assistant/ai");

    try (Stream<Path> sources = Files.walk(root)) {
      for (Path source : sources.filter(path -> path.toString().endsWith(".java")).toList()) {
        assertThat(read(source))
            .as(
                "assistant production source must use the canonical Spring AI transport: %s",
                source)
            .doesNotContain("SpringAiChatClientAdapter")
            .doesNotContain("SpringAiEmbeddingAdapter");
      }
    }
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
