package com.emme.ai.platform.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.ai.contracts.embedding.EmbeddingService;
import com.emme.ai.contracts.model.AiChatCompletion;
import com.emme.ai.platform.adapter.out.provider.springai.SpringAiChatModel;
import com.emme.ai.platform.adapter.out.provider.springai.SpringAiEmbeddingModel;
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
}
