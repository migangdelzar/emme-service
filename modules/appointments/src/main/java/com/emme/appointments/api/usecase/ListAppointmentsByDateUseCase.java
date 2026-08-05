package com.emme.appointments.api.usecase;

import com.emme.appointments.api.result.AppointmentDetails;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Lists appointments for a tenant and local calendar date. */
public interface ListAppointmentsByDateUseCase {

  List<AppointmentDetails> list(UUID tenantId, LocalDate date);
}
