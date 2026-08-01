package com.emme.studio.api.usecase;

import com.emme.studio.application.result.AppointmentView;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Lists appointments for a tenant and local calendar date. */
public interface ListAppointmentsByDateUseCase {

  List<AppointmentView> list(UUID tenantId, LocalDate date);
}
