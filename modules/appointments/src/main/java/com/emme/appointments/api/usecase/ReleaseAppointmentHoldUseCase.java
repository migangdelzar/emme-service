package com.emme.appointments.api.usecase;

import java.util.UUID;

public interface ReleaseAppointmentHoldUseCase {

  void release(UUID holdId);
}
