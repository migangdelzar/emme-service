package com.emme.payment.api.command;

import java.util.UUID;

public record AuthorizePaymentCommand(UUID paymentId) {}
