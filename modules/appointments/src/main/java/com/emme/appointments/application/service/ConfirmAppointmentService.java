package com.emme.appointments.application.service;

import com.emme.appointments.api.result.AppointmentDetails;
import com.emme.appointments.api.usecase.ConfirmAppointmentUseCase;
import com.emme.appointments.application.port.out.AppointmentCollisionPort;
import com.emme.appointments.application.port.out.AppointmentRepository;
import com.emme.services.application.port.out.ArtistRepository;
import com.emme.clients.application.port.out.CustomerRepository;
import com.emme.services.application.port.out.ServiceRepository;
import com.emme.appointments.domain.model.Appointment;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Application service for appointment confirmation. */
@Service
@Transactional
public class ConfirmAppointmentService implements ConfirmAppointmentUseCase {

  private final AppointmentRepository repository;
  private final AppointmentApplicationSupport support;

  public ConfirmAppointmentService(
      AppointmentRepository repository,
      AppointmentCollisionPort collisionPort,
      CustomerRepository customerRepository,
      ServiceRepository serviceRepository,
      ArtistRepository artistRepository) {
    this.repository = repository;
    this.support =
        new AppointmentApplicationSupport(
            repository, collisionPort, customerRepository, serviceRepository, artistRepository);
  }

  @Override
  public AppointmentDetails confirm(UUID id) {
    Appointment appointment = support.find(id);
    appointment.confirm();
    return support.toDetails(repository.save(appointment));
  }
}
