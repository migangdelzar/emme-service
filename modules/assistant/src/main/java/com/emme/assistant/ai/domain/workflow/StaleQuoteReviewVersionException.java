package com.emme.assistant.ai.domain.workflow;

/** Raised when a staff review command is based on an older task version. */
public final class StaleQuoteReviewVersionException extends IllegalStateException {

  private static final long serialVersionUID = 1L;

  public StaleQuoteReviewVersionException(long expectedVersion, long actualVersion) {
    super("Stale quote review version: expected " + expectedVersion + " but was " + actualVersion);
  }
}
