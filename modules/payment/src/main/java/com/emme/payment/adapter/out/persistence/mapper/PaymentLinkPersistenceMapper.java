package com.emme.payment.adapter.out.persistence.mapper;

import com.emme.ai.contracts.payment.PaymentLink;
import com.emme.payment.adapter.out.persistence.entity.PaymentLinkEntity;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Maps payment-link persistence without exposing JPA types to application contracts. */
@Component
public final class PaymentLinkPersistenceMapper {

  public PaymentLink toDomain(PaymentLinkEntity entity) {
    return new PaymentLink(
        entity.getId(),
        entity.getWorkflowId(),
        entity.getProvider(),
        entity.getCheckoutUrl(),
        entity.getExpiresAt());
  }

  public PaymentLinkEntity toNewEntity(PaymentLink link, String idempotencyKey, UUID tenantId) {
    return new PaymentLinkEntity(
        tenantId,
        link.linkId(),
        link.workflowId(),
        link.provider(),
        link.checkoutUrl(),
        link.expiresAt(),
        idempotencyKey);
  }

  public void updateEntity(PaymentLink link, PaymentLinkEntity entity) {
    entity.setExpiresAt(link.expiresAt());
  }
}
