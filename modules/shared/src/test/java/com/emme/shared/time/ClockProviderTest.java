package com.emme.shared.time;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ClockProviderTest {

  @AfterEach
  void reset() {
    ClockProvider.reset();
  }

  @Test
  void shouldReturnSystemUtcByDefault() {
    Clock clock = ClockProvider.clock();
    assertThat(clock.getZone().getId()).isEqualTo("Z");
  }

  @Test
  void shouldUseFixedClockWhenSet() {
    Instant fixed = Instant.parse("2026-01-01T00:00:00Z");
    ClockProvider.setClock(Clock.fixed(fixed, java.time.ZoneOffset.UTC));

    assertThat(ClockProvider.instant()).isEqualTo(fixed);
  }

  @Test
  void shouldResetToSystemUtcAfterReset() {
    Instant fixed = Instant.parse("2026-01-01T00:00:00Z");
    ClockProvider.setClock(Clock.fixed(fixed, java.time.ZoneOffset.UTC));
    ClockProvider.reset();

    Instant afterReset = ClockProvider.instant();
    assertThat(afterReset).isAfter(fixed);
  }
}
