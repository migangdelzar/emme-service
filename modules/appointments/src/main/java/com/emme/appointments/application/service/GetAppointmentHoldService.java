package com.emme.appointments.application.service;

import com.emme.ai.contracts.appointment.AppointmentHold;
import com.emme.appointments.api.usecase.GetAppointmentHoldUseCase;
import com.emme.appointments.application.port.out.AppointmentHoldRepository;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Application service for tenant-local appointment hold retrieval. */
@Service
@Transactional(readOnly = true)
public class GetAppointmentHoldService implements GetAppointmentHoldUseCase {

  private final AppointmentHoldRepository repository;

  public GetAppointmentHoldService(AppointmentHoldRepository repository) {
    this.repository = Objects.requireNonNull(repository, "repository must not be null");
  }

  @Override
  public Optional<AppointmentHold> get(UUID holdId) {
    return repository.findById(Objects.requireNonNull(holdId, "holdId must not be null"));
  }
}
