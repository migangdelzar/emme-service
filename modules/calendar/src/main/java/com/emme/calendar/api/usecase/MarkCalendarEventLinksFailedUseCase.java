package com.emme.calendar.api.usecase;

import java.util.UUID;

/** Marks all calendar links for an appointment as failed. */
public interface MarkCalendarEventLinksFailedUseCase {

  void markFailed(UUID tenantId, UUID appointmentId);
}
