package com.emme.identity.adapter.out.persistence.entity;

import com.emme.shared.BaseEntity;
import com.emme.studio.subscriptions.api.PlanType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "feature_flag", schema = "emme_core")
public class FeatureFlag extends BaseEntity {

  @Column(name = "tenant_id")
  private UUID tenantId;

  @Column(name = "code", length = 80, nullable = false)
  private String code;

  @Column(name = "enabled", nullable = false)
  private boolean enabled;

  @Enumerated(EnumType.STRING)
  @Column(name = "plan_required")
  private PlanType planRequired;

  @Column(name = "description", length = 255)
  private String description;

  protected FeatureFlag() {}

  public FeatureFlag(
      UUID tenantId, String code, boolean enabled, PlanType planRequired, String description) {
    this.tenantId = tenantId;
    this.code = code;
    this.enabled = enabled;
    this.planRequired = planRequired;
    this.description = description;
  }

  public UUID getTenantId() {
    return tenantId;
  }

  public String getCode() {
    return code;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public PlanType getPlanRequired() {
    return planRequired;
  }

  public String getDescription() {
    return description;
  }
}
