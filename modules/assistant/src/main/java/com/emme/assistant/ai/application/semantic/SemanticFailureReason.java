package com.emme.assistant.ai.application.semantic;

import com.emme.assistant.ai.application.port.out.EmbeddingProviderUnavailableException;
import java.util.Objects;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;

/** Maps semantic failures to stable, bounded metric reason codes. */
final class SemanticFailureReason {

  private SemanticFailureReason() {
    throw new UnsupportedOperationException("Utility class");
  }

  static String code(Throwable failure) {
    Objects.requireNonNull(failure, "failure must not be null");
    if (hasCause(failure, EmbeddingProviderUnavailableException.class)) {
      return "embedding_unavailable";
    }
    if (hasCause(failure, AuthenticationException.class)
        || hasCause(failure, AccessDeniedException.class)
        || hasCause(failure, SecurityException.class)) {
      return "security_failure";
    }
    if (hasCause(failure, TransientDataAccessException.class)) {
      return "transient_data_store";
    }
    if (hasCause(failure, DataAccessException.class)) {
      return "data_store_unavailable";
    }
    if (hasCause(failure, IllegalArgumentException.class)) {
      return "invalid_input";
    }
    return "unexpected_failure";
  }

  private static boolean hasCause(Throwable failure, Class<? extends Throwable> type) {
    Throwable current = failure;
    while (current != null) {
      if (type.isInstance(current)) {
        return true;
      }
      current = current.getCause();
    }
    return false;
  }
}
