package com.emme.studio.api.usecase;

import com.emme.studio.api.result.AppointmentSummary;
import java.util.List;
import java.util.UUID;

/** Lists appointments exposed by Studio to other modules. */
public interface ListAppointmentsUseCase {

  List<AppointmentSummary> listAppointments(UUID tenantId);
}
