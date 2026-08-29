package com.emme.ai.platform.model;

/** Signals that bounded model admission rejected work because capacity was full. */
public final class ModelAdmissionRejectedException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public ModelAdmissionRejectedException(String message) {
    super(message);
  }
}
