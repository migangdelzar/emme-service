package com.emme.payment.adapter.out.persistence.adapter;

import com.emme.payment.adapter.out.persistence.entity.PaymentWebhookEventEntity;
import com.emme.payment.adapter.out.persistence.repository.SpringDataPaymentWebhookEventRepository;
import com.emme.payment.application.port.out.PaymentWebhookEventRepository;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

/** Implements durable webhook idempotency with a database uniqueness constraint. */
@Component
public class PaymentWebhookEventPersistenceAdapter implements PaymentWebhookEventRepository {
  private final SpringDataPaymentWebhookEventRepository repository;

  public PaymentWebhookEventPersistenceAdapter(SpringDataPaymentWebhookEventRepository repository) {
    this.repository = repository;
  }

  @Override
  public boolean claim(UUID tenantId, String provider, String eventId) {
    if (repository.existsByTenantIdAndProviderAndEventId(tenantId, provider, eventId)) {
      return false;
    }
    try {
      repository.saveAndFlush(new PaymentWebhookEventEntity(tenantId, provider, eventId));
      return true;
    } catch (DataIntegrityViolationException duplicate) {
      return false;
    }
  }
}
