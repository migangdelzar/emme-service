package com.emme.payment.api.command;

import java.math.BigDecimal;
import java.util.UUID;

public record InitiatePaymentCommand(
    UUID tenantId, String providerReference, BigDecimal amount, String currency) {}
