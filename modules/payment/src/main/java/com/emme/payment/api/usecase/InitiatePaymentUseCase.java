package com.emme.payment.api.usecase;

import com.emme.payment.domain.model.Payment;
import java.math.BigDecimal;
import java.util.UUID;

public interface InitiatePaymentUseCase {
  Payment initiate(UUID tenantId, String providerReference, BigDecimal amount, String currency);
}
