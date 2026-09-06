package com.emme.payment.adapter.out.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emme.ai.contracts.payment.PaymentLink;
import com.emme.payment.adapter.out.persistence.entity.PaymentLinkEntity;
import com.emme.payment.adapter.out.persistence.mapper.PaymentLinkPersistenceMapper;
import com.emme.payment.adapter.out.persistence.repository.SpringDataPaymentLinkRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PaymentLinkPersistenceAdapterTest {

  @Test
  void findsLinkByIdempotencyKeyInTheCurrentTenantSchema() {
    SpringDataPaymentLinkRepository repository = mock();
    PaymentLinkPersistenceAdapter adapter =
        new PaymentLinkPersistenceAdapter(repository, new PaymentLinkPersistenceMapper());
    PaymentLinkEntity entity =
        new PaymentLinkEntity(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            "mock",
            "https://pay.test/1",
            Instant.parse("2030-01-01T09:15:00Z"),
            "payment-1");
    when(repository.findByIdempotencyKey("payment-1")).thenReturn(Optional.of(entity));

    assertThat(adapter.findByIdempotencyKey("payment-1"))
        .contains(
            new PaymentLink(
                entity.getId(),
                entity.getWorkflowId(),
                entity.getProvider(),
                entity.getCheckoutUrl(),
                entity.getExpiresAt()));

    verify(repository).findByIdempotencyKey("payment-1");
  }
}
