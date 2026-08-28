package com.emme.assistant.ai.adapter.out.provider.springai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.emme.assistant.ai.application.port.out.NailDesignExtractionRejectedException;
import com.emme.assistant.ai.application.port.out.NailDesignExtractor;
import com.emme.assistant.ai.domain.quote.NailDesignFeatures;
import com.emme.assistant.ai.domain.quote.NailLength;
import com.emme.assistant.ai.domain.quote.NailShape;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
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
            key -> {
              throw new UnsupportedOperationException("image not configured");
            });

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
            key -> {
              throw new UnsupportedOperationException("image not configured");
            });

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
            key -> {
              throw new UnsupportedOperationException("image not configured");
            });

    assertThatThrownBy(
            () ->
                extractor.extract(
                    new NailDesignExtractor.ExtractionRequest("almond pink nails", null)))
        .isInstanceOf(NailDesignExtractionRejectedException.class)
        .hasMessage("Spring AI returned no nail-design features");
  }
}
