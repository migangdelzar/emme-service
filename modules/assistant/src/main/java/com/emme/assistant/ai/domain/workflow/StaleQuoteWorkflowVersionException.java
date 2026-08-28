package com.emme.assistant.ai.domain.workflow;

/** Raised when a workflow command was based on an older persisted version. */
public final class StaleQuoteWorkflowVersionException extends IllegalStateException {

  private static final long serialVersionUID = 1L;

  public StaleQuoteWorkflowVersionException(long expectedVersion, long actualVersion) {
    super(
        "Stale quote workflow version: expected " + expectedVersion + " but was " + actualVersion);
  }
}
