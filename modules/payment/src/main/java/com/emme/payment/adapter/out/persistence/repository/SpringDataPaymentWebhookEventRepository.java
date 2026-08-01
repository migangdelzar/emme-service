package com.emme.payment.adapter.out.persistence.repository;

import com.emme.payment.adapter.out.persistence.entity.PaymentWebhookEventEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataPaymentWebhookEventRepository
    extends JpaRepository<PaymentWebhookEventEntity, UUID> {
  boolean existsByTenantIdAndProviderAndEventId(UUID tenantId, String provider, String eventId);
}
