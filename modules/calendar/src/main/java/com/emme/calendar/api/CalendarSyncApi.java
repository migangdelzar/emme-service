package com.emme.calendar.api;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Public API for managing calendar event links. Implemented by the calendar module, consumed by
 * provider modules (e.g. google).
 */
public interface CalendarSyncApi {

  /** Find existing event links for an appointment. */
  List<CalendarEventLinkInfo> findByAppointmentId(UUID appointmentId);

  /** Find link by tenant and appointment. */
  Optional<CalendarEventLinkInfo> findByTenantIdAndAppointmentId(UUID tenantId, UUID appointmentId);

  /** Create a new link with PENDING status. */
  CalendarEventLinkInfo createLink(
      UUID tenantId, UUID appointmentId, String provider, String externalEventId);

  /** Mark a link as synced, saving etag. */
  CalendarEventLinkInfo markSynced(UUID tenantId, UUID appointmentId, String etag);

  /** Mark all links for an appointment as deleted. */
  void markDeleted(UUID tenantId, UUID appointmentId);

  /** Mark all links for an appointment as failed. */
  void markFailed(UUID tenantId, UUID appointmentId);
}
