package com.emme.assistant.ai.application.trace;

/** Application boundary for durable AI execution observability. */
public interface AiTraceRecorder {

  void recordModelExecution(AiModelExecutionTrace trace);

  void recordToolCall(AiToolCallTrace trace);

  default void recordSemanticOutcome(AiSemanticExecutionTrace trace) {
    // Implementations may opt into durable semantic tracing without breaking older adapters.
  }
}
