package com.emme.assistant.ai.application.port.out;

/** Signals that a provider cannot serve the current embedding request. */
public final class EmbeddingProviderUnavailableException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public EmbeddingProviderUnavailableException(String message) {
    super(message);
  }

  public EmbeddingProviderUnavailableException(String message, Throwable cause) {
    super(message, cause);
  }
}
