package com.emme.payment.adapter.out.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emme.payment.adapter.out.persistence.repository.SpringDataPaymentWebhookEventRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentWebhookEventPersistenceAdapterTest {

  @Mock private SpringDataPaymentWebhookEventRepository repository;

  @Test
  void claimsAnEventWhenTheAtomicInsertCreatesARow() {
    UUID tenantId = UUID.randomUUID();
    when(repository.insertIfAbsent(tenantId, "stripe", "evt-1")).thenReturn(1);

    boolean claimed =
        new PaymentWebhookEventPersistenceAdapter(repository).claim(tenantId, "stripe", "evt-1");

    assertThat(claimed).isTrue();
    verify(repository).insertIfAbsent(tenantId, "stripe", "evt-1");
  }

  @Test
  void rejectsAnEventWhenTheAtomicInsertFindsAnExistingClaim() {
    UUID tenantId = UUID.randomUUID();
    when(repository.insertIfAbsent(tenantId, "stripe", "evt-1")).thenReturn(0);

    boolean claimed =
        new PaymentWebhookEventPersistenceAdapter(repository).claim(tenantId, "stripe", "evt-1");

    assertThat(claimed).isFalse();
    verify(repository).insertIfAbsent(tenantId, "stripe", "evt-1");
  }
}
