package com.emme.calendar.domain.model;

import java.util.Objects;
import java.util.UUID;

/** Pure Calendar domain state for the relationship between an appointment and an external event. */
public final class CalendarEventLink {

  private final UUID id;
  private final UUID tenantId;
  private final UUID appointmentId;
  private final CalendarProvider provider;
  private final String externalEventId;
  private String etag;
  private CalendarEventLinkStatus status;

  private CalendarEventLink(
      UUID id,
      UUID tenantId,
      UUID appointmentId,
      CalendarProvider provider,
      String externalEventId,
      String etag,
      CalendarEventLinkStatus status) {
    this.id = id;
    this.tenantId = Objects.requireNonNull(tenantId, "tenantId must not be null");
    this.appointmentId = Objects.requireNonNull(appointmentId, "appointmentId must not be null");
    this.provider = Objects.requireNonNull(provider, "provider must not be null");
    this.externalEventId =
        Objects.requireNonNull(externalEventId, "externalEventId must not be null");
    this.etag = etag;
    this.status = Objects.requireNonNull(status, "status must not be null");
  }

  public static CalendarEventLink pending(
      UUID tenantId, UUID appointmentId, CalendarProvider provider, String externalEventId) {
    return new CalendarEventLink(
        UUID.randomUUID(),
        tenantId,
        appointmentId,
        provider,
        externalEventId,
        null,
        CalendarEventLinkStatus.PENDING);
  }

  public static CalendarEventLink restore(
      UUID id,
      UUID tenantId,
      UUID appointmentId,
      CalendarProvider provider,
      String externalEventId,
      String etag,
      CalendarEventLinkStatus status) {
    return new CalendarEventLink(
        Objects.requireNonNull(id, "id must not be null"),
        tenantId,
        appointmentId,
        provider,
        externalEventId,
        etag,
        status);
  }

  public UUID id() {
    return id;
  }

  public UUID tenantId() {
    return tenantId;
  }

  public UUID appointmentId() {
    return appointmentId;
  }

  public CalendarProvider provider() {
    return provider;
  }

  public String externalEventId() {
    return externalEventId;
  }

  public String etag() {
    return etag;
  }

  public CalendarEventLinkStatus status() {
    return status;
  }

  public void markSynced(String newEtag) {
    if (status != CalendarEventLinkStatus.PENDING) {
      throw new IllegalStateException("Cannot mark synced with status: " + status);
    }
    etag = newEtag;
    status = CalendarEventLinkStatus.SYNCED;
  }

  public void markFailed() {
    status = CalendarEventLinkStatus.FAILED;
  }

  public void markDeleted() {
    status = CalendarEventLinkStatus.DELETED;
  }
}
