package com.emme.assistant.application.port.out;

import java.util.UUID;

/** Durable tenant-scoped idempotency boundary for WhatsApp deliveries. */
public interface WhatsAppWebhookEventRepository {
  boolean claim(UUID tenantId, String provider, String eventId);
}
