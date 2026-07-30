package com.emme.payment.application;

import com.emme.payment.entity.Payment;
import com.emme.payment.entity.PaymentRepository;
import com.emme.payment.entity.PaymentStatus;
import com.emme.payment.provider.PaymentProvider;
import com.emme.payment.provider.PaymentProviderException;
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

  private final PaymentRepository repository;
  private final PaymentProvider provider;

  public PaymentService(PaymentRepository repository, PaymentProvider provider) {
    this.repository = repository;
    this.provider = provider;
  }

  public Payment initiate(
      UUID tenantId, String providerReference, BigDecimal amount, String currency) {
    Optional<Payment> existing =
        repository.findByTenantIdAndProviderReference(tenantId, providerReference);
    if (existing.isPresent()) return existing.get(); // idempotent

    String idempotencyKey = tenantId + "/" + UUID.randomUUID();
    PaymentProvider.PaymentResult result =
        provider.initiate(idempotencyKey, amount, currency, "Payment for tenant " + tenantId);
    if (result == null || result.providerTransactionId() == null) {
      throw new PaymentProviderException("Provider initiate returned null transaction ID");
    }

    return repository.save(new Payment(tenantId, result.providerTransactionId(), amount, currency));
  }

  public Payment authorize(UUID paymentId) {
    Payment payment =
        repository
            .findById(paymentId)
            .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentId));
    PaymentProvider.PaymentResult result = provider.authorize(payment.getProviderReference());
    updateStatus(payment, result.status());
    return repository.save(payment);
  }

  public Payment capture(UUID paymentId) {
    Payment payment =
        repository
            .findById(paymentId)
            .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentId));
    PaymentProvider.PaymentResult result =
        provider.capture(payment.getProviderReference(), payment.getAmount());
    updateStatus(payment, result.status());
    return repository.save(payment);
  }

  public Payment refund(UUID paymentId) {
    Payment payment =
        repository
            .findById(paymentId)
            .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentId));
    PaymentProvider.PaymentResult result =
        provider.refund(payment.getProviderReference(), payment.getAmount(), "Refund requested");
    updateStatus(payment, result.status());
    return repository.save(payment);
  }

  public Payment processCallback(UUID tenantId, Map<String, String> payload, String signature) {
    PaymentProvider.PaymentResult result = provider.handleCallback(payload, signature);
    if (result == null || result.providerTransactionId() == null) {
      throw new PaymentProviderException("Provider handleCallback returned null transaction ID");
    }

    String providerReference = result.providerTransactionId();
    Optional<Payment> existing =
        repository.findByTenantIdAndProviderReference(tenantId, providerReference);
    if (existing.isPresent()) {
      Payment payment = existing.get();
      updateStatus(payment, result.status());
      return repository.save(payment);
    }
    return repository.save(new Payment(tenantId, providerReference, BigDecimal.ZERO, "MXN"));
  }

  @Transactional(readOnly = true)
  public Optional<Payment> findById(UUID id) {
    return repository.findById(id);
  }

  @Transactional(readOnly = true)
  public List<Payment> findByTenantId(UUID tenantId) {
    return repository.findByTenantId(tenantId);
  }

  private void updateStatus(Payment payment, String providerStatus) {
    payment.setStatus(PaymentStatus.valueOf(providerStatus.toUpperCase()));
  }
}
