package com.emme.payment.api.port.out;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Public workflow input boundary for trusted, tenant-local payment facts. */
public interface PaymentLinkSourceRepository {

  Optional<PaymentLinkSource> findByWorkflowIdAndHoldId(UUID workflowId, UUID holdId);

  record PaymentLinkSource(
      BigDecimal amount, String currency, String description, Instant expiresAt) {

    public PaymentLinkSource {
      if (amount == null || amount.signum() <= 0) {
        throw new IllegalArgumentException("amount must be positive");
      }
      if (currency == null || currency.isBlank()) {
        throw new IllegalArgumentException("currency must not be blank");
      }
      if (description == null || description.isBlank()) {
        throw new IllegalArgumentException("description must not be blank");
      }
      if (expiresAt == null) {
        throw new NullPointerException("expiresAt must not be null");
      }
    }
  }
}
