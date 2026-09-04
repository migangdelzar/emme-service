package com.emme.appointments.application.service;

import com.emme.appointments.api.command.CreateAppointmentCommand;
import com.emme.appointments.api.result.AppointmentDetails;
import com.emme.appointments.api.usecase.BookAppointmentUseCase;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Applies trusted actor authorization before creating an appointment. */
@Service
@Transactional
public class BookAppointmentService implements BookAppointmentUseCase {

  private final CreateAppointmentService creation;

  public BookAppointmentService(CreateAppointmentService creation) {
    this.creation = Objects.requireNonNull(creation, "creation must not be null");
  }

  @Override
  public AppointmentDetails book(CreateAppointmentCommand command) {
    return creation.bookWithAuthorization(command);
  }
}
