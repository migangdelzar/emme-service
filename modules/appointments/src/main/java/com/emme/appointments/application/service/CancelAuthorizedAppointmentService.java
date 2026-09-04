package com.emme.appointments.application.service;

import com.emme.appointments.api.command.CancelAppointmentCommand;
import com.emme.appointments.api.result.AppointmentDetails;
import com.emme.appointments.api.usecase.CancelAuthorizedAppointmentUseCase;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Applies trusted actor authorization before delegating an appointment cancellation. */
@Service
@Transactional
public class CancelAuthorizedAppointmentService implements CancelAuthorizedAppointmentUseCase {

  private final CancelAppointmentService cancellation;

  public CancelAuthorizedAppointmentService(CancelAppointmentService cancellation) {
    this.cancellation = Objects.requireNonNull(cancellation, "cancellation must not be null");
  }

  @Override
  public AppointmentDetails cancel(CancelAppointmentCommand command) {
    return cancellation.cancelWithAuthorization(command);
  }
}
