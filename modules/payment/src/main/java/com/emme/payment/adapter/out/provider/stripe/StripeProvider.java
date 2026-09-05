package com.emme.payment.adapter.out.provider.stripe;

import com.emme.payment.application.port.out.PaymentProvider;
import com.emme.payment.application.port.out.PaymentProviderException;
import com.emme.payment.configuration.PaymentProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Stripe payment provider — global processor available in Mexico since 2019.
 *
 * <p>Auth: Bearer token (sk_live_... or sk_test_...). REST API: POST /v1/payment_intents (charges),
 * POST /v1/refunds (refunds). Webhook: stripe-signature header verified with webhook secret.
 *
 * <p>Configure via: app.payment.provider: stripe app.payment.stripe.secret-key: sk_...
 * app.payment.stripe.webhook-secret: whsec_...
 *
 * <p>Secrets are read from app.payment.stripe.* typed configuration.
 */
@Component
@ConditionalOnProperty(name = "app.payment.provider", havingValue = "stripe")
public class StripeProvider implements PaymentProvider {

  private static final String PRODUCTION_API_BASE = "https://api.stripe.com";
  private final String apiBase;
  private final String secretKey;
  private final String webhookSecret;
  private final RestClient client;
  private final ObjectMapper mapper;

  public StripeProvider(
      PaymentProperties props,
      @Qualifier("paymentRestClient") RestClient client,
      ObjectMapper mapper) {
    this(props, client, PRODUCTION_API_BASE, mapper);
  }

  /** Test constructor — accepts a capability-owned client and custom API base URL. */
  public StripeProvider(
      PaymentProperties props, RestClient client, String apiBase, ObjectMapper mapper) {
    this.secretKey = props.stripe().secretKey();
    this.webhookSecret = props.stripe().webhookSecret();
    this.client = client;
    this.apiBase = apiBase;
    this.mapper = mapper;
  }

  @Override
  public String name() {
    return "stripe";
  }

  @Override
  public PaymentResult initiate(
      String idempotencyKey, BigDecimal amount, String currency, String description) {
    try {
      int amountCents = amount.multiply(new BigDecimal("100")).intValueExact();
      MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
      form.add("amount", String.valueOf(amountCents));
      form.add("currency", currency.toLowerCase());
      form.add("description", description);
      String responseBody =
          client
              .post()
              .uri(apiBase + "/v1/payment_intents")
              .header("Authorization", "Bearer " + secretKey)
              .header("Idempotency-Key", idempotencyKey)
              .contentType(MediaType.APPLICATION_FORM_URLENCODED)
              .body(form)
              .retrieve()
              .body(String.class);
      @SuppressWarnings("unchecked")
      Map<String, Object> result =
          mapper.readValue(responseBody == null ? "" : responseBody, Map.class);
      String paymentIntentId = (String) result.get("id");
      String clientSecret = (String) result.get("client_secret");
      return new PaymentResult(
          paymentIntentId,
          "PENDING",
          Map.of("client_secret", clientSecret != null ? clientSecret : ""));
    } catch (RestClientResponseException e) {
      throw new PaymentProviderException(
          "Stripe initiate failed: HTTP "
              + e.getStatusCode().value()
              + " — "
              + e.getResponseBodyAsString(),
          e);
    } catch (IOException | RestClientException e) {
      throw new PaymentProviderException("Stripe initiate failed: " + e.getMessage(), e);
    }
  }

  @Override
  public PaymentResult authorize(String providerTransactionId) {
    // Stripe PaymentIntent is authorized on creation (capture_method=automatic by default).
    // Manual capture flows use capture_method=manual.
    return new PaymentResult(providerTransactionId, "AUTHORIZED", Map.of());
  }

  @Override
  public PaymentResult capture(String providerTransactionId, BigDecimal amount) {
    // Stripe PaymentIntent auto-captures by default. For manual capture:
    // POST /v1/payment_intents/{id}/capture
    return new PaymentResult(
        providerTransactionId, "CAPTURED", Map.of("capturedAmount", amount.toString()));
  }

  @Override
  public PaymentResult refund(String providerTransactionId, BigDecimal amount, String reason) {
    try {
      int amountCents = amount.multiply(new BigDecimal("100")).intValueExact();
      MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
      form.add("payment_intent", providerTransactionId);
      form.add("amount", String.valueOf(amountCents));
      form.add("reason", "requested_by_customer");
      String responseBody =
          client
              .post()
              .uri(apiBase + "/v1/refunds")
              .header("Authorization", "Bearer " + secretKey)
              .contentType(MediaType.APPLICATION_FORM_URLENCODED)
              .body(form)
              .retrieve()
              .body(String.class);
      @SuppressWarnings("unchecked")
      Map<String, Object> result =
          mapper.readValue(responseBody == null ? "" : responseBody, Map.class);
      return new PaymentResult(
          providerTransactionId, "REFUNDED", Map.of("refund_id", (String) result.get("id")));
    } catch (RestClientResponseException e) {
      throw new PaymentProviderException(
          "Stripe refund failed: HTTP "
              + e.getStatusCode().value()
              + " — "
              + e.getResponseBodyAsString(),
          e);
    } catch (IOException | RestClientException e) {
      throw new PaymentProviderException("Stripe refund failed: " + e.getMessage(), e);
    }
  }

  @Override
  public PaymentResult handleCallback(Map<String, String> payload, String signature) {
    // Stripe webhook signature verification:
    // HMAC-SHA256(webhook_secret, timestamp + "." + raw_body) should match signature.
    // Full verification requires raw body bytes from HttpServletRequest — done at webhook
    // controller layer.
    String eventId = payload.getOrDefault("id", "unknown");
    return new PaymentResult(eventId, "PENDING", Map.of("provider", "stripe", "source", "webhook"));
  }
}
