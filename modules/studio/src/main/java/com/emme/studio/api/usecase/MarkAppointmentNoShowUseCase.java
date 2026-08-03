package com.emme.studio.api.usecase;

import com.emme.studio.api.result.AppointmentDetails;
import java.util.UUID;

/** Marks an appointment as a no-show. */
public interface MarkAppointmentNoShowUseCase {

  AppointmentDetails markNoShow(UUID id);
}
