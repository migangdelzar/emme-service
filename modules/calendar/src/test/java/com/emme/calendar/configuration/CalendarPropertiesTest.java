package com.emme.calendar.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CalendarPropertiesTest {

  @Test
  void defaultsCalendarIdWhenThePropertyIsAbsent() {
    assertThat(new CalendarProperties(null).calendarId()).isEqualTo("primary");
  }

  @Test
  void preservesConfiguredCalendarId() {
    assertThat(new CalendarProperties("team-calendar").calendarId()).isEqualTo("team-calendar");
  }
}
