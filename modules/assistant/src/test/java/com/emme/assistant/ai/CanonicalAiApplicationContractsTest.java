package com.emme.assistant.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.ai.contracts.embedding.EmbeddingService;
import com.emme.assistant.ai.application.port.out.ChatCompletionPort;
import com.emme.assistant.ai.application.port.out.EmbeddingModelPort;
import com.emme.assistant.ai.application.tool.AiToolDefinition;
import com.emme.assistant.ai.application.tool.AiToolGateway;
import com.emme.assistant.ai.application.tool.AiToolRisk;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class CanonicalAiApplicationContractsTest {

  @Test
  void legacyChatAndEmbeddingPortsDeclareTheirTemporaryCompatibilityStatus() {
    assertThat(ChatCompletionPort.class).hasAnnotation(Deprecated.class);
    assertThat(EmbeddingModelPort.class).hasAnnotation(Deprecated.class);
    assertThat(EmbeddingService.class).isAssignableFrom(EmbeddingModelPort.class);
  }

  @Test
  void assistantOwnsFrameworkNeutralToolMetadataGatewayAndRisk() throws IOException {
    assertThat(AiToolDefinition.class.isRecord()).isTrue();
    assertThat(AiToolGateway.class.isInterface()).isTrue();
    assertThat(AiToolRisk.class.isEnum()).isTrue();

    for (String source :
        java.util.List.of("AiToolDefinition.java", "AiToolGateway.java", "AiToolRisk.java")) {
      assertThat(Files.readString(toolSource(source)))
          .doesNotContain("org.springframework")
          .doesNotContain("ToolCallback");
    }
  }

  private static Path toolSource(String fileName) {
    String relativePath =
        "modules/assistant/src/main/java/com/emme/assistant/ai/application/tool/" + fileName;
    Path current = Path.of("").toAbsolutePath();
    while (current != null) {
      Path candidate = current.resolve(relativePath);
      if (Files.exists(candidate)) return candidate;
      current = current.getParent();
    }
    throw new IllegalStateException("Cannot locate source path: " + relativePath);
  }
}
