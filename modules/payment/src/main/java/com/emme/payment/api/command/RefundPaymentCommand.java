package com.emme.payment.api.command;

import java.util.UUID;

public record RefundPaymentCommand(UUID paymentId) {}
