package com.emme.ai.platform.model;

/** Signals that a waiting model operation was interrupted before admission. */
public final class ModelAdmissionInterruptedException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public ModelAdmissionInterruptedException(Throwable cause) {
    super("Model admission was interrupted", cause);
  }
}
