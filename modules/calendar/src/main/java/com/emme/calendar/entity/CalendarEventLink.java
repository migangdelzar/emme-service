package com.emme.calendar.entity;

import com.emme.shared.TenantOwnedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "calendar_event_link")
public class CalendarEventLink extends TenantOwnedEntity {

  @Column(name = "appointment_id", nullable = false)
  private UUID appointmentId;

  @Enumerated(EnumType.STRING)
  @Column(name = "provider", nullable = false, length = 30)
  private CalendarProvider provider;

  @Column(name = "external_event_id", nullable = false, length = 150)
  private String externalEventId;

  @Column(name = "etag", length = 150)
  private String etag;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private CalendarEventLinkStatus status = CalendarEventLinkStatus.PENDING;

  protected CalendarEventLink() {}

  public CalendarEventLink(
      UUID tenantId, UUID appointmentId, CalendarProvider provider, String externalEventId) {
    super(tenantId);
    this.appointmentId = Objects.requireNonNull(appointmentId, "appointmentId must not be null");
    this.provider = Objects.requireNonNull(provider, "provider must not be null");
    this.externalEventId =
        Objects.requireNonNull(externalEventId, "externalEventId must not be null");
  }

  public UUID getAppointmentId() {
    return appointmentId;
  }

  public CalendarProvider getProvider() {
    return provider;
  }

  public String getExternalEventId() {
    return externalEventId;
  }

  public String getEtag() {
    return etag;
  }

  public void setEtag(String etag) {
    this.etag = etag;
  }

  public CalendarEventLinkStatus getStatus() {
    return status;
  }

  public void setStatus(CalendarEventLinkStatus status) {
    this.status = status;
  }

  /** Transition from PENDING to SYNCED (stub) */
  public void markSynced() {
    if (status != CalendarEventLinkStatus.PENDING) {
      throw new IllegalStateException("Cannot mark synced with status: " + status);
    }
    status = CalendarEventLinkStatus.SYNCED;
  }

  /** Transition to FAILED */
  public void markFailed() {
    status = CalendarEventLinkStatus.FAILED;
  }

  /** Transition to DELETED */
  public void markDeleted() {
    status = CalendarEventLinkStatus.DELETED;
  }
}
