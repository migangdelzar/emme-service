package com.emme.studio.application.service;

import com.emme.studio.api.event.AppointmentCancelledEvent;
import com.emme.studio.api.usecase.CancelAppointmentUseCase;
import com.emme.studio.application.port.out.AppointmentCollisionPort;
import com.emme.studio.application.port.out.AppointmentEventPublisher;
import com.emme.studio.application.port.out.AppointmentRepository;
import com.emme.studio.application.port.out.ArtistRepository;
import com.emme.studio.application.port.out.CustomerRepository;
import com.emme.studio.application.port.out.ServiceRepository;
import com.emme.studio.application.result.AppointmentView;
import com.emme.studio.domain.model.Appointment;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Application service for appointment cancellation. */
@Service
@Transactional
public class CancelAppointmentService implements CancelAppointmentUseCase {

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
  public AppointmentView cancel(UUID id) {
    Appointment saved = support.find(id);
    saved.cancel();
    saved = repository.save(saved);
    eventPublisher.publish(
        new AppointmentCancelledEvent(
            UUID.randomUUID(), saved.getTenantId(), saved.getId(), Instant.now()));
    return support.toView(saved);
  }
}
