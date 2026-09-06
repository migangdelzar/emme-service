package com.emme.payment.adapter.out.persistence.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.ai.contracts.payment.PaymentLink;
import com.emme.payment.adapter.out.persistence.entity.PaymentLinkEntity;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PaymentLinkPersistenceMapperTest {

  @Test
  void mapsTenantLocalLinkAndKeepsItsIdempotencyKeyInPersistenceOnly() {
    UUID tenantId = UUID.randomUUID();
    PaymentLink link =
        new PaymentLink(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "mock",
            "https://pay.test/1",
            Instant.parse("2030-01-01T09:15:00Z"));

    PaymentLinkPersistenceMapper mapper = new PaymentLinkPersistenceMapper();
    PaymentLinkEntity entity = mapper.toNewEntity(link, "payment-1", tenantId);

    assertThat(entity.getId()).isEqualTo(link.linkId());
    assertThat(entity.getTenantId()).isEqualTo(tenantId);
    assertThat(entity.getWorkflowId()).isEqualTo(link.workflowId());
    assertThat(entity.getProvider()).isEqualTo(link.provider());
    assertThat(entity.getCheckoutUrl()).isEqualTo(link.checkoutUrl());
    assertThat(entity.getExpiresAt()).isEqualTo(link.expiresAt());
    assertThat(entity.getIdempotencyKey()).isEqualTo("payment-1");
    assertThat(mapper.toDomain(entity)).isEqualTo(link);
  }
}
