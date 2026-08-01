package com.emme.identity.domain.model;

import com.emme.studio.subscriptions.api.PlanType;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Domain representation of a global or tenant-specific feature flag. */
public final class FeatureFlag {

  private final UUID id;
  private final UUID tenantId;
  private final String code;
  private boolean enabled;
  private final PlanType planRequired;
  private final String description;
  private final Instant createdAt;
  private final Instant updatedAt;

  public FeatureFlag(
      UUID tenantId, String code, boolean enabled, PlanType planRequired, String description) {
    this(null, tenantId, code, enabled, planRequired, description, null, null);
  }

  private FeatureFlag(
      UUID id,
      UUID tenantId,
      String code,
      boolean enabled,
      PlanType planRequired,
      String description,
      Instant createdAt,
      Instant updatedAt) {
    this.id = id;
    this.tenantId = tenantId;
    this.code = Objects.requireNonNull(code, "code must not be null");
    this.enabled = enabled;
    this.planRequired = planRequired;
    this.description = description;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public static FeatureFlag rehydrate(
      UUID id,
      UUID tenantId,
      String code,
      boolean enabled,
      PlanType planRequired,
      String description,
      Instant createdAt,
      Instant updatedAt) {
    return new FeatureFlag(
        Objects.requireNonNull(id, "id must not be null"),
        tenantId,
        code,
        enabled,
        planRequired,
        description,
        Objects.requireNonNull(createdAt, "createdAt must not be null"),
        Objects.requireNonNull(updatedAt, "updatedAt must not be null"));
  }

  public UUID id() {
    return id;
  }

  public UUID tenantId() {
    return tenantId;
  }

  public String code() {
    return code;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public PlanType planRequired() {
    return planRequired;
  }

  public String description() {
    return description;
  }

  public Instant createdAt() {
    return createdAt;
  }

  public Instant updatedAt() {
    return updatedAt;
  }

  public void changeEnabled(boolean enabled) {
    this.enabled = enabled;
  }
}
