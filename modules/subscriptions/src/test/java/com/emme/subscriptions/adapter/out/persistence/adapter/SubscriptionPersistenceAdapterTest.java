package com.emme.subscriptions.adapter.out.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.emme.subscriptions.adapter.out.persistence.entity.SubscriptionEntity;
import com.emme.subscriptions.adapter.out.persistence.repository.SpringDataSubscriptionRepository;
import com.emme.subscriptions.api.type.PlanType;
import com.emme.subscriptions.domain.model.Subscription;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SubscriptionPersistenceAdapterTest {

  private final SpringDataSubscriptionRepository repository = org.mockito.Mockito.mock();
  private final SubscriptionPersistenceAdapter adapter =
      new SubscriptionPersistenceAdapter(repository);

  @Test
  void findsSubscriptionThroughTheExistingJpaEntityMapping() {
    UUID tenantId = UUID.randomUUID();
    Subscription subscription =
        new Subscription(tenantId, PlanType.STARTER, Instant.parse("2026-09-04T00:00:00Z"));
    SubscriptionEntity entity = SubscriptionEntity.from(subscription);
    when(repository.findByTenantId(tenantId)).thenReturn(java.util.Optional.of(entity));

    var found = adapter.findByTenantId(tenantId);

    assertThat(found).isPresent();
    assertThat(found.orElseThrow().id()).isEqualTo(subscription.id());
    assertThat(found.orElseThrow().tenantId()).isEqualTo(subscription.tenantId());
    assertThat(found.orElseThrow().plan()).isEqualTo(subscription.plan());
  }
}
