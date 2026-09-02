package com.emme.assistant.ai.application.provider;

import com.emme.assistant.ai.application.port.out.ChatProviderUnavailableException;

/**
 * Classifies provider failures without converting request or schema errors into failover signals.
 */
public final class ChatProviderFailurePolicy {

  private ChatProviderFailurePolicy() {}

  public static RuntimeException preserveInputOrUnavailable(
      String providerKey, RuntimeException failure) {
    if (failure instanceof ChatProviderUnavailableException) {
      return failure;
    }
    if (isInputOrSchemaFailure(failure) && !isMissingCredentials(failure)) {
      return failure;
    }
    return new ChatProviderUnavailableException(
        "Chat provider '" + providerKey + "' is unavailable", failure);
  }

  private static boolean isInputOrSchemaFailure(Throwable failure) {
    Throwable current = failure;
    while (current != null) {
      if (current instanceof IllegalArgumentException) {
        return true;
      }
      String message = current.getMessage();
      if (message != null) {
        String normalized = message.toLowerCase(java.util.Locale.ROOT);
        if (normalized.contains("schema")
            || normalized.contains("invalid input")
            || normalized.contains("invalid request")
            || normalized.contains("malformed")) {
          return true;
        }
      }
      current = current.getCause();
    }
    return false;
  }

  private static boolean isMissingCredentials(Throwable failure) {
    Throwable current = failure;
    while (current != null) {
      String message = current.getMessage();
      if (message != null) {
        String normalized = message.toLowerCase(java.util.Locale.ROOT);
        boolean namesCredential =
            normalized.contains("api key")
                || normalized.contains("api_key")
                || normalized.contains("apikey")
                || normalized.contains("credential");
        boolean describesMissing =
            normalized.contains("blank")
                || normalized.contains("missing")
                || normalized.contains("required")
                || normalized.contains("not configured")
                || normalized.contains("not provided");
        if (namesCredential && describesMissing) {
          return true;
        }
      }
      current = current.getCause();
    }
    return false;
  }
}
