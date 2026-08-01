package com.emme.studio.application.service;

import com.emme.studio.adapter.out.persistence.entity.AppointmentEntity;
import com.emme.studio.adapter.out.persistence.entity.ArtistEntity;
import com.emme.studio.adapter.out.persistence.entity.CustomerEntity;
import com.emme.studio.adapter.out.persistence.entity.ServiceEntity;
import com.emme.studio.adapter.out.persistence.repository.SpringDataAppointmentRepository;
import com.emme.studio.adapter.out.persistence.repository.SpringDataArtistRepository;
import com.emme.studio.adapter.out.persistence.repository.SpringDataCustomerRepository;
import com.emme.studio.adapter.out.persistence.repository.SpringDataServiceRepository;
import com.emme.studio.api.event.AppointmentCancelledEvent;
import com.emme.studio.api.event.AppointmentCreatedEvent;
import com.emme.studio.api.event.AppointmentRescheduledEvent;
import com.emme.studio.domain.model.AppointmentStatus;
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

  private final SpringDataAppointmentRepository appointmentRepo;
  private final CollisionDetector collisionDetector;
  private final SpringDataCustomerRepository customerRepo;
  private final SpringDataServiceRepository serviceRepo;
  private final SpringDataArtistRepository artistRepo;
  private final ApplicationEventPublisher eventPublisher;

  public AppointmentService(
      SpringDataAppointmentRepository appointmentRepo,
      CollisionDetector collisionDetector,
      SpringDataCustomerRepository customerRepo,
      SpringDataServiceRepository serviceRepo,
      SpringDataArtistRepository artistRepo,
      ApplicationEventPublisher eventPublisher) {
    this.appointmentRepo = appointmentRepo;
    this.collisionDetector = collisionDetector;
    this.customerRepo = customerRepo;
    this.serviceRepo = serviceRepo;
    this.artistRepo = artistRepo;
    this.eventPublisher = eventPublisher;
  }

  public AppointmentEntity create(
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

    CustomerEntity customer = customerRepo.getReferenceById(customerId);
    ServiceEntity service = serviceRepo.getReferenceById(serviceId);
    ArtistEntity artist = artistRepo.getReferenceById(artistId);

    AppointmentEntity appointment =
        new AppointmentEntity(tenantId, customer, service, artist, startsAt, endsAt);
    appointment.setStatus(AppointmentStatus.CONFIRMED);
    AppointmentEntity saved = appointmentRepo.save(appointment);

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

  public AppointmentEntity reschedule(UUID id, Instant newStartsAt, Instant newEndsAt) {
    AppointmentEntity appointment =
        appointmentRepo
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("AppointmentEntity not found: " + id));

    if (collisionDetector.hasCollision(appointment.getArtist().getId(), newStartsAt, newEndsAt)) {
      throw new IllegalStateException("New slot conflicts with existing appointments");
    }

    Instant oldStartsAt = appointment.getStartsAt();
    Instant oldEndsAt = appointment.getEndsAt();

    appointment.setStartsAt(newStartsAt);
    appointment.setEndsAt(newEndsAt);
    AppointmentEntity saved = appointmentRepo.save(appointment);

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

  public AppointmentEntity cancel(UUID id) {
    AppointmentEntity appointment =
        appointmentRepo
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("AppointmentEntity not found: " + id));
    appointment.setStatus(AppointmentStatus.CANCELLED);
    AppointmentEntity saved = appointmentRepo.save(appointment);

    eventPublisher.publishEvent(
        new AppointmentCancelledEvent(
            UUID.randomUUID(), appointment.getTenantId(), saved.getId(), Instant.now()));

    return saved;
  }

  public AppointmentEntity confirm(UUID id) {
    AppointmentEntity appointment =
        appointmentRepo
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("AppointmentEntity not found: " + id));
    if (appointment.getStatus() != AppointmentStatus.DRAFT) {
      throw new IllegalStateException(
          "Only DRAFT appointments can be confirmed. Current status: " + appointment.getStatus());
    }
    appointment.setStatus(AppointmentStatus.CONFIRMED);
    return appointmentRepo.save(appointment);
  }

  public AppointmentEntity start(UUID id) {
    AppointmentEntity appointment =
        appointmentRepo
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("AppointmentEntity not found: " + id));
    if (appointment.getStatus() != AppointmentStatus.CONFIRMED) {
      throw new IllegalStateException(
          "Only CONFIRMED appointments can be started. Current status: " + appointment.getStatus());
    }
    appointment.setStatus(AppointmentStatus.IN_PROGRESS);
    return appointmentRepo.save(appointment);
  }

  public AppointmentEntity complete(UUID id) {
    AppointmentEntity appointment =
        appointmentRepo
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("AppointmentEntity not found: " + id));
    if (appointment.getStatus() != AppointmentStatus.IN_PROGRESS) {
      throw new IllegalStateException(
          "Only IN_PROGRESS appointments can be completed. Current status: "
              + appointment.getStatus());
    }
    appointment.setStatus(AppointmentStatus.COMPLETED);
    return appointmentRepo.save(appointment);
  }

  public AppointmentEntity noShow(UUID id) {
    AppointmentEntity appointment =
        appointmentRepo
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("AppointmentEntity not found: " + id));
    if (appointment.getStatus() != AppointmentStatus.CONFIRMED) {
      throw new IllegalStateException(
          "Only CONFIRMED appointments can be marked as no-show. Current status: "
              + appointment.getStatus());
    }
    appointment.setStatus(AppointmentStatus.NO_SHOW);
    return appointmentRepo.save(appointment);
  }

  @Transactional(readOnly = true)
  public Optional<AppointmentEntity> findById(UUID id) {
    return appointmentRepo.findById(id);
  }

  @Transactional(readOnly = true)
  public List<AppointmentEntity> findByTenantAndDate(UUID tenantId, LocalDate date) {
    ZoneId zone = ZoneId.of("America/Mexico_City");
    Instant dayStart = ZonedDateTime.of(date.atStartOfDay(), zone).toInstant();
    Instant dayEnd = ZonedDateTime.of(date.plusDays(1).atStartOfDay(), zone).toInstant();
    return appointmentRepo.findByTenantIdAndStartsAtBetween(tenantId, dayStart, dayEnd);
  }
}
