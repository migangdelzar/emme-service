package com.emme.assistant.adapter.out.persistence.entity;

import com.emme.shared.persistence.TenantOwnedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "whatsapp_webhook_event",
    uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "provider", "event_id"}))
public class WhatsAppWebhookEventEntity extends TenantOwnedEntity {
  @Column(nullable = false, length = 40)
  private String provider;

  @Column(name = "event_id", nullable = false, length = 200)
  private String eventId;

  @Column(name = "received_at", nullable = false)
  private Instant receivedAt = Instant.now();

  protected WhatsAppWebhookEventEntity() {}

  public WhatsAppWebhookEventEntity(UUID tenantId, String provider, String eventId) {
    super(tenantId);
    this.provider = provider;
    this.eventId = eventId;
  }
}
