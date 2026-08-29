package com.emme.assistant.ai.adapter.out.graph;

/** Signals that an asynchronous graph projection must be retried. */
public final class AgeGraphUnavailableException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public AgeGraphUnavailableException() {
    super("Apache AGE graph projection is unavailable");
  }

  public AgeGraphUnavailableException(Throwable cause) {
    super("Apache AGE graph projection is unavailable", cause);
  }
}
