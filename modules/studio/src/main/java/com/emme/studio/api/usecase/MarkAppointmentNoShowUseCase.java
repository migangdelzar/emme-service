package com.emme.studio.api.usecase;

import com.emme.studio.application.result.AppointmentView;
import java.util.UUID;

/** Marks an appointment as a no-show. */
public interface MarkAppointmentNoShowUseCase {

  AppointmentView markNoShow(UUID id);
}
