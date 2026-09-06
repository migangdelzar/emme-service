package com.emme.payment.adapter.out.persistence.adapter;

import com.emme.kernel.context.TenantContextHolder;
import com.emme.payment.adapter.out.persistence.entity.PaymentWorkflowCorrelationEntity;
import com.emme.payment.adapter.out.persistence.mapper.PaymentWorkflowCorrelationPersistenceMapper;
import com.emme.payment.adapter.out.persistence.repository.SpringDataPaymentWorkflowCorrelationRepository;
import com.emme.payment.application.port.out.PaymentWorkflowCorrelationRepository;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Persists callback ownership in the tenant schema selected for the current connection. */
@Component
public final class PaymentWorkflowCorrelationPersistenceAdapter
    implements PaymentWorkflowCorrelationRepository {

  private final SpringDataPaymentWorkflowCorrelationRepository repository;
  private final PaymentWorkflowCorrelationPersistenceMapper mapper;

  public PaymentWorkflowCorrelationPersistenceAdapter(
      SpringDataPaymentWorkflowCorrelationRepository repository,
      PaymentWorkflowCorrelationPersistenceMapper mapper) {
    this.repository = Objects.requireNonNull(repository, "repository must not be null");
    this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
  }

  @Override
  public Optional<PaymentWorkflowCorrelation> findByProviderAndProviderReference(
      String provider, String providerReference) {
    return repository
        .findByProviderAndProviderReference(provider, providerReference)
        .map(mapper::toDomain);
  }

  @Override
  public Optional<PaymentWorkflowCorrelation> findByWorkflowId(UUID workflowId) {
    return repository.findByWorkflowId(workflowId).map(mapper::toDomain);
  }

  @Override
  public PaymentWorkflowCorrelation save(PaymentWorkflowCorrelation correlation) {
    PaymentWorkflowCorrelationEntity entity =
        repository
            .findByWorkflowId(correlation.workflowId())
            .orElseGet(
                () ->
                    mapper.toNewEntity(correlation, TenantContextHolder.requireCurrentTenantId()));
    return mapper.toDomain(repository.save(entity));
  }
}
