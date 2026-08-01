package com.emme.calendar.adapter.out.persistence.entity;

import com.emme.calendar.domain.model.CalendarProvider;
import com.emme.calendar.domain.model.CalendarSyncStatus;
import com.emme.shared.TenantOwnedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "calendar_sync_state")
public class CalendarSyncStateEntity extends TenantOwnedEntity {

  @Enumerated(EnumType.STRING)
  @Column(name = "provider", nullable = false, length = 30)
  private CalendarProvider provider;

  @Column(name = "sync_token", length = 255)
  private String syncToken;

  @Column(name = "last_synced_at")
  private Instant lastSyncedAt;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private CalendarSyncStatus status = CalendarSyncStatus.ACTIVE;

  protected CalendarSyncStateEntity() {}

  public CalendarSyncStateEntity(UUID tenantId, CalendarProvider provider) {
    super(tenantId);
    this.provider = Objects.requireNonNull(provider, "provider must not be null");
  }

  public static CalendarSyncStateEntity restore(
      UUID id,
      UUID tenantId,
      CalendarProvider provider,
      String syncToken,
      Instant lastSyncedAt,
      CalendarSyncStatus status) {
    CalendarSyncStateEntity entity = new CalendarSyncStateEntity(tenantId, provider);
    entity.setId(id);
    entity.syncToken = syncToken;
    entity.lastSyncedAt = lastSyncedAt;
    entity.status = status;
    return entity;
  }

  public CalendarProvider getProvider() {
    return provider;
  }

  public String getSyncToken() {
    return syncToken;
  }

  public void setSyncToken(String syncToken) {
    this.syncToken = syncToken;
  }

  public Instant getLastSyncedAt() {
    return lastSyncedAt;
  }

  public void setLastSyncedAt(Instant lastSyncedAt) {
    this.lastSyncedAt = lastSyncedAt;
  }

  public CalendarSyncStatus getStatus() {
    return status;
  }

  public void setStatus(CalendarSyncStatus status) {
    this.status = status;
  }

  /** Mark sync state as STALE */
  public void markStale() {
    if (status == CalendarSyncStatus.FAILED) {
      throw new IllegalStateException("Cannot mark failed sync as stale");
    }
    status = CalendarSyncStatus.STALE;
  }

  /** Mark sync state as FAILED */
  public void markFailed() {
    status = CalendarSyncStatus.FAILED;
  }
}
