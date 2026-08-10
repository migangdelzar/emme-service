package com.emme.subscriptions.adapter.out.persistence.mapper;

import com.emme.subscriptions.adapter.out.persistence.entity.SubscriptionEntity;
import com.emme.subscriptions.domain.model.Subscription;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionPersistenceMapper {
  public SubscriptionEntity toEntity(Subscription subscription) {
    return SubscriptionEntity.from(subscription);
  }

  public Subscription toDomain(SubscriptionEntity entity) {
    return entity.toDomain();
  }
}
