package com.emme.studio.application.service;

import com.emme.studio.api.event.AppointmentCancelledEvent;
import com.emme.studio.api.event.AppointmentCreatedEvent;
import com.emme.studio.api.event.AppointmentRescheduledEvent;
import com.emme.studio.entity.Appointment;
import com.emme.studio.entity.AppointmentRepository;
import com.emme.studio.entity.AppointmentStatus;
import com.emme.studio.entity.Artist;
import com.emme.studio.entity.ArtistRepository;
import com.emme.studio.entity.Customer;
import com.emme.studio.entity.CustomerRepository;
import com.emme.studio.entity.Service;
import com.emme.studio.entity.ServiceRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

@org.springframework.stereotype.Service
@Transactional
public class AppointmentService {

  private final AppointmentRepository appointmentRepo;
  private final CollisionDetector collisionDetector;
  private final CustomerRepository customerRepo;
  private final ServiceRepository serviceRepo;
  private final ArtistRepository artistRepo;
  private final ApplicationEventPublisher eventPublisher;

  public AppointmentService(
      AppointmentRepository appointmentRepo,
      CollisionDetector collisionDetector,
      CustomerRepository customerRepo,
      ServiceRepository serviceRepo,
      ArtistRepository artistRepo,
      ApplicationEventPublisher eventPublisher) {
    this.appointmentRepo = appointmentRepo;
    this.collisionDetector = collisionDetector;
    this.customerRepo = customerRepo;
    this.serviceRepo = serviceRepo;
    this.artistRepo = artistRepo;
    this.eventPublisher = eventPublisher;
  }

  public Appointment create(
      UUID tenantId,
      UUID customerId,
      UUID serviceId,
      UUID artistId,
      Instant startsAt,
      Instant endsAt) {
    if (collisionDetector.hasCollision(artistId, startsAt, endsAt)) {
      throw new IllegalStateException(
          "Slot conflict: artist "
              + artistId
              + " already has a confirmed appointment in this time range");
    }

    Customer customer = customerRepo.getReferenceById(customerId);
    Service service = serviceRepo.getReferenceById(serviceId);
    Artist artist = artistRepo.getReferenceById(artistId);

    Appointment appointment =
        new Appointment(tenantId, customer, service, artist, startsAt, endsAt);
    appointment.setStatus(AppointmentStatus.CONFIRMED);
    Appointment saved = appointmentRepo.save(appointment);

    eventPublisher.publishEvent(
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

    return saved;
  }

  public Appointment reschedule(UUID id, Instant newStartsAt, Instant newEndsAt) {
    Appointment appointment =
        appointmentRepo
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Appointment not found: " + id));

    if (collisionDetector.hasCollision(appointment.getArtist().getId(), newStartsAt, newEndsAt)) {
      throw new IllegalStateException("New slot conflicts with existing appointments");
    }

    Instant oldStartsAt = appointment.getStartsAt();
    Instant oldEndsAt = appointment.getEndsAt();

    appointment.setStartsAt(newStartsAt);
    appointment.setEndsAt(newEndsAt);
    Appointment saved = appointmentRepo.save(appointment);

    eventPublisher.publishEvent(
        new AppointmentRescheduledEvent(
            UUID.randomUUID(),
            appointment.getTenantId(),
            saved.getId(),
            oldStartsAt,
            oldEndsAt,
            newStartsAt,
            newEndsAt,
            Instant.now()));

    return saved;
  }

  public Appointment cancel(UUID id) {
    Appointment appointment =
        appointmentRepo
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Appointment not found: " + id));
    appointment.setStatus(AppointmentStatus.CANCELLED);
    Appointment saved = appointmentRepo.save(appointment);

    eventPublisher.publishEvent(
        new AppointmentCancelledEvent(
            UUID.randomUUID(), appointment.getTenantId(), saved.getId(), Instant.now()));

    return saved;
  }

  public Appointment confirm(UUID id) {
    Appointment appointment =
        appointmentRepo
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Appointment not found: " + id));
    if (appointment.getStatus() != AppointmentStatus.DRAFT) {
      throw new IllegalStateException(
          "Only DRAFT appointments can be confirmed. Current status: " + appointment.getStatus());
    }
    appointment.setStatus(AppointmentStatus.CONFIRMED);
    return appointmentRepo.save(appointment);
  }

  public Appointment start(UUID id) {
    Appointment appointment =
        appointmentRepo
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Appointment not found: " + id));
    if (appointment.getStatus() != AppointmentStatus.CONFIRMED) {
      throw new IllegalStateException(
          "Only CONFIRMED appointments can be started. Current status: " + appointment.getStatus());
    }
    appointment.setStatus(AppointmentStatus.IN_PROGRESS);
    return appointmentRepo.save(appointment);
  }

  public Appointment complete(UUID id) {
    Appointment appointment =
        appointmentRepo
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Appointment not found: " + id));
    if (appointment.getStatus() != AppointmentStatus.IN_PROGRESS) {
      throw new IllegalStateException(
          "Only IN_PROGRESS appointments can be completed. Current status: "
              + appointment.getStatus());
    }
    appointment.setStatus(AppointmentStatus.COMPLETED);
    return appointmentRepo.save(appointment);
  }

  public Appointment noShow(UUID id) {
    Appointment appointment =
        appointmentRepo
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Appointment not found: " + id));
    if (appointment.getStatus() != AppointmentStatus.CONFIRMED) {
      throw new IllegalStateException(
          "Only CONFIRMED appointments can be marked as no-show. Current status: "
              + appointment.getStatus());
    }
    appointment.setStatus(AppointmentStatus.NO_SHOW);
    return appointmentRepo.save(appointment);
  }

  @Transactional(readOnly = true)
  public Optional<Appointment> findById(UUID id) {
    return appointmentRepo.findById(id);
  }

  @Transactional(readOnly = true)
  public List<Appointment> findByTenantAndDate(UUID tenantId, LocalDate date) {
    ZoneId zone = ZoneId.of("America/Mexico_City");
    Instant dayStart = ZonedDateTime.of(date.atStartOfDay(), zone).toInstant();
    Instant dayEnd = ZonedDateTime.of(date.plusDays(1).atStartOfDay(), zone).toInstant();
    return appointmentRepo.findByTenantIdAndStartsAtBetween(tenantId, dayStart, dayEnd);
  }
}
