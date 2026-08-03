package com.emme.studio.application.service;

import com.emme.studio.api.event.AppointmentRescheduledEvent;
import com.emme.studio.api.result.AppointmentDetails;
import com.emme.studio.api.usecase.RescheduleAppointmentUseCase;
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

/** Application service for appointment rescheduling. */
@Service
@Transactional
public class RescheduleAppointmentService implements RescheduleAppointmentUseCase {

  private final AppointmentRepository repository;
  private final AppointmentEventPublisher eventPublisher;
  private final AppointmentApplicationSupport support;

  public RescheduleAppointmentService(
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
  public AppointmentDetails reschedule(UUID id, Instant newStartsAt, Instant newEndsAt) {
    Appointment appointment = support.find(id);
    support.ensureAvailable(appointment.getArtistId(), newStartsAt, newEndsAt);
    Instant oldStartsAt = appointment.getStartsAt();
    Instant oldEndsAt = appointment.getEndsAt();
    appointment.reschedule(newStartsAt, newEndsAt);
    Appointment saved = repository.save(appointment);
    eventPublisher.publish(
        new AppointmentRescheduledEvent(
            UUID.randomUUID(),
            saved.getTenantId(),
            saved.getId(),
            oldStartsAt,
            oldEndsAt,
            newStartsAt,
            newEndsAt,
            Instant.now()));
    return support.toView(saved);
  }
}
