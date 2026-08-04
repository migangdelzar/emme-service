package com.emme.shared.web.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Reads the authenticated subject without coupling business modules to Identity adapters. */
public final class CurrentUserContextHolder {

  private CurrentUserContextHolder() {
    throw new UnsupportedOperationException("Utility class");
  }

  /** Returns the subject selected by Spring Security for the current request. */
  public static String currentSubject() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null
        || authentication.getName() == null
        || authentication.getName().isBlank()) {
      throw new IllegalStateException("No authenticated user context");
    }
    return authentication.getName();
  }
}
