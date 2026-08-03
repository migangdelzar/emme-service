package com.emme.studio.subscriptions.application.port.out;

import com.emme.studio.subscriptions.domain.model.Subscription;
import java.util.Optional;
import java.util.UUID;

public interface SubscriptionRepository {
  Optional<Subscription> findByTenantId(UUID tenantId);

  Subscription save(Subscription subscription);
}
