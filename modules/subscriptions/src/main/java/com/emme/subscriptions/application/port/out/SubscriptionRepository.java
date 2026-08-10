package com.emme.subscriptions.application.port.out;

import com.emme.subscriptions.domain.model.Subscription;
import java.util.Optional;
import java.util.UUID;

public interface SubscriptionRepository {
  Optional<Subscription> findByTenantId(UUID tenantId);

  Subscription save(Subscription subscription);
}
