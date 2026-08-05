package com.emme.appointments.api.usecase;

import com.emme.appointments.api.result.AppointmentDetails;
import java.util.UUID;

/** Marks an appointment as a no-show. */
public interface MarkAppointmentNoShowUseCase {

  AppointmentDetails markNoShow(UUID id);
}
