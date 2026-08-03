package com.emme.studio.adapter.out.persistence.entity;

import com.emme.shared.persistence.TenantOwnedEntity;
import com.emme.studio.domain.model.AppointmentStatus;
import com.emme.studio.domain.model.ExternalCalendarStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "appointment")
public class AppointmentEntity extends TenantOwnedEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "customer_id",
      nullable = false,
      foreignKey = @ForeignKey(name = "fk_appointment_customer"))
  private CustomerEntity customer;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "service_id",
      nullable = false,
      foreignKey = @ForeignKey(name = "fk_appointment_service"))
  private ServiceEntity service;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "artist_id",
      nullable = false,
      foreignKey = @ForeignKey(name = "fk_appointment_artist"))
  private ArtistEntity artist;

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
      CustomerEntity customer,
      ServiceEntity service,
      ArtistEntity artist,
      Instant startsAt,
      Instant endsAt) {
    super(tenantId);
    this.customer = Objects.requireNonNull(customer, "customer must not be null");
    this.service = Objects.requireNonNull(service, "service must not be null");
    this.artist = Objects.requireNonNull(artist, "artist must not be null");
    this.startsAt = Objects.requireNonNull(startsAt, "startsAt must not be null");
    this.endsAt = Objects.requireNonNull(endsAt, "endsAt must not be null");
  }

  public CustomerEntity getCustomer() {
    return customer;
  }

  public ServiceEntity getService() {
    return service;
  }

  public ArtistEntity getArtist() {
    return artist;
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
