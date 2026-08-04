package com.emme.studio.application.service;

import com.emme.studio.api.event.AppointmentCreated;
import com.emme.studio.api.result.AppointmentDetails;
import com.emme.studio.api.usecase.CreateAppointmentUseCase;
import com.emme.studio.application.port.out.AppointmentCollisionPort;
import com.emme.studio.application.port.out.AppointmentEventPublisher;
import com.emme.studio.application.port.out.AppointmentRepository;
import com.emme.studio.application.port.out.ArtistRepository;
import com.emme.studio.application.port.out.CustomerRepository;
import com.emme.studio.application.port.out.ServiceRepository;
import com.emme.studio.domain.model.Appointment;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Application service for appointment creation. */
@Service
@Transactional
public class CreateAppointmentService implements CreateAppointmentUseCase {

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
  public AppointmentDetails create(
      UUID tenantId,
      UUID customerId,
      UUID serviceId,
      UUID artistId,
      Instant startsAt,
      Instant endsAt) {
    support.ensureReferences(customerId, serviceId, artistId);
    support.ensureAvailable(artistId, startsAt, endsAt);
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
