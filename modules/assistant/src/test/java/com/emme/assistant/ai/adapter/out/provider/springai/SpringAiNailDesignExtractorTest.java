package com.emme.assistant.ai.adapter.out.provider.springai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.emme.ai.contracts.model.ModelCapability;
import com.emme.ai.contracts.model.ModelExecutionScheduler;
import com.emme.assistant.ai.application.port.out.NailDesignExtractionRejectedException;
import com.emme.assistant.ai.application.port.out.NailDesignExtractor;
import com.emme.assistant.ai.application.trace.AiExecutionStatus;
import com.emme.assistant.ai.application.trace.AiModelExecutionTrace;
import com.emme.assistant.ai.application.trace.AiTraceRecorder;
import com.emme.assistant.ai.application.trace.NoopAiTraceRecorder;
import com.emme.assistant.ai.domain.quote.NailDesignFeatures;
import com.emme.assistant.ai.domain.quote.NailLength;
import com.emme.assistant.ai.domain.quote.NailShape;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;

class SpringAiNailDesignExtractorTest {

  private static final NailDesignFeatures FEATURES =
      new NailDesignFeatures(
          NailShape.ALMOND,
          NailLength.MEDIUM,
          "pink",
          List.of(),
          List.of(),
          null,
          false,
          false,
          null,
          Map.of("shape", 0.98),
          List.of(),
          false);

  @Test
  void acceptsOneExplicitExecutionConfiguration() {
    ChatClient client = mock(ChatClient.class, RETURNS_DEEP_STUBS);
    when(client
            .prompt()
            .system(anyString())
            .user(anyString())
            .call()
            .entity(eq(NailDesignFeatures.class), any()))
        .thenReturn(FEATURES);
    SpringAiNailDesignExtractor extractor =
        new SpringAiNailDesignExtractor(
            client,
            "vision-v1",
            "quote-prompt-v1",
            "nail-features-v1",
            (key, context) -> Optional.empty(),
            NoopAiTraceRecorder.INSTANCE,
            Optional.empty(),
            Duration.ofSeconds(5));

    assertThat(
            extractor.extract(new NailDesignExtractor.ExtractionRequest("pink", null)).features())
        .isEqualTo(FEATURES);
  }

  @Test
  void requestsAValidatedProviderStructuredNailDesignEntity() {
    ChatClient client = mock(ChatClient.class, RETURNS_DEEP_STUBS);
    when(client
            .prompt()
            .system(anyString())
            .user(anyString())
            .call()
            .entity(eq(NailDesignFeatures.class), any()))
        .thenReturn(FEATURES);
    SpringAiNailDesignExtractor extractor =
        new SpringAiNailDesignExtractor(
            client,
            "vision-v1",
            "quote-prompt-v1",
            "nail-features-v1",
            (key, context) -> {
              throw new UnsupportedOperationException("image not configured");
            },
            NoopAiTraceRecorder.INSTANCE,
            Optional.empty(),
            Duration.ofSeconds(5));

    NailDesignExtractor.ExtractionResult result =
        extractor.extract(new NailDesignExtractor.ExtractionRequest("almond pink nails", null));

    assertThat(result.features()).isEqualTo(FEATURES);
    assertThat(result.modelVersion()).isEqualTo("vision-v1");
    assertThat(result.promptVersion()).isEqualTo("quote-prompt-v1");
  }

  @Test
  void convertsProviderFailuresToARejectedExtraction() {
    ChatClient client = mock(ChatClient.class, RETURNS_DEEP_STUBS);
    when(client
            .prompt()
            .system(anyString())
            .user(anyString())
            .call()
            .entity(eq(NailDesignFeatures.class), any()))
        .thenThrow(new IllegalStateException("model timeout"));
    SpringAiNailDesignExtractor extractor =
        new SpringAiNailDesignExtractor(
            client,
            "vision-v1",
            "quote-prompt-v1",
            "nail-features-v1",
            (key, context) -> {
              throw new UnsupportedOperationException("image not configured");
            },
            NoopAiTraceRecorder.INSTANCE,
            Optional.empty(),
            Duration.ofSeconds(5));

    assertThatThrownBy(
            () ->
                extractor.extract(
                    new NailDesignExtractor.ExtractionRequest("almond pink nails", null)))
        .isInstanceOf(NailDesignExtractionRejectedException.class)
        .hasMessage("Spring AI nail-design extraction failed");
  }

