package com.emme.appointments.application.service;

import com.emme.appointments.api.command.RescheduleAppointmentCommand;
import com.emme.appointments.api.result.AppointmentDetails;
import com.emme.appointments.api.usecase.RescheduleAuthorizedAppointmentUseCase;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Applies trusted actor authorization before delegating an appointment reschedule. */
@Service
@Transactional
public class RescheduleAuthorizedAppointmentService
    implements RescheduleAuthorizedAppointmentUseCase {

  private final RescheduleAppointmentService rescheduling;

  public RescheduleAuthorizedAppointmentService(RescheduleAppointmentService rescheduling) {
    this.rescheduling = Objects.requireNonNull(rescheduling, "rescheduling must not be null");
  }

  @Override
  public AppointmentDetails reschedule(RescheduleAppointmentCommand command) {
    return rescheduling.rescheduleWithAuthorization(command);
  }
}
