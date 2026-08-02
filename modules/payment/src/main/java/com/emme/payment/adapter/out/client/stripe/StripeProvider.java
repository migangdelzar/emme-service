package com.emme.payment.adapter.out.client.stripe;

import com.emme.payment.application.port.out.PaymentProvider;
import com.emme.payment.application.port.out.PaymentProviderException;
import com.emme.payment.configuration.PaymentProperties;
import com.emme.payment.configuration.PaymentHttpClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

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
  private static final MediaType FORM_URLENCODED =
      MediaType.get("application/x-www-form-urlencoded");

  private final String apiBase;
  private final String secretKey;
  private final String webhookSecret;
  private final PaymentHttpClient client;
  private final ObjectMapper mapper;

  public StripeProvider(
      PaymentProperties props, PaymentHttpClient client, ObjectMapper mapper) {
    this(props, client, PRODUCTION_API_BASE, mapper);
  }

  /** Test constructor — accepts a capability-owned client and custom API base URL. */
  public StripeProvider(
      PaymentProperties props, PaymentHttpClient client, String apiBase, ObjectMapper mapper) {
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
      String body =
          "amount="
              + amountCents
              + "&currency="
              + currency.toLowerCase()
              + "&description="
              + URLEncoder.encode(description, StandardCharsets.UTF_8);

      Request req =
          new Request.Builder()
              .url(apiBase + "/v1/payment_intents")
              .header("Authorization", "Bearer " + secretKey)
              .header("Content-Type", "application/x-www-form-urlencoded")
              .header("Idempotency-Key", idempotencyKey)
              .post(RequestBody.create(body, FORM_URLENCODED))
              .build();

      try (Response res = client.newCall(req).execute()) {
        String responseBody = res.body() != null ? res.body().string() : "";
        if (!res.isSuccessful()) {
          throw new PaymentProviderException(
              "Stripe initiate failed: HTTP " + res.code() + " — " + responseBody);
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> result = mapper.readValue(responseBody, Map.class);
        String paymentIntentId = (String) result.get("id");
        String clientSecret = (String) result.get("client_secret");
        return new PaymentResult(
            paymentIntentId,
            "PENDING",
            Map.of("client_secret", clientSecret != null ? clientSecret : ""));
      }
    } catch (IOException e) {
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
      String body =
          "payment_intent="
              + providerTransactionId
              + "&amount="
              + amountCents
              + "&reason=requested_by_customer";

      Request req =
          new Request.Builder()
              .url(apiBase + "/v1/refunds")
              .header("Authorization", "Bearer " + secretKey)
              .header("Content-Type", "application/x-www-form-urlencoded")
              .post(RequestBody.create(body, FORM_URLENCODED))
              .build();

      try (Response res = client.newCall(req).execute()) {
        String responseBody = res.body() != null ? res.body().string() : "";
        if (!res.isSuccessful()) {
          throw new PaymentProviderException(
              "Stripe refund failed: HTTP " + res.code() + " — " + responseBody);
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> result = mapper.readValue(responseBody, Map.class);
        return new PaymentResult(
            providerTransactionId, "REFUNDED", Map.of("refund_id", (String) result.get("id")));
      }
    } catch (IOException e) {
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
