package com.emme.salon.adapter.out.persistence.entity;

import com.emme.shared.persistence.TenantOwnedEntity;
import com.emme.salon.domain.model.DayOfWeek;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
    name = "operating_hours",
    uniqueConstraints = {@UniqueConstraint(columnNames = {"tenant_id", "day_of_week"})})
public class OperatingHoursEntity extends TenantOwnedEntity {

  @Enumerated(EnumType.STRING)
  @Column(name = "day_of_week", nullable = false, length = 3)
  private DayOfWeek dayOfWeek;

  @Column(name = "opens_at", nullable = false)
  private LocalTime opensAt;

  @Column(name = "closes_at", nullable = false)
  private LocalTime closesAt;

  @Column(name = "active", nullable = false)
  private boolean active = true;

  protected OperatingHoursEntity() {}

  public OperatingHoursEntity(
      UUID tenantId, DayOfWeek dayOfWeek, LocalTime opensAt, LocalTime closesAt) {
    super(tenantId);
    this.dayOfWeek = Objects.requireNonNull(dayOfWeek, "dayOfWeek must not be null");
    this.opensAt = Objects.requireNonNull(opensAt, "opensAt must not be null");
    this.closesAt = Objects.requireNonNull(closesAt, "closesAt must not be null");
  }

  public DayOfWeek getDayOfWeek() {
    return dayOfWeek;
  }

  public LocalTime getOpensAt() {
    return opensAt;
  }

  public void setOpensAt(LocalTime opensAt) {
    this.opensAt = opensAt;
  }

  public LocalTime getClosesAt() {
    return closesAt;
  }

  public void setClosesAt(LocalTime closesAt) {
    this.closesAt = closesAt;
  }

  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
  }
}
