package com.emme.payment.adapter.out.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emme.payment.adapter.out.persistence.entity.PaymentWorkflowCorrelationEntity;
import com.emme.payment.adapter.out.persistence.mapper.PaymentWorkflowCorrelationPersistenceMapper;
import com.emme.payment.adapter.out.persistence.repository.SpringDataPaymentWorkflowCorrelationRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PaymentWorkflowCorrelationPersistenceAdapterTest {

  @Test
  void findsTheWorkflowForAProviderReferenceInTheCurrentTenantSchema() {
    SpringDataPaymentWorkflowCorrelationRepository repository = mock();
    PaymentWorkflowCorrelationPersistenceAdapter adapter =
        new PaymentWorkflowCorrelationPersistenceAdapter(
            repository, new PaymentWorkflowCorrelationPersistenceMapper());
    UUID workflowId = UUID.randomUUID();
    PaymentWorkflowCorrelationEntity entity =
        new PaymentWorkflowCorrelationEntity(
            UUID.randomUUID(), workflowId, "mock", "provider-payment-1");
    when(repository.findByProviderAndProviderReference("mock", "provider-payment-1"))
        .thenReturn(Optional.of(entity));

    assertThat(adapter.findByProviderAndProviderReference("mock", "provider-payment-1"))
        .hasValueSatisfying(
            correlation -> {
              assertThat(correlation.workflowId()).isEqualTo(workflowId);
              assertThat(correlation.provider()).isEqualTo("mock");
            });

    verify(repository).findByProviderAndProviderReference("mock", "provider-payment-1");
  }
}
