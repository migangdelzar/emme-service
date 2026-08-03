package com.emme.assistant.adapter.in.webhook;

import java.util.UUID;

/** Normalized inbound WhatsApp webhook message before application dispatch. */
public record WhatsAppWebhookMessage(
    UUID tenantId, String eventId, String from, String text, boolean statusUpdate) {}
