package com.emme.studio.application.service;

import com.emme.studio.api.event.AppointmentCancelledEvent;
import com.emme.studio.api.event.AppointmentCreatedEvent;
import com.emme.studio.api.event.AppointmentRescheduledEvent;
import com.emme.studio.application.port.out.AppointmentCollisionPort;
import com.emme.studio.application.port.out.AppointmentEventPublisher;
import com.emme.studio.application.port.out.AppointmentRepository;
import com.emme.studio.application.port.out.ArtistRepository;
import com.emme.studio.application.port.out.CustomerRepository;
import com.emme.studio.application.port.out.ServiceRepository;
import com.emme.studio.application.result.AppointmentView;
import com.emme.studio.domain.model.Appointment;
import com.emme.studio.domain.model.Artist;
import com.emme.studio.domain.model.Customer;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AppointmentService {

  private final AppointmentRepository appointmentRepository;
  private final AppointmentCollisionPort collisionPort;
  private final CustomerRepository customerRepository;
  private final ServiceRepository serviceRepository;
  private final ArtistRepository artistRepository;
  private final AppointmentEventPublisher eventPublisher;

  public AppointmentService(
      AppointmentRepository appointmentRepository,
      AppointmentCollisionPort collisionPort,
      CustomerRepository customerRepository,
      ServiceRepository serviceRepository,
      ArtistRepository artistRepository,
      AppointmentEventPublisher eventPublisher) {
    this.appointmentRepository = appointmentRepository;
    this.collisionPort = collisionPort;
    this.customerRepository = customerRepository;
    this.serviceRepository = serviceRepository;
    this.artistRepository = artistRepository;
    this.eventPublisher = eventPublisher;
  }

  public AppointmentView create(
      UUID tenantId,
      UUID customerId,
      UUID serviceId,
      UUID artistId,
      Instant startsAt,
      Instant endsAt) {
    ensureReferences(customerId, serviceId, artistId);
    ensureAvailable(artistId, startsAt, endsAt);

    Appointment saved =
        appointmentRepository.save(
            new Appointment(tenantId, customerId, serviceId, artistId, startsAt, endsAt));
    eventPublisher.publish(
        new AppointmentCreatedEvent(
            UUID.randomUUID(),
            tenantId,
            saved.getId(),
            customerId,
            artistId,
            serviceId,
            startsAt,
            endsAt,
            Instant.now()));
    return toView(saved);
  }

  public AppointmentView reschedule(UUID id, Instant newStartsAt, Instant newEndsAt) {
    Appointment appointment = find(id);
    ensureAvailable(appointment.getArtistId(), newStartsAt, newEndsAt);
    Instant oldStartsAt = appointment.getStartsAt();
    Instant oldEndsAt = appointment.getEndsAt();

    appointment.reschedule(newStartsAt, newEndsAt);
    Appointment saved = appointmentRepository.save(appointment);
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
    return toView(saved);
  }

  public AppointmentView cancel(UUID id) {
    Appointment appointment = find(id);
    appointment.cancel();
    Appointment saved = appointmentRepository.save(appointment);
    eventPublisher.publish(
        new AppointmentCancelledEvent(
            UUID.randomUUID(), saved.getTenantId(), saved.getId(), Instant.now()));
    return toView(saved);
  }

  public AppointmentView confirm(UUID id) {
    Appointment appointment = find(id);
    appointment.confirm();
    return toView(appointmentRepository.save(appointment));
  }

  public AppointmentView start(UUID id) {
    Appointment appointment = find(id);
    appointment.start();
    return toView(appointmentRepository.save(appointment));
  }

  public AppointmentView complete(UUID id) {
    Appointment appointment = find(id);
    appointment.complete();
    return toView(appointmentRepository.save(appointment));
  }

  public AppointmentView noShow(UUID id) {
    Appointment appointment = find(id);
    appointment.noShow();
    return toView(appointmentRepository.save(appointment));
  }

  @Transactional(readOnly = true)
  public Optional<AppointmentView> findById(UUID id) {
    return appointmentRepository.findById(id).map(this::toView);
  }

  @Transactional(readOnly = true)
  public List<AppointmentView> findByTenantAndDate(UUID tenantId, LocalDate date) {
    ZoneId zone = ZoneId.of("America/Mexico_City");
    Instant dayStart = ZonedDateTime.of(date.atStartOfDay(), zone).toInstant();
    Instant dayEnd = ZonedDateTime.of(date.plusDays(1).atStartOfDay(), zone).toInstant();
    return appointmentRepository
        .findByTenantIdAndStartsAtBetween(tenantId, dayStart, dayEnd)
        .stream()
        .map(this::toView)
        .toList();
  }

  private Appointment find(UUID id) {
    return appointmentRepository
        .findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Appointment not found: " + id));
  }

  private void ensureReferences(UUID customerId, UUID serviceId, UUID artistId) {
    if (customerRepository.findById(customerId).isEmpty()) {
      throw new IllegalArgumentException("Customer not found: " + customerId);
    }
    if (serviceRepository.findById(serviceId).isEmpty()) {
      throw new IllegalArgumentException("Service not found: " + serviceId);
    }
    if (artistRepository.findById(artistId).isEmpty()) {
      throw new IllegalArgumentException("Artist not found: " + artistId);
    }
  }

  private void ensureAvailable(UUID artistId, Instant startsAt, Instant endsAt) {
    if (collisionPort.hasCollision(artistId, startsAt, endsAt)) {
      throw new IllegalStateException(
          "Slot conflict: artist "
              + artistId
              + " already has a confirmed appointment in this time range");
    }
  }

  private AppointmentView toView(Appointment appointment) {
    String customerName =
        customerRepository
            .findById(appointment.getCustomerId())
            .map(Customer::getName)
            .orElse(null);
    String serviceName =
        serviceRepository
            .findById(appointment.getServiceId())
            .map(com.emme.studio.domain.model.Service::getName)
            .orElse(null);
    String artistName =
        artistRepository.findById(appointment.getArtistId()).map(Artist::getName).orElse(null);
    return AppointmentView.from(appointment, customerName, serviceName, artistName);
  }
}
