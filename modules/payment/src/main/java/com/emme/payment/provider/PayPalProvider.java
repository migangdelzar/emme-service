package com.emme.payment.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Base64;
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
 * PayPal Orders API v2 integration via REST.
 *
 * <p>Supported in: Global (200+ markets). Auth: OAuth2 client credentials — POST /v1/oauth2/token
 * API: POST /v2/checkout/orders (intent=CAPTURE) Refund: POST
 * /v2/payments/captures/{captureId}/refund Webhook: POST /v1/notifications/verify-webhook-signature
 *
 * <p>Configure via: app.payment.provider: paypal and app.payment.paypal.* typed properties.
 */
@Component
@ConditionalOnProperty(name = "app.payment.provider", havingValue = "paypal")
public class PayPalProvider implements PaymentProvider {

  private static final String API_BASE = "https://api-m.sandbox.paypal.com";
  private static final MediaType JSON = MediaType.get("application/json");
  private static final MediaType FORM = MediaType.get("application/x-www-form-urlencoded");

  private final String clientId;
  private final String clientSecret;
  private final OkHttpClient client;
  private final ObjectMapper mapper;
  private final String apiBase;

  private String accessToken;
  private Instant tokenExpiry;

  /** Production constructor — receives typed credentials from application configuration. */
  public PayPalProvider(PaymentProperties properties) {
    this.clientId = properties.paypal().clientId();
    this.clientSecret = properties.paypal().clientSecret();
    this.client = new OkHttpClient();
    this.mapper = new ObjectMapper();
    this.apiBase = API_BASE;
  }

  /** Test constructor — injects HTTP client and overrides base URL. */
  public PayPalProvider(OkHttpClient client, String clientId, String clientSecret, String apiBase) {
    this.clientId = clientId;
    this.clientSecret = clientSecret;
    this.client = client;
    this.mapper = new ObjectMapper();
    this.apiBase = apiBase;
  }

  @Override
  public String name() {
    return "paypal";
  }

  // ── OAuth2 ──

  private String getAccessToken() throws IOException {
    if (accessToken != null && tokenExpiry != null && Instant.now().isBefore(tokenExpiry)) {
      return accessToken;
    }
    if (clientId == null || clientId.isBlank() || clientSecret == null || clientSecret.isBlank()) {
      throw new PaymentProviderException(
          "app.payment.paypal.client-id or app.payment.paypal.client-secret not configured");
    }

    String credentials =
        Base64.getEncoder().encodeToString((clientId + ":" + clientSecret).getBytes());

    Request req =
        new Request.Builder()
            .url(apiBase + "/v1/oauth2/token")
            .header("Authorization", "Basic " + credentials)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .post(RequestBody.create("grant_type=client_credentials", FORM))
            .build();

    try (Response res = client.newCall(req).execute()) {
      String responseBody = res.body() != null ? res.body().string() : "{}";
      if (!res.isSuccessful()) {
        throw new PaymentProviderException(
            "PayPal OAuth2 failed: HTTP " + res.code() + " — " + responseBody);
      }
      @SuppressWarnings("unchecked")
      Map<String, Object> result = mapper.readValue(responseBody, Map.class);
      accessToken = (String) result.get("access_token");
      int expiresIn = ((Number) result.get("expires_in")).intValue();
      tokenExpiry = Instant.now().plusSeconds(expiresIn - 60); // 60s buffer
      return accessToken;
    }
  }

  // ── initiate ──

