package com.emme.calendar;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.calendar.adapter.out.google.client.GoogleCalendarClient;
import com.emme.calendar.api.result.CalendarBusyTimeRange;
import com.emme.calendar.configuration.GoogleCalendarProperties;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class GoogleCalendarClientLiveTest {

  @Test
  void shouldAuthAndGetFreeBusy() throws Exception {
    String saJson = System.getenv("GOOGLE_SA_JSON_BASE64");
    if (saJson == null || saJson.isBlank()) {
      System.out.println("⏭️  GOOGLE_SA_JSON_BASE64 not set — skipping live test");
      return;
    }

    GoogleCalendarClient client =
        new GoogleCalendarClient(
            Optional.empty(), new GoogleCalendarProperties(saJson, null, null));

    // Verify auth works (token obtained)
    String token = client.getAccessToken(false);
    assertThat(token).isNotEmpty();
    System.out.println("✅ Auth OK — token obtained (" + token.length() + " chars)");

    // Verify free/busy API works
    String now = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
    String tomorrow = DateTimeFormatter.ISO_INSTANT.format(Instant.now().plus(1, ChronoUnit.DAYS));
    List<CalendarBusyTimeRange> busy = client.freeBusy("primary", now, tomorrow);

    assertThat(busy).isNotNull();
    System.out.println("✅ Free/busy OK — " + busy.size() + " busy slots found");

    if (!busy.isEmpty()) {
      busy.forEach(b -> System.out.println("  Busy: " + b.start() + " → " + b.end()));
    } else {
      System.out.println("  (calendar is empty — no busy slots)");
    }
  }
}
