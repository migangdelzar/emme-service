package com.emme.payment.adapter.out.persistence.repository;

import com.emme.payment.adapter.out.persistence.entity.PaymentWorkflowCorrelationEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataPaymentWorkflowCorrelationRepository
    extends JpaRepository<PaymentWorkflowCorrelationEntity, UUID> {

  Optional<PaymentWorkflowCorrelationEntity> findByProviderAndProviderReference(
      String provider, String providerReference);

  Optional<PaymentWorkflowCorrelationEntity> findByWorkflowId(UUID workflowId);
}
