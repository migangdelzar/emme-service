package com.emme.assistant.ai.application.tool;

/** Signals that a tool call failed a backend policy check. */
public final class AiToolExecutionRejectedException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public AiToolExecutionRejectedException(String message) {
    super(message);
  }
}
