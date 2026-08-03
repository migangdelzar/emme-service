package com.emme.payment.api.result;

import com.emme.payment.domain.model.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentInfo(
    UUID id,
    UUID tenantId,
    String providerReference,
    BigDecimal amount,
    String currency,
    PaymentStatus status,
    Instant updatedAt) {}
