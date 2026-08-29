package com.emme.ai.platform.model;

/** Signals that a model operation expired while waiting for bounded admission. */
public final class ModelAdmissionTimeoutException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public ModelAdmissionTimeoutException(String message) {
    super(message);
  }
}
