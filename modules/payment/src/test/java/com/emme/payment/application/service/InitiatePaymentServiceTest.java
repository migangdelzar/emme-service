package com.emme.payment.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.payment.api.command.InitiatePaymentCommand;
import com.emme.payment.api.result.PaymentDetails;
import com.emme.payment.application.port.out.PaymentProvider;
import com.emme.payment.application.port.out.PaymentRepository;
import com.emme.payment.domain.model.Payment;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class InitiatePaymentServiceTest {

  @Test
  void returnsExistingPaymentWithoutCallingProvider() {
    UUID tenantId = UUID.randomUUID();
    Payment existing = new Payment(tenantId, "provider-1", BigDecimal.TEN, "MXN");
    InMemoryRepository repository = new InMemoryRepository(existing);
    RecordingProvider provider = new RecordingProvider();
    InitiatePaymentService service = new InitiatePaymentService(repository, provider);

    PaymentDetails result =
        service.initiate(new InitiatePaymentCommand(tenantId, "provider-1", BigDecimal.TEN, "MXN"));

    assertThat(result.id()).isEqualTo(existing.id());
    assertThat(provider.initiateCalls).isZero();
  }

  @Test
  void persistsNewPaymentFromProviderTransaction() {
    UUID tenantId = UUID.randomUUID();
    InMemoryRepository repository = new InMemoryRepository();
    RecordingProvider provider = new RecordingProvider();
    InitiatePaymentService service = new InitiatePaymentService(repository, provider);

    PaymentDetails result =
        service.initiate(new InitiatePaymentCommand(tenantId, "request-1", BigDecimal.TEN, "MXN"));

    assertThat(result.providerReference()).isEqualTo("transaction-1");
    assertThat(repository.saved).isNotNull();
    assertThat(provider.initiateCalls).isEqualTo(1);
  }

  private static final class InMemoryRepository implements PaymentRepository {
    private final List<Payment> payments = new ArrayList<>();
    private Payment saved;

    private InMemoryRepository(Payment... initial) {
      payments.addAll(List.of(initial));
    }

    @Override
    public Optional<Payment> findById(UUID paymentId) {
      return payments.stream().filter(payment -> payment.id().equals(paymentId)).findFirst();
    }

    @Override
    public Optional<Payment> findByTenantIdAndProviderReference(
        UUID tenantId, String providerReference) {
      return payments.stream()
          .filter(payment -> payment.tenantId().equals(tenantId))
          .filter(payment -> payment.providerReference().equals(providerReference))
          .findFirst();
    }

    @Override
    public List<Payment> findByTenantId(UUID tenantId) {
      return payments.stream().filter(payment -> payment.tenantId().equals(tenantId)).toList();
    }

    @Override
    public Payment save(Payment payment) {
      saved = payment;
      payments.add(payment);
      return payment;
    }
  }

  private static final class RecordingProvider implements PaymentProvider {
    private int initiateCalls;

    @Override
    public String name() {
      return "test";
    }

    @Override
    public PaymentResult initiate(
        String idempotencyKey, BigDecimal amount, String currency, String description) {
      initiateCalls++;
      return new PaymentResult("transaction-1", "PENDING", Map.of());
    }

    @Override
    public PaymentResult authorize(String providerTransactionId) {
      return new PaymentResult(providerTransactionId, "AUTHORIZED", Map.of());
    }

    @Override
    public PaymentResult capture(String providerTransactionId, BigDecimal amount) {
      return new PaymentResult(providerTransactionId, "CAPTURED", Map.of());
    }

    @Override
    public PaymentResult refund(String providerTransactionId, BigDecimal amount, String reason) {
      return new PaymentResult(providerTransactionId, "REFUNDED", Map.of());
    }

    @Override
    public PaymentResult handleCallback(Map<String, String> payload, String signature) {
      return new PaymentResult("transaction-1", "PENDING", Map.of());
    }
  }
}
