package com.emme.database;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class CalendarEventLinkMigrationContractTest {

  private static final String MIGRATION =
      "db/emme-studio/releases/0.1.0/034-calendar-event-link-cardinality.sql";

  @Test
  void enforcesOneCalendarLinkPerAppointmentAndProvider() throws IOException {
    assertThat(resource(MIGRATION))
        .contains("UNIQUE (tenant_id, appointment_id, provider)")
        .contains("calendar_event_link_duplicate_key")
        .contains("RAISE EXCEPTION");
  }

  @Test
  void isIncludedByTheStudioChangelog() throws IOException {
    assertThat(resource("db/emme-studio/changelog.yaml"))
        .contains("releases/0.1.0/034-calendar-event-link-cardinality.sql");
  }

  private static String resource(String path) throws IOException {
    try (InputStream stream =
        CalendarEventLinkMigrationContractTest.class.getClassLoader().getResourceAsStream(path)) {
      assertThat(stream).as("classpath resource %s", path).isNotNull();
      return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
