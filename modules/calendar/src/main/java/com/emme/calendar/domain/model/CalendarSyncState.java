package com.emme.calendar.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Pure Calendar domain state for incremental provider synchronization. */
public final class CalendarSyncState {

  private final UUID id;
  private final UUID tenantId;
  private final CalendarProvider provider;
  private String syncToken;
  private Instant lastSyncedAt;
  private CalendarSyncStatus status;

  private CalendarSyncState(
      UUID id,
      UUID tenantId,
      CalendarProvider provider,
      String syncToken,
      Instant lastSyncedAt,
      CalendarSyncStatus status) {
    this.id = id;
    this.tenantId = Objects.requireNonNull(tenantId, "tenantId must not be null");
    this.provider = Objects.requireNonNull(provider, "provider must not be null");
    this.syncToken = syncToken;
    this.lastSyncedAt = lastSyncedAt;
    this.status = Objects.requireNonNull(status, "status must not be null");
  }

  public static CalendarSyncState active(UUID tenantId, CalendarProvider provider) {
    return new CalendarSyncState(
        UUID.randomUUID(), tenantId, provider, null, null, CalendarSyncStatus.ACTIVE);
  }

  public static CalendarSyncState restore(
      UUID id,
      UUID tenantId,
      CalendarProvider provider,
      String syncToken,
      Instant lastSyncedAt,
      CalendarSyncStatus status) {
    return new CalendarSyncState(
        Objects.requireNonNull(id, "id must not be null"),
        tenantId,
        provider,
        syncToken,
        lastSyncedAt,
        status);
  }

  public UUID id() {
    return id;
  }

  public UUID tenantId() {
    return tenantId;
  }

  public CalendarProvider provider() {
    return provider;
  }

  public String syncToken() {
    return syncToken;
  }

  public Instant lastSyncedAt() {
    return lastSyncedAt;
  }

  public CalendarSyncStatus status() {
    return status;
  }

  public void updateSync(String newSyncToken, Instant syncedAt) {
    syncToken = newSyncToken;
    lastSyncedAt = syncedAt;
    status = CalendarSyncStatus.ACTIVE;
  }

  public void markStale() {
    if (status == CalendarSyncStatus.FAILED) {
      throw new IllegalStateException("Cannot mark failed sync as stale");
    }
    status = CalendarSyncStatus.STALE;
  }

  public void markFailed() {
    status = CalendarSyncStatus.FAILED;
  }
}
