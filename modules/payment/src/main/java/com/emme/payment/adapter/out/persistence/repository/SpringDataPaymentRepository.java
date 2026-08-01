package com.emme.payment.adapter.out.persistence.repository;

import com.emme.payment.adapter.out.persistence.entity.PaymentEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataPaymentRepository extends JpaRepository<PaymentEntity, UUID> {
  List<PaymentEntity> findByTenantId(UUID tenantId);

  Optional<PaymentEntity> findByTenantIdAndProviderReference(
      UUID tenantId, String providerReference);
}
