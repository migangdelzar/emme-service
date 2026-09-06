package com.emme.assistant.ai.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ChatCompositionArchitectureTest {

  @Test
  void keepsChatServiceFreeOfTheLegacyModelProviderFallback() throws IOException {
    String chatService =
        Files.readString(
            sourcePath(
                "modules/assistant/src/main/java/com/emme/assistant/ai/application/service/ChatService.java"));

    assertThat(chatService)
        .contains("private final AiChatCompletion chatCompletion")
        .doesNotContain("ChatCompletionPort")
        .doesNotContain("IdentifiedChatCompletionPort")
        .doesNotContain("legacy-provider")
        .doesNotContain("legacy-model")
        .doesNotContain("AiModelProvider")
        .doesNotContain("executeLegacyChat")
        .doesNotContain("ChatProviderFailurePolicy");
  }

  @Test
  void keepsRagQueryServiceOnTheCanonicalChatBoundary() throws IOException {
    String ragQueryService =
        Files.readString(
            sourcePath(
                "modules/assistant/src/main/java/com/emme/assistant/ai/application/service/RagQueryService.java"));

    assertThat(ragQueryService)
        .contains("private final AiChatCompletion chatCompletion")
        .doesNotContain("AiModelProvider")
        .doesNotContain("ModelExecutionScheduler")
        .doesNotContain("executeLegacy");
  }

  @Test
  void selectsExactlyOneAssistantChatCompositionRootPerRuntimeProfile() throws IOException {
    String springChat =
        Files.readString(
            sourcePath(
                "modules/assistant/src/main/java/com/emme/assistant/ai/configuration/SpringAiChatConfiguration.java"));
    String legacyChat =
        Files.readString(
            sourcePath(
                "modules/assistant/src/main/java/com/emme/assistant/ai/configuration/LegacyChatCompletionConfiguration.java"));

    assertThat(springChat)
        .contains("prefix = \"app.ai.spring-chat\"")
        .contains("havingValue = \"true\"")
        .contains("ChatModelSelector chatCompletionPort");
    assertThat(legacyChat)
        .contains("prefix = \"app.ai.spring-chat\"")
        .contains("havingValue = \"false\"")
        .contains("matchIfMissing = true")
        .contains("ChatModelSelector legacyChatCompletion")
        .contains("new TracingAiChatCompletion");
  }

  @Test
  void legacyChatCompositionDoesNotDependOnTheCompositeModelProvider() throws IOException {
    String legacyChat =
        Files.readString(
            sourcePath(
                "modules/assistant/src/main/java/com/emme/assistant/ai/configuration/LegacyChatCompletionConfiguration.java"));

    assertThat(legacyChat).doesNotContain("AiModelProvider");
  }

  @Test
  void keepsProviderSelectionAndTracingOnTheCanonicalChatBoundary() throws IOException {
    String selector =
        Files.readString(
            sourcePath(
                "modules/assistant/src/main/java/com/emme/assistant/ai/application/provider/ChatModelSelector.java"));
    String tracing =
        Files.readString(
            sourcePath(
                "modules/assistant/src/main/java/com/emme/assistant/ai/application/provider/TracingAiChatCompletion.java"));
    String registry =
        Files.readString(
            sourcePath(
                "modules/assistant/src/main/java/com/emme/assistant/ai/configuration/SpringAiChatProviderRegistry.java"));

    assertThat(selector)
        .contains("implements AiChatCompletion")
        .contains("record Provider(String key, AiChatCompletion model")
        .doesNotContain("IdentifiedChatCompletionPort")
        .doesNotContain("ChatCompletionPort");
    assertThat(tracing)
        .contains("implements AiChatCompletion")
        .doesNotContain("ChatCompletionPort");
    assertThat(registry)
        .contains("private static AiChatCompletion applicationPort")
        .doesNotContain("ChatCompletionPort");
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
