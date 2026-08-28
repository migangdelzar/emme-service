package com.emme.assistant.ai.application.trace;

/** Application boundary for durable AI execution observability. */
public interface AiTraceRecorder {

  void recordModelExecution(AiModelExecutionTrace trace);

  void recordToolCall(AiToolCallTrace trace);
}
