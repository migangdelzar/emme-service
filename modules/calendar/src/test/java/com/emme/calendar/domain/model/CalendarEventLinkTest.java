package com.emme.calendar.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class CalendarEventLinkTest {

  @Test
  void marksPendingLinkAsSynced() {
    CalendarEventLink link =
        CalendarEventLink.pending(
            UUID.randomUUID(), UUID.randomUUID(), CalendarProvider.GOOGLE_CALENDAR, "event-1");

    link.markSynced("etag-1");

    assertThat(link.status()).isEqualTo(CalendarEventLinkStatus.SYNCED);
    assertThat(link.etag()).isEqualTo("etag-1");
  }

  @Test
  void rejectsSyncingAnAlreadySyncedLink() {
    CalendarEventLink link =
        CalendarEventLink.pending(
            UUID.randomUUID(), UUID.randomUUID(), CalendarProvider.GOOGLE_CALENDAR, "event-1");
    link.markSynced("etag-1");

    assertThatThrownBy(() -> link.markSynced("etag-2"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Cannot mark synced");
  }
}
