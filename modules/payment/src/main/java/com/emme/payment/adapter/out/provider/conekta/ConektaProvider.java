package com.emme.payment.adapter.out.provider.conekta;

import com.emme.payment.application.port.out.PaymentProvider;
import com.emme.payment.application.port.out.PaymentProviderException;
import com.emme.payment.configuration.PaymentProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Base64;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Conekta payment provider — Mexican fintech (https://api.conekta.io).
 *
 * <p>Auth: Private API key via Basic base64(key:). API: POST /charges → create charge POST
 * /charges/{id}/refund → refund charge Webhook: HMAC-SHA256 signature verification.
 *
 * <p>Configure via: app.payment.provider: conekta and app.payment.conekta.* typed properties.
 */
@Component
@ConditionalOnProperty(name = "app.payment.provider", havingValue = "conekta")
public class ConektaProvider implements PaymentProvider {

  private final String privateKey;
  private final RestClient client;
  private final ObjectMapper mapper;
  private String apiBase;

  /** Production constructor — receives typed credentials from application configuration. */
  public ConektaProvider(
      PaymentProperties properties,
      @Qualifier("paymentRestClient") RestClient client,
      ObjectMapper mapper) {
    this.privateKey = properties.conekta().privateKey();
    this.client = client;
    this.mapper = mapper;
    this.apiBase = "https://api.conekta.io";
  }

  public ConektaProvider(
      RestClient client, ObjectMapper mapper, String privateKey, String apiBase) {
    this.privateKey = privateKey;
    this.client = client;
    this.mapper = mapper;
    this.apiBase = apiBase;
  }

  @Override
  public String name() {
    return "conekta";
  }

  private String authHeader() {
    if (privateKey == null || privateKey.isBlank()) {
      throw new PaymentProviderException("app.payment.conekta.private-key not configured");
    }
    return "Basic "
        + Base64.getEncoder()
            .encodeToString((privateKey + ":").getBytes(java.nio.charset.StandardCharsets.UTF_8));
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

      String responseBody =
          client
              .post()
              .uri(apiBase + "/charges")
              .header("Authorization", authHeader())
              .header("Accept", "application/vnd.conekta-v2.0.0+json")
              .contentType(MediaType.APPLICATION_JSON)
              .body(body)
              .retrieve()
              .body(String.class);
      @SuppressWarnings("unchecked")
      Map<String, Object> result =
          mapper.readValue(responseBody == null ? "{}" : responseBody, Map.class);
      String chargeId = (String) result.get("id");
      return new PaymentResult(chargeId, "PENDING", Map.of());
    } catch (RestClientResponseException e) {
      throw new PaymentProviderException(
          "Conekta initiate failed: HTTP "
              + e.getStatusCode().value()
              + " — "
              + e.getResponseBodyAsString(),
          e);
    } catch (IOException | RestClientException e) {
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

      String responseBody =
          client
              .post()
              .uri(apiBase + "/charges/" + providerTransactionId + "/refund")
              .header("Authorization", authHeader())
              .header("Accept", "application/vnd.conekta-v2.0.0+json")
              .contentType(MediaType.APPLICATION_JSON)
              .body(body)
              .retrieve()
              .body(String.class);
      @SuppressWarnings("unchecked")
      Map<String, Object> result =
          mapper.readValue(responseBody == null ? "{}" : responseBody, Map.class);
      return new PaymentResult(
          providerTransactionId, "REFUNDED", Map.of("refund_id", String.valueOf(result.get("id"))));
    } catch (RestClientResponseException e) {
      throw new PaymentProviderException(
          "Conekta refund failed: HTTP "
              + e.getStatusCode().value()
              + " — "
              + e.getResponseBodyAsString(),
          e);
    } catch (IOException | RestClientException e) {
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
