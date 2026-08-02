package com.emme.shared.time;

import java.time.Clock;
import java.time.Instant;

/**
 * Injectable time source for deterministic testing. Use ClockProvider.clock() instead of
 * Instant.now() or System.currentTimeMillis().
 */
public final class ClockProvider {

  private static volatile Clock clock = Clock.systemUTC();

  private ClockProvider() {
    throw new UnsupportedOperationException("Utility class");
  }

  public static Clock clock() {
    return clock;
  }

  public static Instant instant() {
    return Instant.now(clock);
  }

  /**
   * For testing only. Sets a fixed clock. Must be called from test code or a test configuration.
   */
  public static void setClock(Clock testClock) {
    clock = testClock;
  }

  public static void reset() {
    clock = Clock.systemUTC();
  }
}
