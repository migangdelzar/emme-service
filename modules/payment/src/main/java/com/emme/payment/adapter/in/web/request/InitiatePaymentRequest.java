package com.emme.payment.adapter.in.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record InitiatePaymentRequest(
    @NotBlank String providerReference,
    @NotNull @Positive BigDecimal amount,
    @NotBlank String currency) {}
