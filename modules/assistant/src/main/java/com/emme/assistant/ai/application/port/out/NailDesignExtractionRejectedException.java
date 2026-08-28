package com.emme.assistant.ai.application.port.out;

/** Signals that model output could not satisfy the strict nail-design extraction contract. */
public final class NailDesignExtractionRejectedException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public NailDesignExtractionRejectedException(String message) {
    super(message);
  }

  public NailDesignExtractionRejectedException(String message, Throwable cause) {
    super(message, cause);
  }
}
