package com.emme.payment.adapter.out.persistence.adapter;

import com.emme.ai.contracts.payment.PaymentLink;
import com.emme.kernel.context.TenantContextHolder;
import com.emme.payment.adapter.out.persistence.entity.PaymentLinkEntity;
import com.emme.payment.adapter.out.persistence.mapper.PaymentLinkPersistenceMapper;
import com.emme.payment.adapter.out.persistence.repository.SpringDataPaymentLinkRepository;
import com.emme.payment.application.port.out.PaymentLinkRepository;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Persists links in the tenant schema selected for the current connection. */
@Component
public final class PaymentLinkPersistenceAdapter implements PaymentLinkRepository {

  private final SpringDataPaymentLinkRepository repository;
  private final PaymentLinkPersistenceMapper mapper;

  public PaymentLinkPersistenceAdapter(
      SpringDataPaymentLinkRepository repository, PaymentLinkPersistenceMapper mapper) {
    this.repository = Objects.requireNonNull(repository, "repository must not be null");
    this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
  }

  @Override
  public Optional<PaymentLink> findByIdempotencyKey(String idempotencyKey) {
    return repository.findByIdempotencyKey(idempotencyKey).map(mapper::toDomain);
  }

  @Override
  public PaymentLink save(PaymentLink link, String idempotencyKey) {
    PaymentLinkEntity entity =
        repository
            .findById(link.linkId())
            .orElseGet(
                () ->
                    mapper.toNewEntity(
                        link, idempotencyKey, TenantContextHolder.requireCurrentTenantId()));
    mapper.updateEntity(link, entity);
    return mapper.toDomain(repository.save(entity));
  }
}
