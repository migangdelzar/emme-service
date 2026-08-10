package com.emme.payment.api.result;

import com.emme.payment.api.type.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentDetails(
    UUID id,
    UUID tenantId,
    String providerReference,
    BigDecimal amount,
    String currency,
    PaymentStatus status,
    Instant updatedAt) {}
