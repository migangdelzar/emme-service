package com.emme.kernel.tracing;

import java.util.UUID;

/**
 * Thread-local correlation ID for request tracing. Each HTTP request gets a unique correlation ID
 * propagated through logs, traces, and outbound calls.
 */
public final class CorrelationId {

  private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

  private CorrelationId() {
    throw new UnsupportedOperationException("Utility class");
  }

  public static String generate() {
    return UUID.randomUUID().toString().replace("-", "");
  }

  public static void set(String correlationId) {
    CURRENT.set(correlationId);
  }

  public static String get() {
    return CURRENT.get();
  }

  public static void clear() {
    CURRENT.remove();
  }
}
