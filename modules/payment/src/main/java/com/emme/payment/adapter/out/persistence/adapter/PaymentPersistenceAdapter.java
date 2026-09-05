package com.emme.payment.adapter.out.persistence.adapter;

import com.emme.payment.adapter.out.persistence.entity.PaymentEntity;
import com.emme.payment.adapter.out.persistence.mapper.PaymentPersistenceMapper;
import com.emme.payment.adapter.out.persistence.repository.SpringDataPaymentRepository;
import com.emme.payment.application.port.out.PaymentRepository;
import com.emme.payment.domain.model.Payment;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Implements the Payment persistence port without leaking JPA beyond the adapter. */
@Component
public class PaymentPersistenceAdapter implements PaymentRepository {
  private final SpringDataPaymentRepository repository;
  private final PaymentPersistenceMapper mapper;

  public PaymentPersistenceAdapter(
      SpringDataPaymentRepository repository, PaymentPersistenceMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Override
  public Optional<Payment> findById(UUID paymentId) {
    return repository.findById(paymentId).map(mapper::toDomain);
  }

  @Override
  public Optional<Payment> findByTenantIdAndProviderReference(
      UUID tenantId, String providerReference) {
    return repository
        .findByTenantIdAndProviderReference(tenantId, providerReference)
        .map(mapper::toDomain);
  }

  @Override
  public List<Payment> findByTenantId(UUID tenantId) {
    return repository.findByTenantId(tenantId).stream().map(mapper::toDomain).toList();
  }

  @Override
  public Payment save(Payment payment) {
    PaymentEntity saved = repository.save(mapper.toEntity(payment));
    return mapper.toDomain(saved);
  }
}
