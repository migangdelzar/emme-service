package com.emme.appointments.application.service;

import com.emme.appointments.api.event.AppointmentRescheduled;
import com.emme.appointments.api.result.AppointmentDetails;
import com.emme.appointments.api.usecase.RescheduleAppointmentUseCase;
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
        new AppointmentRescheduled(
            UUID.randomUUID(),
            saved.getTenantId(),
            saved.getId(),
            oldStartsAt,
            oldEndsAt,
            newStartsAt,
            newEndsAt,
            Instant.now()));
    return support.toDetails(saved);
  }
}
