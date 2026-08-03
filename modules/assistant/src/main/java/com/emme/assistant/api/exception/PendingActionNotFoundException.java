package com.emme.assistant.api.exception;

import java.util.UUID;

public class PendingActionNotFoundException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public PendingActionNotFoundException(UUID actionId) {
    super("Pending action not found: " + actionId);
  }
}
