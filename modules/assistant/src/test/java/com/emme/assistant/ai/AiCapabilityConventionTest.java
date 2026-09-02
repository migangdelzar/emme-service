package com.emme.assistant.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class AiCapabilityConventionTest {
  @Test
  void keepsProviderImplementationsBehindApplicationPorts() throws Exception {
    Path assistantRoot = sourcePath("modules/assistant/src/main/java/com/emme/assistant/ai");
    Path repositoryRoot = sourcePath(".git").getParent();
    Path platformRoot =
        repositoryRoot.resolve("modules/ai-platform/src/main/java/com/emme/ai/platform");
    Path contractsRoot =
        repositoryRoot.resolve("libraries/ai-contracts/src/main/java/com/emme/ai/contracts");
    Path platformBuild = repositoryRoot.resolve("modules/ai-platform/build.gradle.kts");

    assertThat(read(assistantRoot.resolve("package-info.java"))).doesNotContain("@NamedInterface");
    assertThat(read(platformBuild)).doesNotContain(":modules:assistant");
    try (Stream<Path> platformSources = Files.walk(platformRoot)) {
      for (Path source :
          platformSources
              .filter(path -> path.getFileName().toString().endsWith(".java"))
              .toList()) {
        assertThat(read(source))
            .as("provider platform source must not depend on assistant internals: %s", source)
            .doesNotContain("com.emme.assistant");
      }
    }
    assertThat(Files.exists(assistantRoot.resolve("application/port/out/ModelProvider.java")))
        .isFalse();
    assertThat(
            Files.exists(assistantRoot.resolve("adapter/out/provider/mock/MockModelProvider.java")))
        .isFalse();
    assertThat(
            Files.exists(assistantRoot.resolve("adapter/out/provider/groq/GroqModelProvider.java")))
        .isFalse();
    assertThat(
            Files.exists(
                assistantRoot.resolve("adapter/out/provider/ollama/OllamaModelProvider.java")))
        .isFalse();
    assertThat(Files.exists(contractsRoot.resolve("model/AiModelProvider.java"))).isTrue();
    assertThat(
            Files.exists(platformRoot.resolve("adapter/out/provider/mock/MockModelProvider.java")))
        .isTrue();
    assertThat(
            Files.exists(platformRoot.resolve("adapter/out/provider/groq/GroqModelProvider.java")))
        .isFalse();
    assertThat(
            Files.exists(
                platformRoot.resolve("adapter/out/provider/ollama/OllamaModelProvider.java")))
        .isFalse();
    assertThat(
            Files.exists(
                platformRoot.resolve("adapter/out/provider/springai/SpringAiModelProvider.java")))
        .isTrue();
    assertThat(Files.exists(assistantRoot.resolve("api/usecase/CaptionImageUseCase.java")))
        .isFalse();
    assertThat(Files.exists(assistantRoot.resolve("api/usecase/EmbedTextUseCase.java"))).isFalse();
    assertThat(Files.exists(assistantRoot.resolve("application/service/CaptionImageService.java")))
        .isFalse();
    assertThat(Files.exists(assistantRoot.resolve("application/service/EmbedTextService.java")))
        .isFalse();
    assertThat(Files.exists(contractsRoot.resolve("image/CaptionImageUseCase.java"))).isTrue();
    assertThat(Files.exists(contractsRoot.resolve("embedding/EmbedTextUseCase.java"))).isTrue();
    assertThat(
            Files.exists(platformRoot.resolve("adapter/out/capability/AiCaptionImageAdapter.java")))
        .isTrue();
    assertThat(Files.exists(platformRoot.resolve("adapter/out/capability/AiEmbeddingAdapter.java")))
        .isTrue();
    assertThat(Files.exists(assistantRoot.resolve("adapter/in/web/request/ChatRequest.java")))
        .isTrue();
    assertThat(Files.exists(assistantRoot.resolve("adapter/in/web/request/IntentRequest.java")))
        .isTrue();
    assertThat(Files.exists(assistantRoot.resolve("adapter/in/web/request/RagRequest.java")))
        .isTrue();
    assertThat(Files.exists(assistantRoot.resolve("application/service/DetectIntentService.java")))
        .isTrue();
    assertThat(Files.exists(assistantRoot.resolve("api/usecase/DetectIntentUseCase.java")))
        .isTrue();
  }

  @Test
  void everyMaterializedProductionPackageHasPackageMetadata() throws Exception {
    Path root = sourcePath("modules/assistant/src/main/java/com/emme/assistant");

    try (Stream<Path> paths = Files.walk(root)) {
      for (Path directory : paths.filter(Files::isDirectory).toList()) {
        boolean containsProductionSource;
        try (Stream<Path> files = Files.walk(directory)) {
          containsProductionSource =
              files.anyMatch(
                  path ->
                      path.getFileName().toString().endsWith(".java")
                          && !path.getFileName().toString().equals("package-info.java"));
        }
        if (containsProductionSource) {
          assertThat(Files.exists(directory.resolve("package-info.java")))
              .as("package metadata for %s", directory)
              .isTrue();
        }
      }
    }
  }

  @Test
  void doesNotRetainUnusedAiHelpersOrLegacyConfigurationPackage() {
    Path root = sourcePath("modules/assistant/src/main/java/com/emme/assistant/ai");

    assertThat(Files.exists(root.resolve("application/FallbackHandler.java"))).isFalse();
    assertThat(Files.exists(root.resolve("application/ToolRegistry.java"))).isFalse();
    assertThat(Files.exists(root.resolve("application/ToolExecutor.java"))).isFalse();
    assertThat(Files.exists(root.resolve("config"))).isFalse();
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
