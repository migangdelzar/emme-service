package com.emme.subscriptions.domain.model;

import com.emme.subscriptions.api.SubscriptionEntitlementPolicy;
import com.emme.subscriptions.api.type.PlanType;
import com.emme.subscriptions.domain.exception.EntitlementViolationException;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Framework-free subscription aggregate owning plan and entitlement rules. */
public final class Subscription {

  private final UUID id;
  private final UUID tenantId;
  private final Instant createdAt;

  private PlanType plan;

  private SubscriptionStatus status = SubscriptionStatus.TRIAL;

  private Instant periodEndsAt;

  public Subscription(UUID tenantId, PlanType plan, Instant periodEndsAt) {
    this(UUID.randomUUID(), tenantId, plan, SubscriptionStatus.TRIAL, periodEndsAt, Instant.now());
  }

  private Subscription(
      UUID id,
      UUID tenantId,
      PlanType plan,
      SubscriptionStatus status,
      Instant periodEndsAt,
      Instant createdAt) {
    this.id = Objects.requireNonNull(id, "id must not be null");
    this.tenantId = Objects.requireNonNull(tenantId, "tenantId must not be null");
    this.plan = Objects.requireNonNull(plan, "plan must not be null");
    this.status = Objects.requireNonNull(status, "status must not be null");
    this.periodEndsAt = Objects.requireNonNull(periodEndsAt, "periodEndsAt must not be null");
    this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
  }

  public static Subscription rehydrate(
      UUID id,
      UUID tenantId,
      PlanType plan,
      SubscriptionStatus status,
      Instant periodEndsAt,
      Instant createdAt) {
    return new Subscription(id, tenantId, plan, status, periodEndsAt, createdAt);
  }

  public UUID id() {
    return id;
  }

  public UUID tenantId() {
    return tenantId;
  }

  public Instant createdAt() {
    return createdAt;
  }

  public PlanType plan() {
    return plan;
  }

  public SubscriptionStatus status() {
    return status;
  }

  public Instant periodEndsAt() {
    return periodEndsAt;
  }

  public void changePlan(PlanType plan) {
    this.plan = plan;
  }

  public void enforce(String entitlement) {
    if (!SubscriptionEntitlementPolicy.getEntitlements(plan).contains(entitlement)) {
      throw new EntitlementViolationException(plan, entitlement);
    }
  }
}
