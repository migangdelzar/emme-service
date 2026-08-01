package com.emme.calendar.domain.model;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class CalendarSyncStateTest {

  @Test
  void doesNotMarkFailedStateAsStale() {
    CalendarSyncState state =
        CalendarSyncState.active(UUID.randomUUID(), CalendarProvider.GOOGLE_CALENDAR);
    state.markFailed();

    assertThatThrownBy(state::markStale)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Cannot mark failed sync as stale");
  }
}
