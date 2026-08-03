package com.emme.calendar.application.port.out;

import java.time.Instant;
import java.util.UUID;

/** Outbound capability for synchronizing an appointment with a client calendar provider. */
public interface ClientCalendarSyncPort {

  String sync(
      UUID tenantId,
      UUID appointmentId,
      String userId,
      Instant startsAt,
      Instant endsAt,
      String summary,
      String description);

  void unsync(UUID tenantId, UUID appointmentId, String userId);
}