  @Test
  void rejectsAProviderThatReturnsNoStructuredEntity() {
    ChatClient client = mock(ChatClient.class, RETURNS_DEEP_STUBS);
    when(client
            .prompt()
            .system(anyString())
            .user(anyString())
            .call()
            .entity(eq(NailDesignFeatures.class), any()))
        .thenReturn(null);
    SpringAiNailDesignExtractor extractor =
        new SpringAiNailDesignExtractor(
            client,
            "vision-v1",
            "quote-prompt-v1",
            "nail-features-v1",
            (key, context) -> {
              throw new UnsupportedOperationException("image not configured");
            },
            NoopAiTraceRecorder.INSTANCE,
            Optional.empty(),
            Duration.ofSeconds(5));

    assertThatThrownBy(
            () ->
                extractor.extract(
                    new NailDesignExtractor.ExtractionRequest("almond pink nails", null)))
        .isInstanceOf(NailDesignExtractionRejectedException.class)
        .hasMessage("Spring AI returned no nail-design features");
  }

  @Test
  void recordsStructuredExtractionMetadataWithoutStoringTheImageBytes() {
    ChatClient client = mock(ChatClient.class, RETURNS_DEEP_STUBS);
    when(client
            .prompt()
            .system(anyString())
            .user(anyString())
            .call()
            .entity(eq(NailDesignFeatures.class), any()))
        .thenReturn(FEATURES);
    AiTraceRecorder recorder = mock(AiTraceRecorder.class);
    SpringAiNailDesignExtractor extractor =
        new SpringAiNailDesignExtractor(
            client,
            "vision-v1",
            "quote-prompt-v1",
            "nail-features-v1",
            (key, context) -> {
              throw new UnsupportedOperationException("image not configured");
            },
            recorder,
            Optional.empty(),
            Duration.ofSeconds(5));

    AiExecutionContext context = context();
    AiExecutionContextScope.call(
        context,
        () -> extractor.extract(new NailDesignExtractor.ExtractionRequest("pink design", null)));

    ArgumentCaptor<AiModelExecutionTrace> trace =
        ArgumentCaptor.forClass(AiModelExecutionTrace.class);
    org.mockito.Mockito.verify(recorder).recordModelExecution(trace.capture());
    assertThat(trace.getValue().status()).isEqualTo(AiExecutionStatus.SUCCEEDED);
    assertThat(trace.getValue().operation()).isEqualTo("DESIGN_EXTRACTION");
    assertThat(trace.getValue().modelVersion()).isEqualTo("vision-v1");
    assertThat(trace.getValue().promptVersion()).isEqualTo("quote-prompt-v1");
    assertThat(trace.getValue().responsePayload()).isEqualTo("structured-features");
    assertThat(trace.getValue().requestPayload()).contains("imagePresent=false");
    assertThat(trace.getValue().requestPayload()).doesNotContain("image-bytes");
  }

  @Test
  void admitsVisionExtractionThroughTheSharedModelScheduler() {
    ChatClient client = mock(ChatClient.class, RETURNS_DEEP_STUBS);
    when(client
            .prompt()
            .system(anyString())
            .user(anyString())
            .call()
            .entity(eq(NailDesignFeatures.class), any()))
        .thenReturn(FEATURES);
    RecordingScheduler scheduler = new RecordingScheduler();
    SpringAiNailDesignExtractor extractor =
        new SpringAiNailDesignExtractor(
            client,
            "vision-v1",
            "quote-prompt-v1",
            "nail-features-v1",
            (key, context) -> {
              throw new UnsupportedOperationException("image not configured");
            },
            NoopAiTraceRecorder.INSTANCE,
            Optional.of(scheduler),
            Duration.ofSeconds(3));

    AiExecutionContext expectedContext = context();
    AiExecutionContextScope.call(
        expectedContext,
        () -> extractor.extract(new NailDesignExtractor.ExtractionRequest("pink design", null)));

    assertThat(scheduler.capability).isEqualTo(ModelCapability.VISION);
    assertThat(scheduler.timeout).isEqualTo(Duration.ofSeconds(3));
    assertThat(scheduler.context).isEqualTo(expectedContext);
    assertThat(scheduler.invocations).isEqualTo(1);
  }

  private static final class RecordingScheduler implements ModelExecutionScheduler {
    private ModelCapability capability;
    private AiExecutionContext context;
    private Duration timeout;
    private int invocations;

    @Override
    public <T> T execute(
        ModelCapability capability,
        AiExecutionContext context,
        Duration admissionTimeout,
        java.util.concurrent.Callable<T> operation) {
      this.capability = capability;
      this.context = context;
      this.timeout = admissionTimeout;
      invocations++;
      try {
        return operation.call();
      } catch (Exception exception) {
        throw new RuntimeException(exception);
      }
    }
  }

  private static AiExecutionContext context() {
    UUID id = UUID.randomUUID();
    return new AiExecutionContext(
        UUID.randomUUID(),
        UUID.randomUUID(),
        java.util.Set.of("ROLE_CLIENT"),
        id,
        id,
        "trace-1",
        "idem-1");
  }
}
