package com.emme.calendar.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class GoogleCalendarPropertiesTest {

  @Test
  void appliesProductionEndpointDefaultsWhenUnset() {
    GoogleCalendarProperties properties = new GoogleCalendarProperties(null, null, null);

    assertThat(properties.serviceAccountJsonBase64()).isEmpty();
    assertThat(properties.tokenUrl()).isEqualTo("https://oauth2.googleapis.com/token");
    assertThat(properties.freeBusyUrl())
        .isEqualTo("https://www.googleapis.com/calendar/v3/freeBusy");
  }

  @Test
  void preservesConfiguredCredentialAndEndpoints() {
    GoogleCalendarProperties properties =
        new GoogleCalendarProperties("encoded", "https://token.test", "https://freebusy.test");

    assertThat(properties.serviceAccountJsonBase64()).isEqualTo("encoded");
    assertThat(properties.tokenUrl()).isEqualTo("https://token.test");
    assertThat(properties.freeBusyUrl()).isEqualTo("https://freebusy.test");
  }
}
