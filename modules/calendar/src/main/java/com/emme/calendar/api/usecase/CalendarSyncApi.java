package com.emme.calendar.api.usecase;

import com.emme.calendar.api.result.CalendarEventLinkInfo;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Public API for managing calendar event links. */
public interface CalendarSyncApi {

  List<CalendarEventLinkInfo> findByAppointmentId(UUID appointmentId);

  Optional<CalendarEventLinkInfo> findByTenantIdAndAppointmentId(UUID tenantId, UUID appointmentId);

  CalendarEventLinkInfo createLink(
      UUID tenantId, UUID appointmentId, String provider, String externalEventId);

  CalendarEventLinkInfo markSynced(UUID tenantId, UUID appointmentId, String etag);

  void markDeleted(UUID tenantId, UUID appointmentId);

  void markFailed(UUID tenantId, UUID appointmentId);
}
