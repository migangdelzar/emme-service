package com.emme.payment.application.port.out;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Tenant-schema correlation boundary between verified provider references and durable workflows.
 */
public interface PaymentWorkflowCorrelationRepository {

  Optional<PaymentWorkflowCorrelation> findByProviderAndProviderReference(
      String provider, String providerReference);

  Optional<PaymentWorkflowCorrelation> findByWorkflowId(UUID workflowId);

  PaymentWorkflowCorrelation save(PaymentWorkflowCorrelation correlation);

  record PaymentWorkflowCorrelation(
      UUID workflowId, String provider, String providerReference, UUID appointmentHoldId) {

    public PaymentWorkflowCorrelation {
      workflowId = Objects.requireNonNull(workflowId, "workflowId must not be null");
      provider = requireText(provider, "provider");
      providerReference = requireText(providerReference, "providerReference");
      appointmentHoldId =
          Objects.requireNonNull(appointmentHoldId, "appointmentHoldId must not be null");
    }

    private static String requireText(String value, String field) {
      Objects.requireNonNull(value, field + " must not be null");
      if (value.isBlank()) {
        throw new IllegalArgumentException(field + " must not be blank");
      }
      return value;
    }
  }
}
