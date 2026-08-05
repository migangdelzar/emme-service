package com.emme.appointments.adapter.out.persistence.entity;

import com.emme.appointments.domain.model.AppointmentStatus;
import com.emme.appointments.domain.model.ExternalCalendarStatus;
import com.emme.shared.persistence.TenantOwnedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "appointment")
public class AppointmentEntity extends TenantOwnedEntity {

  @Column(name = "customer_id", nullable = false)
  private UUID customerId;

  @Column(name = "service_id", nullable = false)
  private UUID serviceId;

  @Column(name = "artist_id", nullable = false)
  private UUID artistId;

  @Column(name = "starts_at", nullable = false)
  private Instant startsAt;

  @Column(name = "ends_at", nullable = false)
  private Instant endsAt;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private AppointmentStatus status = AppointmentStatus.CONFIRMED;

  @Enumerated(EnumType.STRING)
  @Column(name = "external_calendar_status", nullable = false, length = 15)
  private ExternalCalendarStatus externalCalendarStatus = ExternalCalendarStatus.NOT_SYNCED;

  protected AppointmentEntity() {}

  public AppointmentEntity(
      UUID tenantId,
      UUID customerId,
      UUID serviceId,
      UUID artistId,
      Instant startsAt,
      Instant endsAt) {
    super(tenantId);
    this.customerId = Objects.requireNonNull(customerId, "customerId must not be null");
    this.serviceId = Objects.requireNonNull(serviceId, "serviceId must not be null");
    this.artistId = Objects.requireNonNull(artistId, "artistId must not be null");
    this.startsAt = Objects.requireNonNull(startsAt, "startsAt must not be null");
    this.endsAt = Objects.requireNonNull(endsAt, "endsAt must not be null");
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

  public void setStatus(AppointmentStatus status) {
    this.status = status;
  }

  public void setStartsAt(Instant startsAt) {
    this.startsAt = Objects.requireNonNull(startsAt, "startsAt must not be null");
  }

  public void setEndsAt(Instant endsAt) {
    this.endsAt = Objects.requireNonNull(endsAt, "endsAt must not be null");
  }

  public ExternalCalendarStatus getExternalCalendarStatus() {
    return externalCalendarStatus;
  }

  public void setExternalCalendarStatus(ExternalCalendarStatus externalCalendarStatus) {
    this.externalCalendarStatus = externalCalendarStatus;
  }
}
