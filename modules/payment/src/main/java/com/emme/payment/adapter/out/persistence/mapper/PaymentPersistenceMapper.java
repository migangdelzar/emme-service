package com.emme.payment.adapter.out.persistence.mapper;

import com.emme.payment.adapter.out.persistence.entity.PaymentEntity;
import com.emme.payment.domain.model.Payment;
import org.springframework.stereotype.Component;

/** Maps the framework-free Payment aggregate to the JPA representation. */
@Component
public class PaymentPersistenceMapper {
  public Payment toDomain(PaymentEntity entity) {
    return Payment.restore(
        entity.getId(),
        entity.getTenantId(),
        entity.getProviderReference(),
        entity.getAmount(),
        entity.getCurrency(),
        entity.getStatus(),
        entity.getUpdatedAt());
  }

  public PaymentEntity toEntity(Payment payment) {
    PaymentEntity entity =
        new PaymentEntity(
            payment.tenantId(), payment.providerReference(), payment.amount(), payment.currency());
    entity.restoreIdentity(payment.id(), payment.updatedAt());
    entity.setStatus(payment.status());
    return entity;
  }
}
