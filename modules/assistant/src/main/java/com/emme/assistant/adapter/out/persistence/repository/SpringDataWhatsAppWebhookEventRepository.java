package com.emme.assistant.adapter.out.persistence.repository;

import com.emme.assistant.adapter.out.persistence.entity.WhatsAppWebhookEventEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataWhatsAppWebhookEventRepository
    extends JpaRepository<WhatsAppWebhookEventEntity, UUID> {
  boolean existsByTenantIdAndProviderAndEventId(UUID tenantId, String provider, String eventId);
}
