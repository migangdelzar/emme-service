package com.emme.studio.subscriptions.application;

import com.emme.studio.subscriptions.api.PlanType;
import com.emme.studio.subscriptions.entity.Subscription;
import com.emme.studio.subscriptions.entity.SubscriptionRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@org.springframework.modulith.NamedInterface("subscriptions-api")
@Service
@Transactional
public class SubscriptionService {

  private final SubscriptionRepository repo;

  public SubscriptionService(SubscriptionRepository repo) {
    this.repo = repo;
  }

  @Transactional(readOnly = true)
  public Optional<Subscription> getForTenant(UUID tenantId) {
    return repo.findByTenantId(tenantId);
  }

  @Transactional(readOnly = true)
  public Optional<PlanType> getPlanForTenant(UUID tenantId) {
    return repo.findByTenantId(tenantId).map(Subscription::getPlan);
  }

  public Subscription create(UUID tenantId, PlanType plan) {
    if (repo.findByTenantId(tenantId).isPresent()) {
      throw new IllegalArgumentException("Subscription already exists for tenant: " + tenantId);
    }
    Subscription sub =
        new Subscription(tenantId, plan, Instant.now().plusSeconds(365L * 24 * 3600));
    return repo.save(sub);
  }

  public void enforce(UUID tenantId, String entitlement) {
    Subscription sub =
        repo.findByTenantId(tenantId)
            .orElseThrow(
                () -> new IllegalArgumentException("No subscription for tenant: " + tenantId));
    EntitlementEnforcer.enforce(sub.getPlan(), entitlement);
  }

  public Subscription changePlan(UUID tenantId, PlanType newPlan) {
    Subscription sub =
        repo.findByTenantId(tenantId)
            .orElseThrow(() -> new IllegalArgumentException("No subscription for tenant"));
    sub.setPlan(newPlan);
    return repo.save(sub);
  }
}
