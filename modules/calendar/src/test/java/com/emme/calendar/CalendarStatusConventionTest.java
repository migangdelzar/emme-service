package com.emme.calendar;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.calendar.api.result.CalendarEventLinkDetails;
import com.emme.calendar.api.result.CalendarSyncStateDetails;
import com.emme.calendar.api.type.CalendarEventLinkStatus;
import com.emme.calendar.api.type.CalendarSyncStatus;
import org.junit.jupiter.api.Test;

class CalendarStatusConventionTest {

  @Test
  void calendarStatusUsesApiOwnedEnumsAcrossPublicBoundaries() {
    assertThat(CalendarEventLinkDetails.class.getRecordComponents()[5].getType())
        .isEqualTo(CalendarEventLinkStatus.class);
    assertThat(CalendarSyncStateDetails.class.getRecordComponents()[3].getType())
        .isEqualTo(CalendarSyncStatus.class);
  }
}
