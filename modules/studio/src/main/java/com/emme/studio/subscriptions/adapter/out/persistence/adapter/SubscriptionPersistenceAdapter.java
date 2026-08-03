package com.emme.studio.subscriptions.adapter.out.persistence.adapter;

import com.emme.studio.subscriptions.adapter.out.persistence.entity.SubscriptionEntity;
import com.emme.studio.subscriptions.adapter.out.persistence.mapper.SubscriptionPersistenceMapper;
import com.emme.studio.subscriptions.adapter.out.persistence.repository.SpringDataSubscriptionRepository;
import com.emme.studio.subscriptions.application.port.out.SubscriptionRepository;
import com.emme.studio.subscriptions.domain.model.Subscription;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionPersistenceAdapter implements SubscriptionRepository {
  private final SpringDataSubscriptionRepository repository;
  private final SubscriptionPersistenceMapper mapper;

  public SubscriptionPersistenceAdapter(
      SpringDataSubscriptionRepository repository, SubscriptionPersistenceMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Override
  public Optional<Subscription> findByTenantId(UUID tenantId) {
    return repository.findByTenantId(tenantId).map(mapper::toDomain);
  }

  @Override
  public Subscription save(Subscription subscription) {
    SubscriptionEntity existing =
        repository
            .findByTenantIdAndId(subscription.tenantId(), subscription.id())
            .orElseGet(() -> mapper.toEntity(subscription));
    existing.setPlan(subscription.plan());
    existing.setStatus(subscription.status());
    existing.setPeriodEndsAt(subscription.periodEndsAt());
    return mapper.toDomain(repository.save(existing));
  }
}
