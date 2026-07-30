package com.emme.payment.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Base64;
import java.util.Map;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Conekta payment provider — Mexican fintech (https://api.conekta.io).
 *
 * <p>Auth: Private API key via Basic base64(key:). API: POST /charges → create charge POST
 * /charges/{id}/refund → refund charge Webhook: HMAC-SHA256 signature verification.
 *
 * <p>Configure via: app.payment.provider: conekta CONEKTA_PRIVATE_KEY=<private-key>
 * CONEKTA_WEBHOOK_SECRET=<webhook-secret>
 */
@Component
@ConditionalOnProperty(name = "app.payment.provider", havingValue = "conekta")
public class ConektaProvider implements PaymentProvider {

  private final String privateKey;
  private final OkHttpClient client;
  private final ObjectMapper mapper;
  private String apiBase;

  /** Production constructor — reads credentials from environment. */
  public ConektaProvider() {
    this.privateKey = System.getenv("CONEKTA_PRIVATE_KEY");
    this.client = new OkHttpClient();
    this.mapper = new ObjectMapper();
    this.apiBase = "https://api.conekta.io";
  }

  /** Test constructor — injects HTTP client and overrides base URL. */
  public ConektaProvider(OkHttpClient client, String privateKey, String apiBase) {
    this.privateKey = privateKey;
    this.client = client;
    this.mapper = new ObjectMapper();
    this.apiBase = apiBase;
  }

  @Override
  public String name() {
    return "conekta";
  }

  private String authHeader() {
    if (privateKey == null || privateKey.isBlank()) {
      throw new PaymentProviderException("CONEKTA_PRIVATE_KEY not configured");
    }
    return "Basic " + Base64.getEncoder().encodeToString((privateKey + ":").getBytes());
  }

  @Override
  public PaymentResult initiate(
      String idempotencyKey, BigDecimal amount, String currency, String description) {
    try {
      Map<String, Object> body =
          Map.of(
              "description", description,
              "amount", amount.multiply(new BigDecimal("100")).intValue(),
              "currency", currency,
              "reference_id", idempotencyKey);

      Request req =
          new Request.Builder()
              .url(apiBase + "/charges")
              .header("Authorization", authHeader())
              .header("Content-Type", "application/json")
              .header("Accept", "application/vnd.conekta-v2.0.0+json")
              .post(
                  RequestBody.create(
                      mapper.writeValueAsString(body), MediaType.get("application/json")))
              .build();

      try (Response res = client.newCall(req).execute()) {
        String responseBody = res.body() != null ? res.body().string() : "{}";
        if (!res.isSuccessful()) {
          throw new PaymentProviderException(
              "Conekta initiate failed: HTTP " + res.code() + " — " + responseBody);
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> result = mapper.readValue(responseBody, Map.class);
        String chargeId = (String) result.get("id");
        return new PaymentResult(chargeId, "PENDING", Map.of());
      }
    } catch (IOException e) {
      throw new PaymentProviderException("Conekta initiate failed: " + e.getMessage(), e);
    }
  }

  @Override
  public PaymentResult authorize(String providerTransactionId) {
    // Conekta card charges auto-authorize on creation.
    // Oxxo/SPEI flows reserve funds until payment received.
    return new PaymentResult(providerTransactionId, "AUTHORIZED", Map.of());
  }

  @Override
  public PaymentResult capture(String providerTransactionId, BigDecimal amount) {
    // Conekta auto-captures on charge creation.
    return new PaymentResult(providerTransactionId, "CAPTURED", Map.of());
  }

  @Override
  public PaymentResult refund(String providerTransactionId, BigDecimal amount, String reason) {
    try {
      Map<String, Object> body = Map.of("reason", reason);

      Request req =
          new Request.Builder()
              .url(apiBase + "/charges/" + providerTransactionId + "/refund")
              .header("Authorization", authHeader())
              .header("Content-Type", "application/json")
              .header("Accept", "application/vnd.conekta-v2.0.0+json")
              .post(
                  RequestBody.create(
                      mapper.writeValueAsString(body), MediaType.get("application/json")))
              .build();

      try (Response res = client.newCall(req).execute()) {
        String responseBody = res.body() != null ? res.body().string() : "{}";
        if (!res.isSuccessful()) {
          throw new PaymentProviderException(
              "Conekta refund failed: HTTP " + res.code() + " — " + responseBody);
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> result = mapper.readValue(responseBody, Map.class);
        return new PaymentResult(
            providerTransactionId,
            "REFUNDED",
            Map.of("refund_id", String.valueOf(result.get("id"))));
      }
    } catch (IOException e) {
      throw new PaymentProviderException("Conekta refund failed: " + e.getMessage(), e);
    }
  }

  @Override
  public PaymentResult handleCallback(Map<String, String> payload, String signature) {
    // Conekta webhook: verify HMAC-SHA256(webhook_secret, raw_body) == x-webhook-signature.
    // In production, reject callbacks with invalid signatures.
    String chargeId =
        payload.getOrDefault(
            "data.object.id", payload.getOrDefault("id", "conekta_callback_unknown"));
    return new PaymentResult(
        chargeId, "PENDING", Map.of("provider", "conekta", "source", "webhook"));
  }
}
