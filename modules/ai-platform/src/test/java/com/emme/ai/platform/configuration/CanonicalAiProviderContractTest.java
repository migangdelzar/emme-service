package com.emme.ai.platform.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.ai.contracts.embedding.EmbeddingService;
import com.emme.ai.contracts.image.CaptionImageUseCase;
import com.emme.ai.contracts.model.AiChatCompletion;
import com.emme.ai.contracts.model.AiModelProvider;
import com.emme.ai.platform.adapter.out.provider.springai.SpringAiChatModel;
import com.emme.ai.platform.adapter.out.provider.springai.SpringAiEmbeddingModel;
import com.emme.ai.platform.adapter.out.provider.springai.SpringAiVisionModel;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class CanonicalAiProviderContractTest {

  @Test
  void providerMechanicsDoNotImplementThePolicyFacingChatCapability() {
    assertThat(AiChatCompletion.class.isAssignableFrom(SpringAiChatModel.class)).isFalse();
  }

  @Test
  void existingEmbeddingAdapterRemainsCompatibleWithTheSelectedEmbeddingService() {
    assertThat(EmbeddingService.class.isAssignableFrom(SpringAiEmbeddingModel.class)).isTrue();
  }

  @Test
  void legacyCompositeProviderDoesNotBecomeAnEmbeddingServiceBean() {
    assertThat(EmbeddingService.class.isAssignableFrom(AiModelProvider.class)).isFalse();
  }

  @Test
  void visionProvidersExposeTheCanonicalCaptionCapability() {
    assertThat(CaptionImageUseCase.class.isAssignableFrom(SpringAiVisionModel.class)).isTrue();
  }

  @Test
  void compositeCaptionAdapterIsRemovedAfterCanonicalMigration() {
    assertThat(
            sourcePath(
                "modules/ai-platform/src/main/java/com/emme/ai/platform/adapter/out/capability/AiCaptionImageAdapter.java"))
        .doesNotExist();
  }

  private static Path sourcePath(String relativePath) {
    Path current = Path.of("").toAbsolutePath();
    while (current != null) {
      Path candidate = current.resolve(relativePath);
      if (Files.exists(current.resolve(".git"))) return candidate;
      current = current.getParent();
    }
    throw new IllegalStateException("Cannot locate source path: " + relativePath);
  }
}
