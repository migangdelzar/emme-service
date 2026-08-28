package com.emme.assistant.ai.application.port.out;

/** Signals that a chat provider cannot serve the current request. */
public final class ChatProviderUnavailableException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public ChatProviderUnavailableException(String message) {
    super(message);
  }

  public ChatProviderUnavailableException(String message, Throwable cause) {
    super(message, cause);
  }
}
