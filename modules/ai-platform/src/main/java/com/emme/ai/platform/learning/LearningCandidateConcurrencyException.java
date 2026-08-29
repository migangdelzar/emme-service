package com.emme.ai.platform.learning;

/** Raised when a concurrent worker changed a candidate before its transition was persisted. */
public final class LearningCandidateConcurrencyException extends IllegalStateException {

  private static final long serialVersionUID = 1L;

  public LearningCandidateConcurrencyException() {
    super("Learning candidate changed concurrently");
  }
}
