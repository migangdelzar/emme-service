package com.emme.payment.api.command;

import java.util.UUID;

public record CapturePaymentCommand(UUID tenantId, UUID paymentId) {}
