package com.emme.subscriptions.application.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emme.subscriptions.api.type.PlanType;
import com.emme.subscriptions.application.port.out.SubscriptionRepository;
import com.emme.subscriptions.domain.model.Subscription;
import com.emme.subscriptions.domain.model.SubscriptionStatus;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EnsureTenantSubscriptionServiceTest {

  @Mock private SubscriptionRepository repository;

  @Test
  void provisionsOnlyWhenTheTenantHasNoSubscription() {
    UUID tenantId = UUID.randomUUID();
    when(repository.find()).thenReturn(Optional.empty());

    new EnsureTenantSubscriptionService(repository).ensure(tenantId);

    ArgumentCaptor<Subscription> captor = ArgumentCaptor.forClass(Subscription.class);
    verify(repository).save(captor.capture());
    Subscription provisioned = captor.getValue();
    org.assertj.core.api.Assertions.assertThat(provisioned.plan()).isEqualTo(PlanType.PRO);
    org.assertj.core.api.Assertions.assertThat(provisioned.status())
        .isEqualTo(SubscriptionStatus.ACTIVE);
    org.assertj.core.api.Assertions.assertThat(provisioned.periodEndsAt())
        .isAfter(Instant.now().plus(java.time.Duration.ofDays(29)));
  }

  @Test
  void treatsAnExistingSubscriptionAsAnIdempotentSuccess() {
    UUID tenantId = UUID.randomUUID();
    when(repository.find())
        .thenReturn(
            Optional.of(
                new Subscription(
                    tenantId,
                    com.emme.subscriptions.api.type.PlanType.PRO,
                    java.time.Instant.now())));

    new EnsureTenantSubscriptionService(repository).ensure(tenantId);

    verify(repository, never()).save(any(Subscription.class));
  }
}
