package com.emme.assistant.ai.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.emme.ai.contracts.model.ModelExecutionScheduler;
import com.emme.assistant.ai.application.port.out.DesignImageReader;
import com.emme.assistant.ai.application.port.out.NailDesignExtractor;
import com.emme.assistant.ai.application.trace.NoopAiTraceRecorder;
import com.emme.assistant.ai.domain.quote.NailDesignFeatures;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
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
    when(imageReaders.getIfAvailable(any())).thenReturn((key, context) -> Optional.empty());

    NailDesignExtractor extractor =
        configuration.nailDesignExtractor(
            mock(ChatClient.class),
            new SpringAiExtractionProperties(true, "vision-v1", "prompt-v4", "schema-v2"),
            imageReaders);

    assertThat(extractor).isInstanceOf(NailDesignExtractor.class);
  }

  @Test
  void wiresTheSharedModelSchedulerIntoTheExtractorWhenAvailable() {
    SpringAiQuoteExtractionConfiguration configuration = new SpringAiQuoteExtractionConfiguration();
    ChatClient client = mock(ChatClient.class, RETURNS_DEEP_STUBS);
    NailDesignFeatures features = mock(NailDesignFeatures.class);
    when(client
            .prompt()
            .system(anyString())
            .user(anyString())
            .call()
            .entity(eq(NailDesignFeatures.class), any()))
        .thenReturn(features);
    ModelExecutionScheduler scheduler = mock(ModelExecutionScheduler.class);
    when(scheduler.execute(any(), any(), any(), any()))
        .thenAnswer(
            invocation -> invocation.getArgument(3, java.util.concurrent.Callable.class).call());
    ObjectProvider<DesignImageReader> imageReaders = emptyImageReaderProvider();
    when(imageReaders.getIfAvailable(any())).thenReturn((key, context) -> Optional.empty());

    NailDesignExtractor extractor =
        configuration.nailDesignExtractor(
            client,
            new SpringAiExtractionProperties(true, "vision-v1", "prompt-v1", "schema-v1"),
            imageReaders,
            NoopAiTraceRecorder.INSTANCE,
            Optional.of(scheduler),
            new AiExecutorProperties(2, 1, 1, Duration.ofSeconds(4)));

    AiExecutionContext context =
        new AiExecutionContext(
            UUID.randomUUID(),
            UUID.randomUUID(),
            Set.of("ROLE_CLIENT"),
            UUID.randomUUID(),
            UUID.randomUUID(),
            "trace",
            "idempotency");
    AiExecutionContextScope.call(
        context, () -> extractor.extract(new NailDesignExtractor.ExtractionRequest("pink", null)));

    org.mockito.Mockito.verify(scheduler)
        .execute(
            eq(com.emme.ai.contracts.model.ModelCapability.VISION),
            eq(context),
            eq(Duration.ofSeconds(4)),
            any());
  }

  @Test
  void rejectsMissingExtractionVersionMetadata() {
    assertThatThrownBy(() -> new SpringAiExtractionProperties(true, "vision-v1", " ", "schema-v2"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("promptVersion must not be blank");
  }

  @Test
  void suppliesSafeVersionDefaultsWhenTheOptionalIntegrationIsDisabled() {
    SpringAiExtractionProperties properties =
        new SpringAiExtractionProperties(false, null, null, null);

    assertThat(properties.modelVersion()).isEqualTo("ollama-gemma4:e4b-mlx");
    assertThat(properties.promptVersion()).isEqualTo("nail-design-v1");
    assertThat(properties.schemaVersion()).isEqualTo("nail-features-v1");
  }

  private static AiProperties aiProperties() {
    return new AiProperties(
        "mock",
        new AiProperties.ProviderConfig("gemma4:e4b-mlx", "http://localhost:11434", null),
        new AiProperties.EmbeddingConfig(
            "embeddinggemma:300m", "http://localhost:11434", null, 768),
        true);
  }

  @SuppressWarnings("unchecked")
  private static ObjectProvider<DesignImageReader> emptyImageReaderProvider() {
    return mock(ObjectProvider.class);
  }
}
