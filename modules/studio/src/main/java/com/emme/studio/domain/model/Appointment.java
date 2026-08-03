package com.emme.studio.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Business aggregate for a scheduled studio appointment. */
public final class Appointment {

  private final UUID tenantId;
  private final UUID customerId;
  private final UUID serviceId;
  private final UUID artistId;
  private UUID id;
  private Instant startsAt;
  private Instant endsAt;
  private AppointmentStatus status;
  private ExternalCalendarStatus externalCalendarStatus;

  public Appointment(
      UUID tenantId,
      UUID customerId,
      UUID serviceId,
      UUID artistId,
      Instant startsAt,
      Instant endsAt) {
    this.tenantId = require(tenantId, "tenantId");
    this.customerId = require(customerId, "customerId");
    this.serviceId = require(serviceId, "serviceId");
    this.artistId = require(artistId, "artistId");
    updateInterval(startsAt, endsAt);
    this.status = AppointmentStatus.CONFIRMED;
    this.externalCalendarStatus = ExternalCalendarStatus.NOT_SYNCED;
  }

  private Appointment(
      UUID id,
      UUID tenantId,
      UUID customerId,
      UUID serviceId,
      UUID artistId,
      Instant startsAt,
      Instant endsAt,
      AppointmentStatus status,
      ExternalCalendarStatus externalCalendarStatus) {
    this.id = require(id, "id");
    this.tenantId = require(tenantId, "tenantId");
    this.customerId = require(customerId, "customerId");
    this.serviceId = require(serviceId, "serviceId");
    this.artistId = require(artistId, "artistId");
    updateInterval(startsAt, endsAt);
    this.status = require(status, "status");
    this.externalCalendarStatus = require(externalCalendarStatus, "externalCalendarStatus");
  }

  public static Appointment reconstitute(
      UUID id,
      UUID tenantId,
      UUID customerId,
      UUID serviceId,
      UUID artistId,
      Instant startsAt,
      Instant endsAt,
      AppointmentStatus status,
      ExternalCalendarStatus externalCalendarStatus) {
    return new Appointment(
        id,
        tenantId,
        customerId,
        serviceId,
        artistId,
        startsAt,
        endsAt,
        status,
        externalCalendarStatus);
  }

  public void reschedule(Instant newStartsAt, Instant newEndsAt) {
    updateInterval(newStartsAt, newEndsAt);
  }

  public void cancel() {
    status = AppointmentStatus.CANCELLED;
  }

  public void confirm() {
    transition(AppointmentStatus.DRAFT, AppointmentStatus.CONFIRMED);
  }

  public void start() {
    transition(AppointmentStatus.CONFIRMED, AppointmentStatus.IN_PROGRESS);
  }

  public void complete() {
    transition(AppointmentStatus.IN_PROGRESS, AppointmentStatus.COMPLETED);
  }

  public void noShow() {
    transition(AppointmentStatus.CONFIRMED, AppointmentStatus.NO_SHOW);
  }

  private void transition(AppointmentStatus expected, AppointmentStatus target) {
    if (status != expected) {
      throw new IllegalStateException(
          "Only "
              + expected
              + " appointments can transition to "
              + target
              + ". Current status: "
              + status);
    }
    status = target;
  }

  private void updateInterval(Instant start, Instant end) {
    this.startsAt = require(start, "startsAt");
    this.endsAt = require(end, "endsAt");
    if (!startsAt.isBefore(endsAt)) {
      throw new IllegalArgumentException("startsAt must be before endsAt");
    }
  }

  private static <T> T require(T value, String name) {
    return Objects.requireNonNull(value, name + " must not be null");
  }

  public UUID getId() {
    return id;
  }

  public UUID getTenantId() {
    return tenantId;
  }

  public UUID getCustomerId() {
    return customerId;
  }

  public UUID getServiceId() {
    return serviceId;
  }

  public UUID getArtistId() {
    return artistId;
  }

  public Instant getStartsAt() {
    return startsAt;
  }

  public Instant getEndsAt() {
    return endsAt;
  }

  public AppointmentStatus getStatus() {
    return status;
  }

  public ExternalCalendarStatus getExternalCalendarStatus() {
    return externalCalendarStatus;
  }

  public void setExternalCalendarStatus(ExternalCalendarStatus externalCalendarStatus) {
    this.externalCalendarStatus = require(externalCalendarStatus, "externalCalendarStatus");
  }
}