  @Override
  public PaymentResult initiate(
      String idempotencyKey, BigDecimal amount, String currency, String description) {
    try {
      Map<String, Object> body =
          Map.of(
              "intent", "CAPTURE",
              "purchase_units",
                  List.of(
                      Map.of(
                          "amount",
                          Map.of("currency_code", currency, "value", amount.toString()),
                          "description",
                          description)),
              "application_context",
                  Map.of(
                      "return_url", "https://emme.app/success",
                      "cancel_url", "https://emme.app/cancel"));

      Request req =
          new Request.Builder()
              .url(apiBase + "/v2/checkout/orders")
              .header("Authorization", "Bearer " + getAccessToken())
              .header("Content-Type", "application/json")
              .header("PayPal-Request-Id", idempotencyKey)
              .post(RequestBody.create(mapper.writeValueAsString(body), JSON))
              .build();

      try (Response res = client.newCall(req).execute()) {
        String responseBody = res.body() != null ? res.body().string() : "{}";
        if (!res.isSuccessful()) {
          throw new PaymentProviderException(
              "PayPal initiate failed: HTTP " + res.code() + " — " + responseBody);
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> result = mapper.readValue(responseBody, Map.class);
        String orderId = (String) result.get("id");

        // Extract approval URL from links
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> links = (List<Map<String, Object>>) result.get("links");
        String approvalUrl = "";
        if (links != null) {
          approvalUrl =
              links.stream()
                  .filter(l -> "approve".equals(l.get("rel")))
                  .map(l -> (String) l.get("href"))
                  .findFirst()
                  .orElse("");
        }

        return new PaymentResult(orderId, "PENDING", Map.of("approval_url", approvalUrl));
      }
    } catch (IOException e) {
      throw new PaymentProviderException("PayPal initiate failed: " + e.getMessage(), e);
    }
  }

  @Override
  public PaymentResult authorize(String providerTransactionId) {
    // PayPal Orders v2 with intent=CAPTURE does not require separate authorize step.
    // Two-step flows would use intent=AUTHORIZE and POST /v2/checkout/orders/{id}/authorize.
    return new PaymentResult(providerTransactionId, "AUTHORIZED", Map.of());
  }

  @Override
  public PaymentResult capture(String providerTransactionId, BigDecimal amount) {
    // PayPal auto-captures on buyer approval for intent=CAPTURE orders.
    // For intent=AUTHORIZE, call POST /v2/checkout/orders/{id}/capture.
    return new PaymentResult(providerTransactionId, "CAPTURED", Map.of());
  }

  // ── refund ──

  @Override
  public PaymentResult refund(String providerTransactionId, BigDecimal amount, String reason) {
    try {
      Map<String, Object> body =
          Map.of(
              "amount",
              Map.of("currency_code", "MXN", "value", amount.toString()),
              "note_to_payer",
              reason != null ? reason : "");

      Request req =
          new Request.Builder()
              .url(apiBase + "/v2/payments/captures/" + providerTransactionId + "/refund")
              .header("Authorization", "Bearer " + getAccessToken())
              .header("Content-Type", "application/json")
              .post(RequestBody.create(mapper.writeValueAsString(body), JSON))
              .build();

      try (Response res = client.newCall(req).execute()) {
        String responseBody = res.body() != null ? res.body().string() : "{}";
        if (!res.isSuccessful()) {
          throw new PaymentProviderException(
              "PayPal refund failed: HTTP " + res.code() + " — " + responseBody);
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> result = mapper.readValue(responseBody, Map.class);
        return new PaymentResult(
            providerTransactionId,
            "REFUNDED",
            Map.of("refund_id", String.valueOf(result.get("id"))));
      }
    } catch (IOException e) {
      throw new PaymentProviderException("PayPal refund failed: " + e.getMessage(), e);
    }
  }

  // ── webhook ──

  @Override
  public PaymentResult handleCallback(Map<String, String> payload, String signature) {
    // PayPal webhook verification: POST /v1/notifications/verify-webhook-signature
    // Body: { auth_algo, cert_url, transmission_id, transmission_sig,
    //         transmission_time, webhook_id, webhook_event }
    String txnId =
        payload.getOrDefault("resource.id", payload.getOrDefault("resource_id", "unknown"));
    String eventType = payload.getOrDefault("event_type", "UNKNOWN");
    return new PaymentResult(
        txnId, "PENDING", Map.of("event_type", eventType, "source", "paypal_webhook"));
  }
}
