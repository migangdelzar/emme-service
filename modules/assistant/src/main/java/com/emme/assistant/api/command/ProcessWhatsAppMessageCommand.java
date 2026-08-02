package com.emme.assistant.api.command;

import java.util.UUID;

/** Inbound command for processing one already-verified WhatsApp message. */
public record ProcessWhatsAppMessageCommand(
    UUID tenantId, String eventId, String from, String text) {}
