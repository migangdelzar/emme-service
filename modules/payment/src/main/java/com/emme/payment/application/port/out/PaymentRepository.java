package com.emme.payment.application.port.out;

import com.emme.payment.domain.model.Payment;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Persistence capability required by Payment application services. */
public interface PaymentRepository {
  Optional<Payment> findByTenantIdAndId(UUID tenantId, UUID paymentId);

  Optional<Payment> findByTenantIdAndProviderReference(UUID tenantId, String providerReference);

  List<Payment> findByTenantId(UUID tenantId);

  Payment save(Payment payment);
}
