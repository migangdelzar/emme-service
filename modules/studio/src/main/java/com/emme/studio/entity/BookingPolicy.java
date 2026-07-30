package com.emme.studio.entity;

import com.emme.shared.TenantOwnedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "booking_policy")
public class BookingPolicy extends TenantOwnedEntity {

  @Column(name = "min_notice_minutes", nullable = false)
  private int minNoticeMinutes = 60;

  @Column(name = "max_advance_days", nullable = false)
  private int maxAdvanceDays = 30;

  @Column(name = "cancellation_window_minutes", nullable = false)
  private int cancellationWindowMinutes = 120;

  @Column(name = "allow_overlap", nullable = false)
  private boolean allowOverlap = false;

  protected BookingPolicy() {}

  public BookingPolicy(
      UUID tenantId,
      int minNoticeMinutes,
      int maxAdvanceDays,
      int cancellationWindowMinutes,
      boolean allowOverlap) {
    super(tenantId);
    this.minNoticeMinutes = minNoticeMinutes;
    this.maxAdvanceDays = maxAdvanceDays;
    this.cancellationWindowMinutes = cancellationWindowMinutes;
    this.allowOverlap = allowOverlap;
  }

  public int getMinNoticeMinutes() {
    return minNoticeMinutes;
  }

  public void setMinNoticeMinutes(int minNoticeMinutes) {
    this.minNoticeMinutes = minNoticeMinutes;
  }

  public int getMaxAdvanceDays() {
    return maxAdvanceDays;
  }

  public void setMaxAdvanceDays(int maxAdvanceDays) {
    this.maxAdvanceDays = maxAdvanceDays;
  }

  public int getCancellationWindowMinutes() {
    return cancellationWindowMinutes;
  }

  public void setCancellationWindowMinutes(int cancellationWindowMinutes) {
    this.cancellationWindowMinutes = cancellationWindowMinutes;
  }

  public boolean isAllowOverlap() {
    return allowOverlap;
  }

  public void setAllowOverlap(boolean allowOverlap) {
    this.allowOverlap = allowOverlap;
  }
}
