package com.emme.subscriptions.adapter.out.persistence.repository;

import com.emme.subscriptions.adapter.out.persistence.entity.SubscriptionEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataSubscriptionRepository extends JpaRepository<SubscriptionEntity, UUID> {
  Optional<SubscriptionEntity> findByTenantId(UUID tenantId);

  Optional<SubscriptionEntity> findByTenantIdAndId(UUID tenantId, UUID subscriptionId);
}
