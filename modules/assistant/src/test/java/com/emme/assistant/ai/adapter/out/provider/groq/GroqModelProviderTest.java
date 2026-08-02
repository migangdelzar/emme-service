package com.emme.assistant.ai.adapter.out.provider.groq;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.emme.assistant.ai.configuration.AiHttpClient;
import com.emme.assistant.ai.configuration.AiProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class GroqModelProviderTest {

  @Test
  void returnsAnEmptyEmbeddingWhenTheProviderDoesNotSupportEmbeddings() {
    AiProperties properties =
        new AiProperties(
            "groq",
            new AiProperties.ProviderConfig("model", "https://groq.example", "secret"),
            new AiProperties.EmbeddingConfig("embedding", "https://groq.example", null, 768),
            false);
    GroqModelProvider provider =
        new GroqModelProvider(properties, mock(AiHttpClient.class), new ObjectMapper());

    assertThat(provider.embed("text")).isEmpty();
  }
}
