package com.emme.appointments.application.service;

import com.emme.appointments.api.command.CancelAppointmentCommand;
import com.emme.appointments.api.event.AppointmentCancelled;
import com.emme.appointments.api.result.AppointmentDetails;
import com.emme.appointments.api.usecase.CancelAppointmentUseCase;
import com.emme.appointments.api.usecase.CancelAuthorizedAppointmentUseCase;
import com.emme.appointments.application.port.out.AppointmentCollisionPort;
import com.emme.appointments.application.port.out.AppointmentEventPublisher;
import com.emme.appointments.application.port.out.AppointmentRepository;
import com.emme.appointments.domain.model.Appointment;
import com.emme.clients.application.port.out.CustomerRepository;
import com.emme.services.application.port.out.ArtistRepository;
import com.emme.services.application.port.out.ServiceRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Application service for appointment cancellation. */
@Service
@Transactional
public class CancelAppointmentService
    implements CancelAppointmentUseCase, CancelAuthorizedAppointmentUseCase {

  private final AppointmentRepository repository;
  private final AppointmentEventPublisher eventPublisher;
  private final AppointmentApplicationSupport support;

  public CancelAppointmentService(
      AppointmentRepository repository,
      AppointmentCollisionPort collisionPort,
      CustomerRepository customerRepository,
      ServiceRepository serviceRepository,
      ArtistRepository artistRepository,
      AppointmentEventPublisher eventPublisher) {
    this.repository = repository;
    this.eventPublisher = eventPublisher;
    this.support =
        new AppointmentApplicationSupport(
            repository, collisionPort, customerRepository, serviceRepository, artistRepository);
  }

  @Override
  public AppointmentDetails cancel(CancelAppointmentCommand command) {
    if (!command.confirmed()) throw new SecurityException("User confirmation is required");
    Appointment appointment = support.authorize(command.actor(), command.appointmentId());
    support.ensureMutable(appointment);
    return cancel(appointment.getId());
  }

  @Override
  public AppointmentDetails cancel(UUID id) {
    Appointment saved = support.find(id);
    saved.cancel();
    saved = repository.save(saved);
    eventPublisher.publish(
        new AppointmentCancelled(
            UUID.randomUUID(), saved.getTenantId(), saved.getId(), Instant.now()));
    return support.toDetails(saved);
  }
}
