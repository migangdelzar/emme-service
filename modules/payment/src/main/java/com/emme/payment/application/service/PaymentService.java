package com.emme.payment.application.service;

import com.emme.payment.adapter.out.persistence.entity.PaymentEntity;
import com.emme.payment.adapter.out.persistence.repository.SpringDataPaymentRepository;
import com.emme.payment.adapter.out.provider.PaymentProvider;
import com.emme.payment.adapter.out.provider.PaymentProviderException;
import com.emme.payment.domain.model.PaymentStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PaymentService {

  private final SpringDataPaymentRepository repository;
  private final PaymentProvider provider;

  public PaymentService(SpringDataPaymentRepository repository, PaymentProvider provider) {
    this.repository = repository;
    this.provider = provider;
  }

  public PaymentEntity initiate(
      UUID tenantId, String providerReference, BigDecimal amount, String currency) {
    Optional<PaymentEntity> existing =
        repository.findByTenantIdAndProviderReference(tenantId, providerReference);
    if (existing.isPresent()) return existing.get(); // idempotent

    String idempotencyKey = tenantId + "/" + UUID.randomUUID();
    PaymentProvider.PaymentResult result =
        provider.initiate(idempotencyKey, amount, currency, "Payment for tenant " + tenantId);
    if (result == null || result.providerTransactionId() == null) {
      throw new PaymentProviderException("Provider initiate returned null transaction ID");
    }

    return repository.save(
        new PaymentEntity(tenantId, result.providerTransactionId(), amount, currency));
  }

  public PaymentEntity authorize(UUID paymentId) {
    PaymentEntity payment =
        repository
            .findById(paymentId)
            .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentId));
    PaymentProvider.PaymentResult result = provider.authorize(payment.getProviderReference());
    updateStatus(payment, result.status());
    return repository.save(payment);
  }

  public PaymentEntity capture(UUID paymentId) {
    PaymentEntity payment =
        repository
            .findById(paymentId)
            .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentId));
    PaymentProvider.PaymentResult result =
        provider.capture(payment.getProviderReference(), payment.getAmount());
    updateStatus(payment, result.status());
    return repository.save(payment);
  }

  public PaymentEntity refund(UUID paymentId) {
    PaymentEntity payment =
        repository
            .findById(paymentId)
            .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentId));
    PaymentProvider.PaymentResult result =
        provider.refund(payment.getProviderReference(), payment.getAmount(), "Refund requested");
    updateStatus(payment, result.status());
    return repository.save(payment);
  }

  public PaymentEntity processCallback(
      UUID tenantId, Map<String, String> payload, String signature) {
    PaymentProvider.PaymentResult result = provider.handleCallback(payload, signature);
    if (result == null || result.providerTransactionId() == null) {
      throw new PaymentProviderException("Provider handleCallback returned null transaction ID");
    }

    String providerReference = result.providerTransactionId();
    Optional<PaymentEntity> existing =
        repository.findByTenantIdAndProviderReference(tenantId, providerReference);
    if (existing.isPresent()) {
      PaymentEntity payment = existing.get();
      updateStatus(payment, result.status());
      return repository.save(payment);
    }
    return repository.save(new PaymentEntity(tenantId, providerReference, BigDecimal.ZERO, "MXN"));
  }

  @Transactional(readOnly = true)
  public Optional<PaymentEntity> findById(UUID id) {
    return repository.findById(id);
  }

  @Transactional(readOnly = true)
  public List<PaymentEntity> findByTenantId(UUID tenantId) {
    return repository.findByTenantId(tenantId);
  }

  private void updateStatus(PaymentEntity payment, String providerStatus) {
    payment.setStatus(PaymentStatus.valueOf(providerStatus.toUpperCase()));
  }
}
