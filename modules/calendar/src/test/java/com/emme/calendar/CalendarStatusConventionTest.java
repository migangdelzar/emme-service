package com.emme.calendar;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.calendar.api.result.CalendarEventLinkDetails;
import com.emme.calendar.api.result.CalendarSyncStateDetails;
import com.emme.calendar.domain.model.CalendarEventLinkStatus;
import com.emme.calendar.domain.model.CalendarSyncStatus;
import org.junit.jupiter.api.Test;

class CalendarStatusConventionTest {

  @Test
  void calendarStatusUsesDomainEnumsAcrossPublicBoundaries() {
    assertThat(CalendarEventLinkDetails.class.getRecordComponents()[5].getType())
        .isEqualTo(CalendarEventLinkStatus.class);
    assertThat(CalendarSyncStateDetails.class.getRecordComponents()[3].getType())
        .isEqualTo(CalendarSyncStatus.class);
  }
}
