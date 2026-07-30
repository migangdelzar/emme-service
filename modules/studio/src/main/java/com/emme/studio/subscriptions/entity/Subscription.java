package com.emme.studio.subscriptions.entity;

import com.emme.shared.TenantOwnedEntity;
import com.emme.studio.subscriptions.api.PlanType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "subscription")
public class Subscription extends TenantOwnedEntity {

  @Enumerated(EnumType.STRING)
  @Column(name = "plan", nullable = false, length = 20)
  private PlanType plan;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 15)
  private SubscriptionStatus status = SubscriptionStatus.TRIAL;

  @Column(name = "period_ends_at", nullable = false)
  private Instant periodEndsAt;

  protected Subscription() {}

  public Subscription(UUID tenantId, PlanType plan, Instant periodEndsAt) {
    super(tenantId);
    this.plan = Objects.requireNonNull(plan, "plan must not be null");
    this.periodEndsAt = Objects.requireNonNull(periodEndsAt, "periodEndsAt must not be null");
  }

  public PlanType getPlan() {
    return plan;
  }

  public void setPlan(PlanType plan) {
    this.plan = plan;
  }

  public SubscriptionStatus getStatus() {
    return status;
  }

  public void setStatus(SubscriptionStatus status) {
    this.status = status;
  }

  public Instant getPeriodEndsAt() {
    return periodEndsAt;
  }

  public void setPeriodEndsAt(Instant periodEndsAt) {
    this.periodEndsAt = periodEndsAt;
  }
}
