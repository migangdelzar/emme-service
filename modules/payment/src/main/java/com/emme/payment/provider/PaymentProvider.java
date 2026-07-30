package com.emme.payment.provider;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Abstraction for payment providers (MercadoPago, PayPal, Conekta, Stripe, Mock). Implementations
 * handle charge lifecycle and webhook callbacks.
 */
public interface PaymentProvider {

  /** Provider identifier (e.g. "mercadopago", "paypal", "mock") */
  String name();

  /** Initiate a charge. Returns provider transaction ID. */
  PaymentResult initiate(
      String idempotencyKey, BigDecimal amount, String currency, String description);

  /** Authorize a pending charge (for two-step flows like MercadoPago) */
  PaymentResult authorize(String providerTransactionId);

  /** Capture an authorized charge */
  PaymentResult capture(String providerTransactionId, BigDecimal amount);

  /** Refund a captured charge */
  PaymentResult refund(String providerTransactionId, BigDecimal amount, String reason);

  /** Process incoming webhook callback */
  PaymentResult handleCallback(Map<String, String> payload, String signature);

  /** Whether this provider is a mock implementation */
  default boolean isMock() {
    return false;
  }

  /** Payment result DTO returned by provider operations */
  record PaymentResult(String providerTransactionId, String status, Map<String, String> metadata) {}
}
