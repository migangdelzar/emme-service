package com.emme.subscriptions.application.service;

import com.emme.subscriptions.api.type.PlanType;
import com.emme.subscriptions.api.usecase.EnsureTenantSubscriptionUseCase;
import com.emme.subscriptions.application.port.out.SubscriptionRepository;
import com.emme.subscriptions.domain.model.Subscription;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Ensures the default subscription for an activated tenant without duplicate inserts. */
@Service
@Transactional
public class EnsureTenantSubscriptionService implements EnsureTenantSubscriptionUseCase {

  private static final Duration INITIAL_PERIOD = Duration.ofDays(30);

  private final SubscriptionRepository repository;

  public EnsureTenantSubscriptionService(SubscriptionRepository repository) {
    this.repository = repository;
  }

  @Override
  public void ensure(UUID tenantId) {
    if (repository.findByTenantId(tenantId).isEmpty()) {
      repository.save(
          Subscription.provisioned(tenantId, PlanType.PRO, Instant.now().plus(INITIAL_PERIOD)));
    }
  }
}
