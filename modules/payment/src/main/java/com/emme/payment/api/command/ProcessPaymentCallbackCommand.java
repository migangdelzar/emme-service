package com.emme.payment.api.command;

import java.util.Map;
import java.util.UUID;

public record ProcessPaymentCallbackCommand(
    UUID tenantId, Map<String, String> payload, String signature) {}
