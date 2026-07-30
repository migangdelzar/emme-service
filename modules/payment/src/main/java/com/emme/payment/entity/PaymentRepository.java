package com.emme.payment.entity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
  List<Payment> findByTenantId(UUID tenantId);

  Optional<Payment> findByTenantIdAndProviderReference(UUID tenantId, String providerReference);
}
