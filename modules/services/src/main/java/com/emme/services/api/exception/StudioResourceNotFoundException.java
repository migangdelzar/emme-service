package com.emme.services.api.exception;

import java.util.UUID;

/** Indicates that a requested Studio resource does not exist. */
public final class StudioResourceNotFoundException extends IllegalArgumentException {

  private static final long serialVersionUID = 1L;

  public StudioResourceNotFoundException(String resourceType, UUID resourceId) {
    super(resourceType + " not found: " + resourceId);
  }
}
