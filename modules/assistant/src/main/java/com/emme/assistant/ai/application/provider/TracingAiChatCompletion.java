package com.emme.assistant.ai.application.provider;

import com.emme.ai.contracts.model.AiChatCompletion;
import com.emme.ai.contracts.model.ChatResponse;
import com.emme.assistant.ai.application.trace.AiExecutionStatus;
import com.emme.assistant.ai.application.trace.AiModelExecutionTrace;
import com.emme.assistant.ai.application.trace.AiTracePersistenceFailureReporter;
import com.emme.assistant.ai.application.trace.AiTraceRecorder;
import com.emme.kernel.context.AiExecutionContextScope;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Records each provider attempt without changing provider failure semantics. */
public final class TracingAiChatCompletion implements AiChatCompletion {

  private static final Logger LOGGER = LoggerFactory.getLogger(TracingAiChatCompletion.class);

  private final AiChatCompletion delegate;
  private final String providerKey;
  private final String modelVersion;
  private final String promptVersion;
  private final AiTraceRecorder recorder;

  public TracingAiChatCompletion(
      AiChatCompletion delegate,
      String providerKey,
      String modelVersion,
      String promptVersion,
      AiTraceRecorder recorder) {
    this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
    this.providerKey = requireText(providerKey, "providerKey");
    this.modelVersion = requireText(modelVersion, "modelVersion");
    this.promptVersion = requireText(promptVersion, "promptVersion");
    this.recorder = Objects.requireNonNull(recorder, "recorder must not be null");
  }

  @Override
  public ChatResponse complete(Request request) {
    Objects.requireNonNull(request, "request must not be null");
    String requestPayload =
        "conversationContext="
            + request.conversationContext()
            + "\nuserMessage="
            + request.userMessage();
    long startedAt = System.nanoTime();
    try {
      ChatResponse response = delegate.complete(request);
      record(
          new AiModelExecutionTrace(
              UUID.randomUUID(),
              "CHAT_COMPLETION",
              providerKey,
              modelVersion,
              promptVersion,
              null,
              AiExecutionStatus.SUCCEEDED,
              elapsedMillis(startedAt),
              null,
              null,
              null,
              null,
              requestPayload,
              response.content(),
              null,
              null));
      return response;
    } catch (RuntimeException failure) {
      record(
          new AiModelExecutionTrace(
              UUID.randomUUID(),
              "CHAT_COMPLETION",
              providerKey,
              modelVersion,
              promptVersion,
              null,
              AiExecutionStatus.FAILED,
              elapsedMillis(startedAt),
              null,
              null,
              null,
              null,
              requestPayload,
              null,
              failure.getClass().getSimpleName(),
              failure.getMessage()));
      throw failure;
    }
  }

  private void record(AiModelExecutionTrace trace) {
    if (AiExecutionContextScope.current().isEmpty()) return;
    try {
      recorder.recordModelExecution(trace);
    } catch (RuntimeException failure) {
      // Trace persistence is best effort and must not alter the provider result.
      AiTracePersistenceFailureReporter.report(LOGGER, trace.operation(), failure);
    }
  }

  private static long elapsedMillis(long startedAt) {
    return Math.max(0, (System.nanoTime() - startedAt) / 1_000_000);
  }

  private static String requireText(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
    return value;
  }
}
