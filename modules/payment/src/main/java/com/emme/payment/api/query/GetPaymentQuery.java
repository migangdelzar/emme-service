package com.emme.payment.api.query;

import java.util.UUID;

public record GetPaymentQuery(UUID tenantId, UUID paymentId) {}
