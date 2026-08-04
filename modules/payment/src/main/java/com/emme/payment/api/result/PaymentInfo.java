package com.emme.payment.api.result;

import com.emme.payment.api.type.PaymentStatusView;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentInfo(
    UUID id,
    UUID tenantId,
    String providerReference,
    BigDecimal amount,
    String currency,
    PaymentStatusView status,
    Instant updatedAt) {}
