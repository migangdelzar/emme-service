package com.emme.payment.adapter.out.persistence.mapper;

import com.emme.payment.adapter.out.persistence.entity.PaymentWorkflowCorrelationEntity;
import com.emme.payment.api.port.out.PaymentWorkflowCorrelationRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Maps provider workflow correlations without exposing persistence types to the application. */
@Component
public final class PaymentWorkflowCorrelationPersistenceMapper {

  public PaymentWorkflowCorrelationRepository.PaymentWorkflowCorrelation toDomain(
      PaymentWorkflowCorrelationEntity entity) {
    return new PaymentWorkflowCorrelationRepository.PaymentWorkflowCorrelation(
        entity.getWorkflowId(),
        entity.getProvider(),
        entity.getProviderReference(),
        entity.getAppointmentHoldId());
  }

  public PaymentWorkflowCorrelationEntity toNewEntity(
      PaymentWorkflowCorrelationRepository.PaymentWorkflowCorrelation correlation, UUID tenantId) {
    return new PaymentWorkflowCorrelationEntity(
        tenantId,
        correlation.workflowId(),
        correlation.provider(),
        correlation.providerReference(),
        correlation.appointmentHoldId());
  }
}
