package com.emme.payment.adapter.out.provider;

import com.emme.payment.application.port.out.PaymentProvider;
import com.emme.payment.application.port.out.PaymentProviderException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Always-available mock provider. Simulates payment flow transitions without making external API
 * calls. Used in dev/test or when no real provider is configured.
 */
@Component
public class MockPaymentProvider implements PaymentProvider {

  private final Map<String, String> stateStore = new HashMap<>();

  @Override
  public String name() {
    return "mock";
  }

  @Override
  public PaymentResult initiate(
      String idempotencyKey, BigDecimal amount, String currency, String description) {
    String txnId = "mock_txn_" + UUID.randomUUID();
    stateStore.put(txnId, "PENDING");
    return new PaymentResult(
        txnId,
        "PENDING",
        Map.of(
            "idempotencyKey", idempotencyKey, "amount", amount.toString(), "currency", currency));
  }

  @Override
  public PaymentResult authorize(String providerTransactionId) {
    String current = stateStore.get(providerTransactionId);
    if (current == null)
      throw new PaymentProviderException("Transaction not found: " + providerTransactionId);
    if (!"PENDING".equals(current))
      throw new PaymentProviderException("Cannot authorize transaction in state: " + current);
    stateStore.put(providerTransactionId, "AUTHORIZED");
    return new PaymentResult(providerTransactionId, "AUTHORIZED", Map.of());
  }

  @Override
  public PaymentResult capture(String providerTransactionId, BigDecimal amount) {
    String current = stateStore.get(providerTransactionId);
    if (current == null)
      throw new PaymentProviderException("Transaction not found: " + providerTransactionId);
    if (!"AUTHORIZED".equals(current))
      throw new PaymentProviderException("Cannot capture transaction in state: " + current);
    stateStore.put(providerTransactionId, "CAPTURED");
    return new PaymentResult(
        providerTransactionId, "CAPTURED", Map.of("capturedAmount", amount.toString()));
  }

  @Override
  public PaymentResult refund(String providerTransactionId, BigDecimal amount, String reason) {
    String current = stateStore.get(providerTransactionId);
    if (current == null)
      throw new PaymentProviderException("Transaction not found: " + providerTransactionId);
    if (!"CAPTURED".equals(current))
      throw new PaymentProviderException("Cannot refund transaction in state: " + current);
    stateStore.put(providerTransactionId, "REFUNDED");
    return new PaymentResult(
        providerTransactionId,
        "REFUNDED",
        Map.of("refundAmount", amount.toString(), "reason", reason));
  }

  @Override
  public PaymentResult handleCallback(Map<String, String> payload, String signature) {
    String txnId =
        payload.getOrDefault("providerTransactionId", "mock_txn_callback_" + UUID.randomUUID());
    String status = payload.getOrDefault("status", "PENDING");
    stateStore.put(txnId, status);
    return new PaymentResult(txnId, status, Map.of("source", "webhook"));
  }

  @Override
  public boolean isMock() {
    return true;
  }
}
