package com.emme.assistant.adapter.out.persistence.repository;

import com.emme.assistant.adapter.out.persistence.entity.WhatsAppWebhookEventEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface SpringDataWhatsAppWebhookEventRepository
    extends JpaRepository<WhatsAppWebhookEventEntity, UUID> {

  @Modifying
  @Transactional
  @Query(
      value =
          """
          INSERT INTO whatsapp_webhook_event
              (id, tenant_id, provider, event_id, received_at, created_at, updated_at, version)
          VALUES (
              gen_random_uuid(), :tenantId, :provider, :eventId,
              CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
          ON CONFLICT (tenant_id, provider, event_id) DO NOTHING
          """,
      nativeQuery = true)
  int insertIfAbsent(
      @Param("tenantId") UUID tenantId,
      @Param("provider") String provider,
      @Param("eventId") String eventId);
}
