package com.emme.payment.adapter.out.provider;

import com.emme.payment.application.port.out.PaymentProvider;
import com.emme.payment.application.port.out.PaymentProviderException;
import com.emme.payment.configuration.PaymentProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * MercadoPago Checkout API integration via REST.
 *
 * <p>Supported in: Mexico, Brazil, Argentina, Chile, Colombia, Peru, Uruguay. Auth: Bearer token
 * via app.payment.mercadopago.access-token. API: POST /checkout/preferences (Checkout Pro) Webhook:
 * POST /v1/payments/{id}/refunds
 *
 * <p>Configure via: app.payment.provider: mercadopago and app.payment.mercadopago.* typed
 * properties.
 */
@Component
@ConditionalOnProperty(name = "app.payment.provider", havingValue = "mercadopago")
public class MercadoPagoProvider implements PaymentProvider {

  private final String accessToken;
  private final String publicKey;
  private final OkHttpClient client;
  private final ObjectMapper mapper;
  private String apiBase;

  /** Production constructor — receives typed credentials from application configuration. */
  public MercadoPagoProvider(PaymentProperties properties) {
    this.accessToken = properties.mercadopago().accessToken();
    this.publicKey = properties.mercadopago().publicKey();
    this.client = new OkHttpClient();
    this.mapper = new ObjectMapper();
    this.apiBase = "https://api.mercadopago.com";
  }

  /** Test constructor — injects HTTP client and overrides base URL. */
  public MercadoPagoProvider(
      OkHttpClient client, String accessToken, String publicKey, String apiBase) {
    this.accessToken = accessToken;
    this.publicKey = publicKey;
    this.client = client;
    this.mapper = new ObjectMapper();
    this.apiBase = apiBase;
  }

  @Override
  public String name() {
    return "mercadopago";
  }

  @Override
  public PaymentResult initiate(
      String idempotencyKey, BigDecimal amount, String currency, String description) {
    if (accessToken == null || accessToken.isBlank()) {
      throw new PaymentProviderException("app.payment.mercadopago.access-token not configured");
    }

    try {
      Map<String, Object> body =
          Map.of(
              "items",
              List.of(
                  Map.of(
                      "title",
                      description,
                      "quantity",
                      1,
                      "unit_price",
                      amount.doubleValue(),
                      "currency_id",
                      currency)),
              "back_urls",
              Map.of(
                  "success", "https://emme.app/success",
                  "failure", "https://emme.app/failure"),
              "auto_return",
              "approved",
              "external_reference",
              idempotencyKey,
              "notification_url",
              "https://emme.app/api/v1/callbacks/payments");

      Request req =
          new Request.Builder()
              .url(apiBase + "/checkout/preferences")
              .header("Authorization", "Bearer " + accessToken)
              .header("Content-Type", "application/json")
              .header("X-Idempotency-Key", idempotencyKey)
              .post(
                  RequestBody.create(
                      mapper.writeValueAsString(body), MediaType.get("application/json")))
              .build();

      try (Response res = client.newCall(req).execute()) {
        String responseBody = res.body() != null ? res.body().string() : "{}";
        if (!res.isSuccessful()) {
          throw new PaymentProviderException(
              "MercadoPago initiate failed: HTTP " + res.code() + " — " + responseBody);
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> result = mapper.readValue(responseBody, Map.class);
        String preferenceId = (String) result.get("id");
        String initPoint = (String) result.get("init_point");
        return new PaymentResult(
            preferenceId, "PENDING", Map.of("init_point", initPoint != null ? initPoint : ""));
      }
    } catch (IOException e) {
      throw new PaymentProviderException("MercadoPago initiate failed: " + e.getMessage(), e);
    }
  }

  @Override
  public PaymentResult authorize(String providerTransactionId) {
    // MercadoPago Checkout Pro auto-authorizes on payment approval.
    // Two-step flows would use the /v1/payments API with capture=false.
    return new PaymentResult(providerTransactionId, "AUTHORIZED", Map.of());
  }

  @Override
  public PaymentResult capture(String providerTransactionId, BigDecimal amount) {
    // MercadoPago auto-captures on payment approval.
    // For pre-authorized flows, call PUT /v1/payments/{id} with capture=true.
    return new PaymentResult(providerTransactionId, "CAPTURED", Map.of());
  }

  @Override
  public PaymentResult refund(String providerTransactionId, BigDecimal amount, String reason) {
    if (accessToken == null || accessToken.isBlank()) {
      throw new PaymentProviderException("app.payment.mercadopago.access-token not configured");
    }

    try {
      Map<String, Object> body = Map.of("amount", amount.doubleValue());

      Request req =
          new Request.Builder()
              .url(apiBase + "/v1/payments/" + providerTransactionId + "/refunds")
              .header("Authorization", "Bearer " + accessToken)
              .header("Content-Type", "application/json")
              .post(
                  RequestBody.create(
                      mapper.writeValueAsString(body), MediaType.get("application/json")))
              .build();

      try (Response res = client.newCall(req).execute()) {
        String responseBody = res.body() != null ? res.body().string() : "{}";
        if (!res.isSuccessful()) {
          throw new PaymentProviderException(
              "MercadoPago refund failed: HTTP " + res.code() + " — " + responseBody);
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> result = mapper.readValue(responseBody, Map.class);
        return new PaymentResult(
            providerTransactionId,
            "REFUNDED",
            Map.of("refund_id", String.valueOf(result.get("id"))));
      }
    } catch (IOException e) {
      throw new PaymentProviderException("MercadoPago refund failed: " + e.getMessage(), e);
    }
  }

  @Override
  public PaymentResult handleCallback(Map<String, String> payload, String signature) {
    String topic = payload.getOrDefault("topic", "payment");
    String paymentId = payload.getOrDefault("id", payload.getOrDefault("data.id", "unknown"));

    // In production, verify x-signature header:
    //   HMAC-SHA256(webhook_secret, raw_body) should match signature.
    // MercadoPago sends two topics: 'payment' (payment status changes)
    // and 'merchant_order' (order updates).
    return new PaymentResult(
        paymentId, "PENDING", Map.of("topic", topic, "source", "mercadopago_webhook"));
  }
}
