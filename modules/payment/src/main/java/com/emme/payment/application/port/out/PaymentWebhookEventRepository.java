package com.emme.payment.application.port.out;

import java.util.UUID;

/**
 * Durable idempotency boundary for provider webhook events.
 *
 * <p>The implementation must atomically claim the tenant/provider/event tuple. A {@code false}
 * result means another transaction has already claimed the event.
 */
public interface PaymentWebhookEventRepository {
  boolean claim(UUID tenantId, String provider, String eventId);
}
