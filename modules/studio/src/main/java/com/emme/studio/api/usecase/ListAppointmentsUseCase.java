package com.emme.studio.api.usecase;

import com.emme.studio.api.result.AppointmentInfo;
import java.util.List;
import java.util.UUID;

/** Lists appointments exposed by Studio to other modules. */
public interface ListAppointmentsUseCase {

  List<AppointmentInfo> listAppointments(UUID tenantId);
}
