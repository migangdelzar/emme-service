package com.emme.subscriptions.application.service;

import com.emme.subscriptions.api.type.PlanType;
import com.emme.subscriptions.api.usecase.EnsureTenantSubscriptionUseCase;
import com.emme.subscriptions.application.port.out.SubscriptionRepository;
import com.emme.subscriptions.domain.model.Subscription;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Ensures the default subscription for an activated tenant without duplicate inserts. */
@Service
@Transactional
@RequiredArgsConstructor
public class EnsureTenantSubscriptionService implements EnsureTenantSubscriptionUseCase {

  private static final Duration INITIAL_PERIOD = Duration.ofDays(30);

  private final SubscriptionRepository repository;

  @Override
  public void ensure(UUID tenantId) {
    if (repository.find().isEmpty()) {
      repository.save(
          Subscription.provisioned(tenantId, PlanType.PRO, Instant.now().plus(INITIAL_PERIOD)));
    }
  }
}
