package com.emme.appointments.adapter.out.persistence.entity;

import com.emme.shared.persistence.TenantOwnedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Tenant-schema representation of a durable appointment hold. */
@Entity
@Table(name = "appointment_hold")
public class AppointmentHoldEntity extends TenantOwnedEntity {

  @Column(name = "appointment_id", nullable = false)
  private UUID appointmentId;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "idempotency_key", nullable = false, length = 200)
  private String idempotencyKey;

  protected AppointmentHoldEntity() {}

  @SuppressWarnings("this-escape")
  public AppointmentHoldEntity(
      UUID tenantId, UUID holdId, UUID appointmentId, Instant expiresAt, String idempotencyKey) {
    super(tenantId);
    setId(Objects.requireNonNull(holdId, "holdId must not be null"));
    this.appointmentId = Objects.requireNonNull(appointmentId, "appointmentId must not be null");
    this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt must not be null");
    this.idempotencyKey = requireText(idempotencyKey, "idempotencyKey");
  }

  public UUID getAppointmentId() {
    return appointmentId;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public String getIdempotencyKey() {
    return idempotencyKey;
  }

  public void setExpiresAt(Instant expiresAt) {
    this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt must not be null");
  }

  private static String requireText(String value, String field) {
    Objects.requireNonNull(value, field + " must not be null");
    if (value.isBlank()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
    return value;
  }
}
