package com.emme.assistant.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.assistant.ai.application.port.out.SemanticCachePort;
import com.emme.assistant.ai.application.semantic.SemanticCacheIdentity;
import com.emme.assistant.ai.application.tool.AiToolDefinition;
import com.emme.assistant.ai.application.tool.AiToolGateway;
import com.emme.assistant.ai.application.tool.AiToolRisk;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class CanonicalAiApplicationContractsTest {

  @Test
  void temporaryChatPortSourcesAreRemovedAfterCanonicalMigration() throws IOException {
    assertThat(sourcePath("ChatCompletionPort.java")).doesNotExist();
    assertThat(sourcePath("IdentifiedChatCompletionPort.java")).doesNotExist();
  }

  @Test
  void deprecatedEmbeddingPortSourceIsRemovedAfterCanonicalMigration() throws IOException {
    assertThat(Files.exists(embeddingPortSource())).isFalse();
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

  @Test
  void semanticCacheContractsDoNotRetainLegacyIdentityFallbacks() throws IOException {
    assertThat(Files.readString(sourceOf(SemanticCachePort.class)))
        .doesNotContain("SemanticCacheIdentity.legacy()")
        .doesNotContain("public Lookup(")
        .doesNotContain("public Put(");
    assertThat(Files.readString(sourceOf(SemanticCacheIdentity.class)))
        .doesNotContain("static SemanticCacheIdentity legacy");
  }

  @Test
  void semanticCacheInvalidationUsesTheStructuredTenantAwareContract() throws IOException {
    assertThat(Files.readString(sourceOf(SemanticCachePort.class)))
        .doesNotContain("invalidate(String cacheKind)");
    assertThat(Files.readString(sourceOf(SemanticCachePort.class)))
        .doesNotContain("invalidate(CACHE_KIND)");
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

  private static Path sourceOf(Class<?> type) {
    String relativePath =
        "modules/assistant/src/main/java/" + type.getName().replace('.', '/') + ".java";
    Path current = Path.of("").toAbsolutePath();
    while (current != null) {
      Path candidate = current.resolve(relativePath);
      if (Files.exists(candidate)) return candidate;
      current = current.getParent();
    }
    throw new IllegalStateException("Cannot locate source path: " + relativePath);
  }

  private static Path embeddingPortSource() {
    String relativePath =
        "modules/assistant/src/main/java/com/emme/assistant/ai/application/port/out/EmbeddingModelPort.java";
    Path current = Path.of("").toAbsolutePath();
    while (current != null) {
      Path candidate = current.resolve(relativePath);
      if (Files.exists(candidate)) return candidate;
      current = current.getParent();
    }
    return Path.of(relativePath);
  }

  private static Path sourcePath(String fileName) {
    String relativePath =
        "modules/assistant/src/main/java/com/emme/assistant/ai/application/port/out/" + fileName;
    Path current = Path.of("").toAbsolutePath();
    while (current != null) {
      Path candidate = current.resolve(relativePath);
      if (Files.exists(candidate)) return candidate;
      current = current.getParent();
    }
    return Path.of(relativePath);
  }
}
