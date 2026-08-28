package com.emme.ai.platform.adapter.out.provider.groq;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.emme.ai.platform.configuration.AiProviderHttpClient;
import com.emme.ai.platform.configuration.AiProviderProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class GroqModelProviderTest {

  @Test
  void returnsAnEmptyEmbeddingWhenTheProviderDoesNotSupportEmbeddings() {
    AiProviderProperties properties =
        new AiProviderProperties(
            "groq",
            new AiProviderProperties.ProviderConfig("model", "https://groq.example", "secret"),
            new AiProviderProperties.EmbeddingConfig(
                "embedding", "https://groq.example", null, 768),
            false);
    GroqModelProvider provider =
        new GroqModelProvider(properties, mock(AiProviderHttpClient.class), new ObjectMapper());

    assertThat(provider.embed("text")).isEmpty();
  }
}
