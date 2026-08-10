package com.emme.salon.domain.model;

import java.time.LocalTime;
import java.util.Objects;
import java.util.UUID;

/** Domain representation of a tenant's opening interval for one day. */
public final class OperatingHours {

  private final UUID id;
  private final UUID tenantId;
  private final DayOfWeek dayOfWeek;
  private LocalTime opensAt;
  private LocalTime closesAt;
  private boolean active;

  public OperatingHours(UUID tenantId, DayOfWeek dayOfWeek, LocalTime opensAt, LocalTime closesAt) {
    this(null, tenantId, dayOfWeek, opensAt, closesAt, true);
  }

  private OperatingHours(
      UUID id,
      UUID tenantId,
      DayOfWeek dayOfWeek,
      LocalTime opensAt,
      LocalTime closesAt,
      boolean active) {
    this.id = id;
    this.tenantId = Objects.requireNonNull(tenantId, "tenantId must not be null");
    this.dayOfWeek = Objects.requireNonNull(dayOfWeek, "dayOfWeek must not be null");
    updateInterval(opensAt, closesAt);
    this.active = active;
  }

  public static OperatingHours reconstitute(
      UUID id,
      UUID tenantId,
      DayOfWeek dayOfWeek,
      LocalTime opensAt,
      LocalTime closesAt,
      boolean active) {
    return new OperatingHours(id, tenantId, dayOfWeek, opensAt, closesAt, active);
  }

  public void update(LocalTime opensAt, LocalTime closesAt, boolean active) {
    updateInterval(opensAt, closesAt);
    this.active = active;
  }

  private void updateInterval(LocalTime opensAt, LocalTime closesAt) {
    this.opensAt = Objects.requireNonNull(opensAt, "opensAt must not be null");
    this.closesAt = Objects.requireNonNull(closesAt, "closesAt must not be null");
    if (!opensAt.isBefore(closesAt)) {
      throw new IllegalArgumentException("opensAt must be before closesAt");
    }
  }

  public UUID getId() {
    return id;
  }

  public UUID getTenantId() {
    return tenantId;
  }

  public DayOfWeek getDayOfWeek() {
    return dayOfWeek;
  }

  public LocalTime getOpensAt() {
    return opensAt;
  }

  public LocalTime getClosesAt() {
    return closesAt;
  }

  public boolean isActive() {
    return active;
  }
}
