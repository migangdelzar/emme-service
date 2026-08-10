package com.emme.salon.domain.model;

import java.util.Objects;
import java.util.UUID;

/** Tenant-owned identity and localization settings for a studio. */
public final class BusinessProfile {

  private final UUID id;
  private final UUID tenantId;
  private String timeZone;
  private String locale;
  private String displayName;

  public BusinessProfile(UUID tenantId, String timeZone, String locale, String displayName) {
    this(null, tenantId, timeZone, locale, displayName);
  }

  private BusinessProfile(
      UUID id, UUID tenantId, String timeZone, String locale, String displayName) {
    this.id = id;
    this.tenantId = Objects.requireNonNull(tenantId, "tenantId must not be null");
    update(timeZone, locale, displayName);
  }

  public static BusinessProfile reconstitute(
      UUID id, UUID tenantId, String timeZone, String locale, String displayName) {
    return new BusinessProfile(id, tenantId, timeZone, locale, displayName);
  }

  public void update(String timeZone, String locale, String displayName) {
    this.timeZone = Objects.requireNonNull(timeZone, "timeZone must not be null");
    this.locale = Objects.requireNonNull(locale, "locale must not be null");
    this.displayName = displayName;
  }

  public UUID getId() {
    return id;
  }

  public UUID getTenantId() {
    return tenantId;
  }

  public String getTimeZone() {
    return timeZone;
  }

  public String getLocale() {
    return locale;
  }

  public String getDisplayName() {
    return displayName;
  }
}
