package com.emme.appointments.api.usecase;

import com.emme.appointments.api.result.AppointmentSummary;
import java.util.List;
import java.util.UUID;

/** Lists appointments exposed by Studio to other modules. */
public interface ListAppointmentsUseCase {

  List<AppointmentSummary> listAppointments(UUID tenantId);
}
