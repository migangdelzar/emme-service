package com.emme.payment.adapter.out.persistence.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.payment.adapter.out.persistence.entity.PaymentWorkflowCorrelationEntity;
import com.emme.payment.api.port.out.PaymentWorkflowCorrelationRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PaymentWorkflowCorrelationPersistenceMapperTest {

  @Test
  void mapsWorkflowCorrelationWithoutExposingPersistenceTypes() {
    UUID tenantId = UUID.randomUUID();
    PaymentWorkflowCorrelationRepository.PaymentWorkflowCorrelation correlation =
        new PaymentWorkflowCorrelationRepository.PaymentWorkflowCorrelation(
            UUID.randomUUID(), "mock", "provider-payment-1", UUID.randomUUID());

    PaymentWorkflowCorrelationPersistenceMapper mapper =
        new PaymentWorkflowCorrelationPersistenceMapper();
    PaymentWorkflowCorrelationEntity entity = mapper.toNewEntity(correlation, tenantId);

    assertThat(entity.getTenantId()).isEqualTo(tenantId);
    assertThat(entity.getWorkflowId()).isEqualTo(correlation.workflowId());
    assertThat(entity.getProvider()).isEqualTo(correlation.provider());
    assertThat(entity.getProviderReference()).isEqualTo(correlation.providerReference());
    assertThat(entity.getAppointmentHoldId()).isEqualTo(correlation.appointmentHoldId());
    assertThat(mapper.toDomain(entity)).isEqualTo(correlation);
  }
}
