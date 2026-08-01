package com.emme.studio.domain.model;

import java.util.UUID;

/** Tenant policy governing appointment notice, advance, cancellation, and overlap. */
public final class BookingPolicy {

  private final UUID id;
  private final UUID tenantId;
  private int minNoticeMinutes;
  private int maxAdvanceDays;
  private int cancellationWindowMinutes;
  private boolean allowOverlap;

  public BookingPolicy(
      UUID tenantId,
      int minNoticeMinutes,
      int maxAdvanceDays,
      int cancellationWindowMinutes,
      boolean allowOverlap) {
    this(null, tenantId, minNoticeMinutes, maxAdvanceDays, cancellationWindowMinutes, allowOverlap);
  }

  private BookingPolicy(
      UUID id,
      UUID tenantId,
      int minNoticeMinutes,
      int maxAdvanceDays,
      int cancellationWindowMinutes,
      boolean allowOverlap) {
    this.id = id;
    this.tenantId = tenantId;
    this.allowOverlap = allowOverlap;
    update(minNoticeMinutes, maxAdvanceDays, cancellationWindowMinutes, allowOverlap);
  }

  public static BookingPolicy reconstitute(
      UUID id,
      UUID tenantId,
      int minNoticeMinutes,
      int maxAdvanceDays,
      int cancellationWindowMinutes,
      boolean allowOverlap) {
    return new BookingPolicy(
        id, tenantId, minNoticeMinutes, maxAdvanceDays, cancellationWindowMinutes, allowOverlap);
  }

  public void update(
      int minNoticeMinutes,
      int maxAdvanceDays,
      int cancellationWindowMinutes,
      boolean allowOverlap) {
    if (minNoticeMinutes < 0 || maxAdvanceDays < 0 || cancellationWindowMinutes < 0) {
      throw new IllegalArgumentException("Booking policy values must not be negative");
    }
    this.minNoticeMinutes = minNoticeMinutes;
    this.maxAdvanceDays = maxAdvanceDays;
    this.cancellationWindowMinutes = cancellationWindowMinutes;
    this.allowOverlap = allowOverlap;
  }

  public UUID getId() {
    return id;
  }

  public UUID getTenantId() {
    return tenantId;
  }

  public int getMinNoticeMinutes() {
    return minNoticeMinutes;
  }

  public int getMaxAdvanceDays() {
    return maxAdvanceDays;
  }

  public int getCancellationWindowMinutes() {
    return cancellationWindowMinutes;
  }

  public boolean isAllowOverlap() {
    return allowOverlap;
  }
}
