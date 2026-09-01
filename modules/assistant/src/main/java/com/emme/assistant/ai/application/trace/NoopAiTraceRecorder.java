package com.emme.assistant.ai.application.trace;

/** No-op trace recorder used when durable persistence is not available. */
public enum NoopAiTraceRecorder implements AiTraceRecorder {
  INSTANCE;

  @Override
  public void recordModelExecution(AiModelExecutionTrace trace) {
    // Intentionally empty: observability must not make an AI request fail.
  }

  @Override
  public void recordToolCall(AiToolCallTrace trace) {
    // Intentionally empty: observability must not make a tool request fail.
  }

  @Override
  public void recordSemanticOutcome(AiSemanticExecutionTrace trace) {
    // Intentionally empty: observability must not make a semantic request fail.
  }
}
