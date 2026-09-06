package com.emme.appointments.application.service;

import com.emme.appointments.api.usecase.ReleaseAppointmentHoldUseCase;
import com.emme.appointments.application.port.out.AppointmentHoldRepository;
import java.util.Objects;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

/** Releases a tenant-local appointment hold after payment or workflow cancellation. */
@Transactional
public final class ReleaseAppointmentHoldService implements ReleaseAppointmentHoldUseCase {

  private final AppointmentHoldRepository holds;

  public ReleaseAppointmentHoldService(AppointmentHoldRepository holds) {
    this.holds = Objects.requireNonNull(holds, "holds must not be null");
  }

  @Override
  public void release(UUID holdId) {
    holds.deleteById(Objects.requireNonNull(holdId, "holdId must not be null"));
  }
}
