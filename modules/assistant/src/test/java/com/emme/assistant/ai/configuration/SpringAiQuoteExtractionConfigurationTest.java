package com.emme.assistant.ai.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.emme.assistant.ai.application.port.out.DesignImageReader;
import com.emme.assistant.ai.application.port.out.NailDesignExtractor;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.beans.factory.ObjectProvider;

class SpringAiQuoteExtractionConfigurationTest {

  @Test
  void createsAnOllamaChatModelOnlyThroughTheExplicitConfigurationBoundary() {
    SpringAiQuoteExtractionConfiguration configuration = new SpringAiQuoteExtractionConfiguration();

    assertThat(configuration.ollamaChatModel(aiProperties())).isInstanceOf(OllamaChatModel.class);
  }

  @Test
  void createsAChatClientFromTheInjectedChatModel() {
    SpringAiQuoteExtractionConfiguration configuration = new SpringAiQuoteExtractionConfiguration();

    assertThat(configuration.quoteExtractionChatClient(mock(ChatModel.class)))
        .isInstanceOf(ChatClient.class);
  }

  @Test
  void createsAProviderNeutralExtractorWithConfiguredVersionMetadata() {
    SpringAiQuoteExtractionConfiguration configuration = new SpringAiQuoteExtractionConfiguration();
    ObjectProvider<DesignImageReader> imageReaders = emptyImageReaderProvider();
    when(imageReaders.getIfAvailable(any())).thenReturn(key -> Optional.empty());

    NailDesignExtractor extractor =
        configuration.nailDesignExtractor(
            mock(ChatClient.class),
            new SpringAiExtractionProperties(true, "vision-v1", "prompt-v4", "schema-v2"),
            imageReaders);

    assertThat(extractor).isInstanceOf(NailDesignExtractor.class);
  }

  @Test
  void rejectsMissingExtractionVersionMetadata() {
    assertThatThrownBy(() -> new SpringAiExtractionProperties(true, "vision-v1", " ", "schema-v2"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("promptVersion must not be blank");
  }

  private static AiProperties aiProperties() {
    return new AiProperties(
        "mock",
        new AiProperties.ProviderConfig("gemma3:4b", "http://localhost:11434", null),
        new AiProperties.EmbeddingConfig("bge-m3", "http://localhost:11434", null, 1024),
        true);
  }

  @SuppressWarnings("unchecked")
  private static ObjectProvider<DesignImageReader> emptyImageReaderProvider() {
    return mock(ObjectProvider.class);
  }
}
