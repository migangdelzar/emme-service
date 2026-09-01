package com.emme.assistant.ai.application.provider;

/** Signals that an answer cannot be grounded because retrieval is unavailable or empty. */
public final class RetrievalUnavailableException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public RetrievalUnavailableException() {
    super("RAG retrieval unavailable");
  }

  public RetrievalUnavailableException(Throwable cause) {
    super("RAG retrieval unavailable", cause);
  }
}
