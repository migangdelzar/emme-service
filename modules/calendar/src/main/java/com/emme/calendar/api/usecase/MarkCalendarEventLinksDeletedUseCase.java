package com.emme.calendar.api.usecase;

import java.util.UUID;

/** Marks all calendar links for an appointment as deleted. */
public interface MarkCalendarEventLinksDeletedUseCase {

  void markDeleted(UUID tenantId, UUID appointmentId);
}
