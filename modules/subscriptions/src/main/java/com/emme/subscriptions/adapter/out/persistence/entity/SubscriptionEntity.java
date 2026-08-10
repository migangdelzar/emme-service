package com.emme.subscriptions.adapter.out.persistence.entity;

import com.emme.shared.persistence.TenantOwnedEntity;
import com.emme.subscriptions.api.type.PlanType;
import com.emme.subscriptions.domain.model.Subscription;
import com.emme.subscriptions.domain.model.SubscriptionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "subscription")
public class SubscriptionEntity extends TenantOwnedEntity {

  @Enumerated(EnumType.STRING)
  @Column(name = "plan", nullable = false, length = 20)
  private PlanType plan;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 15)
  private SubscriptionStatus status = SubscriptionStatus.TRIAL;

  @Column(name = "period_ends_at", nullable = false)
  private Instant periodEndsAt;

  protected SubscriptionEntity() {}

  public SubscriptionEntity(UUID tenantId, PlanType plan, Instant periodEndsAt) {
    super(tenantId);
    this.plan = plan;
    this.periodEndsAt = periodEndsAt;
  }

  private SubscriptionEntity(Subscription subscription) {
    super(subscription.tenantId());
    setId(subscription.id());
    this.plan = subscription.plan();
    this.status = subscription.status();
    this.periodEndsAt = subscription.periodEndsAt();
    restoreAuditFields(subscription.id(), subscription.createdAt(), subscription.createdAt());
  }

  public static SubscriptionEntity from(Subscription subscription) {
    return new SubscriptionEntity(subscription);
  }

  public Subscription toDomain() {
    return Subscription.rehydrate(
        getId(), getTenantId(), plan, status, periodEndsAt, getCreatedAt());
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
