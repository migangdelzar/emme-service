package com.emme.payment.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.payment.api.command.ProcessPaymentCallbackCommand;
import com.emme.payment.api.result.PaymentDetails;
import com.emme.payment.application.port.out.PaymentProvider;
import com.emme.payment.application.port.out.PaymentRepository;
import com.emme.payment.application.port.out.PaymentWebhookEventRepository;
import com.emme.payment.domain.model.Payment;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProcessPaymentCallbackServiceTest {

  @Test
  void ignoresDuplicateProviderEventsWithoutApplyingTheCallbackTwice() {
    UUID tenantId = UUID.randomUUID();
    Payment payment = new Payment(tenantId, "provider-payment-1", BigDecimal.TEN, "MXN");
    InMemoryPaymentRepository payments = new InMemoryPaymentRepository(payment);
    RecordingWebhookEvents events = new RecordingWebhookEvents();
    RecordingProvider provider = new RecordingProvider();
    ProcessPaymentCallbackService service =
        new ProcessPaymentCallbackService(payments, provider, events);

    ProcessPaymentCallbackCommand command =
        new ProcessPaymentCallbackCommand(
            tenantId,
            "mercadopago",
            "event-1",
            Map.of("topic", "payment", "id", "provider-payment-1"),
            "signature");

    PaymentDetails first = service.process(command);
    PaymentDetails duplicate = service.process(command);

    assertThat(first.id()).isEqualTo(duplicate.id());
    assertThat(events.claims).containsExactly("event-1");
    assertThat(provider.callbackCalls).isEqualTo(1);
  }

  private static final class InMemoryPaymentRepository implements PaymentRepository {
    private final List<Payment> payments = new ArrayList<>();

    private InMemoryPaymentRepository(Payment... initial) {
      payments.addAll(List.of(initial));
    }

    @Override
    public Optional<Payment> findByTenantIdAndId(UUID tenantId, UUID paymentId) {
      return payments.stream()
          .filter(payment -> payment.tenantId().equals(tenantId))
          .filter(payment -> payment.id().equals(paymentId))
          .findFirst();
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
      return payment;
    }
  }

  private static final class RecordingWebhookEvents implements PaymentWebhookEventRepository {
    private final List<String> claims = new ArrayList<>();

    @Override
    public boolean claim(UUID tenantId, String provider, String eventId) {
      if (claims.contains(eventId)) {
        return false;
      }
      claims.add(eventId);
      return true;
    }
  }

  private static final class RecordingProvider implements PaymentProvider {
    private int callbackCalls;

    @Override
    public String name() {
      return "mercadopago";
    }

    @Override
    public PaymentResult initiate(
        String idempotencyKey, BigDecimal amount, String currency, String description) {
      return new PaymentResult("provider-payment-1", "PENDING", Map.of());
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
      callbackCalls++;
      return new PaymentResult("provider-payment-1", "CAPTURED", Map.of());
    }
  }
}
