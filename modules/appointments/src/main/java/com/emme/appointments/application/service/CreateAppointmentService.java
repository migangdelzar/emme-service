package com.emme.appointments.application.service;

import com.emme.appointments.api.command.CreateAppointmentCommand;
import com.emme.appointments.api.event.AppointmentCreated;
import com.emme.appointments.api.result.AppointmentDetails;
import com.emme.appointments.api.usecase.BookAppointmentUseCase;
import com.emme.appointments.api.usecase.CreateAppointmentUseCase;
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

/** Application service for appointment creation. */
@Service
@Transactional
public class CreateAppointmentService implements CreateAppointmentUseCase, BookAppointmentUseCase {

  private final AppointmentRepository repository;
  private final AppointmentEventPublisher eventPublisher;
  private final AppointmentApplicationSupport support;

  public CreateAppointmentService(
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
  public AppointmentDetails book(CreateAppointmentCommand command) {
    support.ensureActorCanBook(command.actor(), command.customerId(), command.confirmed());
    return create(
        command.actor().tenantId(),
        command.customerId(),
        command.serviceId(),
        command.artistId(),
        command.startsAt(),
        command.endsAt());
  }

  @Override
  public AppointmentDetails create(
      UUID tenantId,
      UUID customerId,
      UUID serviceId,
      UUID artistId,
      Instant startsAt,
      Instant endsAt) {
    support.ensureReferences(tenantId, customerId, serviceId, artistId);
    support.ensureAvailable(tenantId, artistId, startsAt, endsAt);
    Appointment saved =
        repository.save(
            new Appointment(tenantId, customerId, serviceId, artistId, startsAt, endsAt));
    eventPublisher.publish(
        new AppointmentCreated(
            UUID.randomUUID(),
            tenantId,
            saved.getId(),
            customerId,
            artistId,
            serviceId,
            startsAt,
            endsAt,
            Instant.now()));
    return support.toDetails(saved);
  }
}
