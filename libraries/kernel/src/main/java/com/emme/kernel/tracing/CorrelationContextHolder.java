package com.emme.kernel.tracing;

import com.emme.functional.unchecked.URunnable;
import com.emme.functional.unchecked.USupplier;
import org.slf4j.MDC;

public final class CorrelationContextHolder {

  private static final String MDC_KEY = "correlationId";

  private CorrelationContextHolder() {
    throw new UnsupportedOperationException("Utility class");
  }

  public static String requireCorrelationId() {
    String correlationId = CorrelationId.get();
    if (correlationId == null || correlationId.isBlank()) {
      throw new IllegalStateException("No correlation context");
    }
    return correlationId;
  }

  public static <T> T withCorrelationId(String correlationId, USupplier<T> action) {
    if (correlationId == null || correlationId.isBlank()) {
      throw new IllegalArgumentException("correlationId must not be blank");
    }

    String previousCorrelationId = CorrelationId.get();
    String previousMdc = MDC.get(MDC_KEY);
    try {
      CorrelationId.set(correlationId);
      MDC.put(MDC_KEY, correlationId);
      return action.get();
    } finally {
      restore(previousCorrelationId, previousMdc);
    }
  }

  public static void withCorrelationId(String correlationId, URunnable action) {
    withCorrelationId(
        correlationId,
        () -> {
          action.runThrows();
          return null;
        });
  }

  public static <T> T withGeneratedCorrelationId(USupplier<T> action) {
    return withCorrelationId(CorrelationId.generate(), action);
  }

  public static void withGeneratedCorrelationId(URunnable action) {
    withGeneratedCorrelationId(
        () -> {
          action.runThrows();
          return null;
        });
  }

  private static void restore(String correlationId, String mdcCorrelationId) {
    if (correlationId == null) {
      CorrelationId.clear();
    } else {
      CorrelationId.set(correlationId);
    }

    if (mdcCorrelationId == null) {
      MDC.remove(MDC_KEY);
    } else {
      MDC.put(MDC_KEY, mdcCorrelationId);
    }
  }
}
