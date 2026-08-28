package com.emme.assistant.ai.adapter.out.provider.springai;

import com.emme.assistant.ai.application.port.out.DesignImageReader;
import com.emme.assistant.ai.application.port.out.NailDesignExtractionRejectedException;
import com.emme.assistant.ai.application.port.out.NailDesignExtractor;
import com.emme.assistant.ai.application.trace.AiExecutionStatus;
import com.emme.assistant.ai.application.trace.AiModelExecutionTrace;
import com.emme.assistant.ai.application.trace.AiTraceRecorder;
import com.emme.assistant.ai.application.trace.NoopAiTraceRecorder;
import com.emme.assistant.ai.domain.quote.NailDesignFeatures;
import com.emme.kernel.context.AiExecutionContextScope;
import java.util.Objects;
import java.util.UUID;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.content.Media;
import org.springframework.util.MimeTypeUtils;

/** Spring AI structured-output adapter for text and securely loaded design images. */
public final class SpringAiNailDesignExtractor implements NailDesignExtractor {

  private static final String SYSTEM_PROMPT =
      "You extract nail-design attributes for Emme. Return only the requested structured schema. "
          + "Use null when a field is not visible or cannot be determined. "
          + "Use confidenceByField values from 0 to 1 and list every ambiguity. "
          + "Never calculate prices, availability, or appointments.";

  private final ChatClient chatClient;
  private final String modelVersion;
  private final String promptVersion;
  private final String schemaVersion;
  private final DesignImageReader imageReader;
  private final AiTraceRecorder traceRecorder;

  public SpringAiNailDesignExtractor(
      ChatClient chatClient,
      String modelVersion,
      String promptVersion,
      String schemaVersion,
      DesignImageReader imageReader) {
    this(
        chatClient,
        modelVersion,
        promptVersion,
        schemaVersion,
        imageReader,
        NoopAiTraceRecorder.INSTANCE);
  }

  public SpringAiNailDesignExtractor(
      ChatClient chatClient,
      String modelVersion,
      String promptVersion,
      String schemaVersion,
      DesignImageReader imageReader,
      AiTraceRecorder traceRecorder) {
    this.chatClient = Objects.requireNonNull(chatClient, "chatClient must not be null");
    this.modelVersion = requireText(modelVersion, "modelVersion");
    this.promptVersion = requireText(promptVersion, "promptVersion");
    this.schemaVersion = requireText(schemaVersion, "schemaVersion");
    this.imageReader = Objects.requireNonNull(imageReader, "imageReader must not be null");
    this.traceRecorder = Objects.requireNonNull(traceRecorder, "traceRecorder must not be null");
  }

  @Override
  public ExtractionResult extract(ExtractionRequest request) {
    Objects.requireNonNull(request, "request must not be null");
    long startedAt = System.nanoTime();
    try {
      NailDesignFeatures features =
          request.imageStorageKey() == null ? extractText(request) : extractImage(request);
      if (features == null) {
        throw new NailDesignExtractionRejectedException(
            "Spring AI returned no nail-design features");
      }
      ExtractionResult result =
          new ExtractionResult(features, modelVersion, promptVersion, schemaVersion);
      record(
          new AiModelExecutionTrace(
              UUID.randomUUID(),
              "DESIGN_EXTRACTION",
              "spring-ai",
              modelVersion,
              promptVersion,
              null,
              AiExecutionStatus.SUCCEEDED,
              elapsedMillis(startedAt),
              null,
              null,
              null,
              null,
              requestPayload(request),
              "structured-features",
              null,
              null));
      return result;
    } catch (NailDesignExtractionRejectedException exception) {
      recordFailure(request, startedAt, exception);
      throw exception;
    } catch (RuntimeException exception) {
      NailDesignExtractionRejectedException rejected =
          new NailDesignExtractionRejectedException(
              "Spring AI nail-design extraction failed", exception);
      recordFailure(request, startedAt, rejected);
      throw rejected;
    }
  }

  private void recordFailure(
      NailDesignExtractor.ExtractionRequest request, long startedAt, RuntimeException failure) {
    record(
        new AiModelExecutionTrace(
            UUID.randomUUID(),
            "DESIGN_EXTRACTION",
            "spring-ai",
            modelVersion,
            promptVersion,
            null,
            AiExecutionStatus.FAILED,
            elapsedMillis(startedAt),
            null,
            null,
            null,
            null,
            requestPayload(request),
            null,
            failure.getClass().getSimpleName(),
            failure.getMessage()));
  }

  private void record(AiModelExecutionTrace trace) {
    if (AiExecutionContextScope.current().isEmpty()) return;
    try {
      traceRecorder.recordModelExecution(trace);
    } catch (RuntimeException ignored) {
      // Trace persistence is best effort and must not alter extraction semantics.
    }
  }

  private static String requestPayload(NailDesignExtractor.ExtractionRequest request) {
    return "inputText="
        + String.valueOf(request.inputText())
        + "; imagePresent="
        + (request.imageStorageKey() != null);
  }

  private static long elapsedMillis(long startedAt) {
    return Math.max(0, (System.nanoTime() - startedAt) / 1_000_000);
  }

  private NailDesignFeatures extractText(ExtractionRequest request) {
    return structuredEntity(
        chatClient.prompt().system(SYSTEM_PROMPT).user(request.inputText()).call());
  }

  private NailDesignFeatures extractImage(ExtractionRequest request) {
    DesignImageReader.StoredImage image =
        imageReader
            .read(request.imageStorageKey())
            .orElseThrow(
                () ->
                    new NailDesignExtractionRejectedException(
                        "Design image is unavailable: " + request.imageStorageKey()));
    String userText =
        request.inputText() == null
            ? "Analyze the attached nail-design image."
            : request.inputText();
    return structuredEntity(
        chatClient
            .prompt()
            .system(SYSTEM_PROMPT)
            .user(
                user ->
                    user.text(userText)
                        .media(
                            Media.builder()
                                .mimeType(MimeTypeUtils.parseMimeType(image.mediaType()))
                                .data(image.bytes())
                                .build()))
            .call());
  }

  private NailDesignFeatures structuredEntity(ChatClient.CallResponseSpec response) {
    return response.entity(
        NailDesignFeatures.class,
        options -> options.validateSchema().useProviderStructuredOutput());
  }

  private static String requireText(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
    return value;
  }
}
