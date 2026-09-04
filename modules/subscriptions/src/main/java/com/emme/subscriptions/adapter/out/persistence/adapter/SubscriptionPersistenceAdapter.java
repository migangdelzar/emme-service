package com.emme.subscriptions.adapter.out.persistence.adapter;

import com.emme.subscriptions.adapter.out.persistence.entity.SubscriptionEntity;
import com.emme.subscriptions.adapter.out.persistence.repository.SpringDataSubscriptionRepository;
import com.emme.subscriptions.application.port.out.SubscriptionRepository;
import com.emme.subscriptions.domain.model.Subscription;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionPersistenceAdapter implements SubscriptionRepository {
  private final SpringDataSubscriptionRepository repository;

  public SubscriptionPersistenceAdapter(SpringDataSubscriptionRepository repository) {
    this.repository = repository;
  }

  @Override
  public Optional<Subscription> findByTenantId(UUID tenantId) {
    return repository.findByTenantId(tenantId).map(SubscriptionEntity::toDomain);
  }

  @Override
  public Subscription save(Subscription subscription) {
    SubscriptionEntity existing =
        repository
            .findByTenantIdAndId(subscription.tenantId(), subscription.id())
            .orElseGet(() -> SubscriptionEntity.from(subscription));
    existing.setPlan(subscription.plan());
    existing.setStatus(subscription.status());
    existing.setPeriodEndsAt(subscription.periodEndsAt());
    return repository.save(existing).toDomain();
  }
}
