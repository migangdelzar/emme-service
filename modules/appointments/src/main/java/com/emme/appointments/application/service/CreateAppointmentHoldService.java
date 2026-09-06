package com.emme.appointments.application.service;

import com.emme.ai.contracts.appointment.AppointmentHold;
import com.emme.appointments.api.command.CreateAppointmentHoldCommand;
import com.emme.appointments.api.usecase.CreateAppointmentHoldUseCase;
import com.emme.appointments.application.port.out.AppointmentHoldRepository;
import com.emme.appointments.application.port.out.AppointmentRepository;
import com.emme.appointments.domain.model.Appointment;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Creates tenant-local appointment holds with an idempotent business key. */
public final class CreateAppointmentHoldService implements CreateAppointmentHoldUseCase {

  private final AppointmentRepository appointments;
  private final AppointmentHoldRepository holds;
  private final Clock clock;
  private final Duration holdDuration;

  public CreateAppointmentHoldService(
      AppointmentRepository appointments,
      AppointmentHoldRepository holds,
      Clock clock,
      Duration holdDuration) {
    this.appointments = Objects.requireNonNull(appointments, "appointments must not be null");
    this.holds = Objects.requireNonNull(holds, "holds must not be null");
    this.clock = Objects.requireNonNull(clock, "clock must not be null");
    this.holdDuration = Objects.requireNonNull(holdDuration, "holdDuration must not be null");
    if (holdDuration.isZero() || holdDuration.isNegative()) {
      throw new IllegalArgumentException("holdDuration must be positive");
    }
  }

  @Override
  public AppointmentHold create(CreateAppointmentHoldCommand command) {
    Objects.requireNonNull(command, "command must not be null");
    var existing = holds.findByIdempotencyKey(command.idempotencyKey());
    if (existing.isPresent()) {
      return existing.orElseThrow();
    }
    Appointment appointment =
        appointments
            .findById(command.appointmentId())
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Appointment not found: " + command.appointmentId()));
    Instant expiry = clock.instant().plus(holdDuration);
    return holds.save(
        new AppointmentHold(
            UUID.randomUUID(), appointment.getId(), expiry, command.idempotencyKey()));
  }
}
