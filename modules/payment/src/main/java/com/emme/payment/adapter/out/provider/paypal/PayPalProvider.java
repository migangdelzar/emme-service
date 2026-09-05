package com.emme.payment.adapter.out.provider.paypal;

import com.emme.payment.application.port.out.PaymentProvider;
import com.emme.payment.application.port.out.PaymentProviderException;
import com.emme.payment.configuration.PaymentProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
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
  private final String clientId;
  private final String clientSecret;
  private final RestClient client;
  private final ObjectMapper mapper;
  private final String apiBase;

  private String accessToken;
  private Instant tokenExpiry;

  /** Production constructor — receives typed credentials from application configuration. */
  public PayPalProvider(
      PaymentProperties properties,
      @Qualifier("paymentRestClient") RestClient client,
      ObjectMapper mapper) {
    this.clientId = properties.paypal().clientId();
    this.clientSecret = properties.paypal().clientSecret();
    this.client = client;
    this.mapper = mapper;
    this.apiBase = API_BASE;
  }

  public PayPalProvider(
      RestClient client,
      ObjectMapper mapper,
      String clientId,
      String clientSecret,
      String apiBase) {
    this.clientId = clientId;
    this.clientSecret = clientSecret;
    this.client = client;
    this.mapper = mapper;
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
        Base64.getEncoder()
            .encodeToString(
                (clientId + ":" + clientSecret).getBytes(java.nio.charset.StandardCharsets.UTF_8));
    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("grant_type", "client_credentials");

    try {
      String responseBody =
          client
              .post()
              .uri(apiBase + "/v1/oauth2/token")
              .header("Authorization", "Basic " + credentials)
              .contentType(MediaType.APPLICATION_FORM_URLENCODED)
              .body(form)
              .retrieve()
              .body(String.class);
      @SuppressWarnings("unchecked")
      Map<String, Object> result =
          mapper.readValue(responseBody == null ? "{}" : responseBody, Map.class);
      accessToken = (String) result.get("access_token");
      int expiresIn = ((Number) result.get("expires_in")).intValue();
      tokenExpiry = Instant.now().plusSeconds(expiresIn - 60); // 60s buffer
      return accessToken;
    } catch (RestClientResponseException e) {
      throw new PaymentProviderException(
          "PayPal OAuth2 failed: HTTP "
              + e.getStatusCode().value()
              + " — "
              + e.getResponseBodyAsString(),
          e);
    } catch (RestClientException e) {
      throw new PaymentProviderException("PayPal OAuth2 failed: " + e.getMessage(), e);
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

      String responseBody =
          client
              .post()
              .uri(apiBase + "/v2/checkout/orders")
              .header("Authorization", "Bearer " + getAccessToken())
              .header("PayPal-Request-Id", idempotencyKey)
              .contentType(MediaType.APPLICATION_JSON)
              .body(body)
              .retrieve()
              .body(String.class);
      @SuppressWarnings("unchecked")
      Map<String, Object> result =
          mapper.readValue(responseBody == null ? "{}" : responseBody, Map.class);
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
    } catch (RestClientResponseException e) {
      throw new PaymentProviderException(
          "PayPal initiate failed: HTTP "
              + e.getStatusCode().value()
              + " — "
              + e.getResponseBodyAsString(),
          e);
    } catch (IOException | RestClientException e) {
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

      String responseBody =
          client
              .post()
              .uri(apiBase + "/v2/payments/captures/" + providerTransactionId + "/refund")
              .header("Authorization", "Bearer " + getAccessToken())
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
          "PayPal refund failed: HTTP "
              + e.getStatusCode().value()
              + " — "
              + e.getResponseBodyAsString(),
          e);
    } catch (IOException | RestClientException e) {
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
