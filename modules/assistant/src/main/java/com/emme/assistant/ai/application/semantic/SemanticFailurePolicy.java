package com.emme.assistant.ai.application.semantic;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;

/** Defines which semantic failures may degrade to a non-semantic path. */
public final class SemanticFailurePolicy {

  private SemanticFailurePolicy() {
    throw new UnsupportedOperationException("Utility class");
  }

  public static void rethrowSecurityFailure(RuntimeException failure) {
    if (isSecurityFailure(failure)) {
      throw failure;
    }
  }

  private static boolean isSecurityFailure(Throwable failure) {
    Throwable current = failure;
    while (current != null) {
      if (current instanceof SecurityException
          || current instanceof AccessDeniedException
          || current instanceof AuthenticationException) {
        return true;
      }
      current = current.getCause();
    }
    return false;
  }
}
