package com.emme.notification.adapter.out.provider.sms;

import com.emme.notification.configuration.NotificationProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Vonage (formerly Nexmo) SMS provider via REST API.
 *
 * <p>Activated when app.notification.sms.provider=vonage.
 *
 * <p>Configuration: app.notification.vonage.*
 */
@Component
@ConditionalOnProperty(name = "app.notification.sms.provider", havingValue = "vonage")
public class VonageProvider implements com.emme.notification.application.port.out.SmsSender {

  private static final Logger log = LoggerFactory.getLogger(VonageProvider.class);
  private static final String PRODUCTION_API_BASE = "https://rest.nexmo.com";
  private final String apiKey;
  private final String apiSecret;
  private final String fromNumber;
  private final RestClient client;
  private final String apiBase;
  private final ObjectMapper mapper;

  public VonageProvider(
      NotificationProperties properties,
      @Qualifier("notificationRestClient") RestClient client,
      ObjectMapper mapper) {
    this(
        client,
        PRODUCTION_API_BASE,
        properties.vonage().apiKey(),
        properties.vonage().apiSecret(),
        properties.vonage().fromNumber(),
        mapper);
  }

  /** Package-private constructor for testing with custom API base, client, and credentials. */
  public VonageProvider(
      RestClient client,
      String apiBase,
      String apiKey,
      String apiSecret,
      String fromNumber,
      ObjectMapper mapper) {
    this.apiKey = notBlank(apiKey) ? apiKey : null;
    this.apiSecret = notBlank(apiSecret) ? apiSecret : null;
    this.fromNumber = notBlank(fromNumber) ? fromNumber : "Emme";
    this.client = client;
    this.apiBase = apiBase;
    this.mapper = mapper;
    log.info(
        "VonageProvider initialized — apiBase={}, apiKeyPresent={}", apiBase, notBlank(apiKey));
  }

  private static boolean notBlank(String s) {
    return s != null && !s.isBlank();
  }

  @Override
  public String name() {
    return "vonage";
  }

  @Override
  public String send(String to, String message) {
    if (apiKey == null || apiKey.isBlank()) {
      log.warn("VONAGE_API_KEY not set — cannot send SMS");
      throw new SmsProviderException("Vonage API_KEY not configured");
    }
    if (apiSecret == null || apiSecret.isBlank()) {
      log.warn("VONAGE_API_SECRET not set — cannot send SMS");
      throw new SmsProviderException("Vonage API_SECRET not configured");
    }

    try {
      Map<String, Object> jsonBody =
          Map.of(
              "api_key", apiKey,
              "api_secret", apiSecret,
              "from", fromNumber,
              "to", to,
              "text", message);

      String responseBody =
          client
              .post()
              .uri(apiBase + "/sms/json")
              .contentType(MediaType.APPLICATION_JSON)
              .body(jsonBody)
              .retrieve()
              .body(String.class);
      @SuppressWarnings("unchecked")
      Map<String, Object> result =
          mapper.readValue(responseBody == null ? "" : responseBody, Map.class);
      @SuppressWarnings("unchecked")
      List<Map<String, Object>> messages = (List<Map<String, Object>>) result.get("messages");
      String msgId =
          messages != null && !messages.isEmpty()
              ? (String) messages.get(0).getOrDefault("message-id", "unknown")
              : "unknown";
      log.info("Vonage SMS sent — id: {}, to: {}", msgId, to);
      return "vonage-" + msgId;
    } catch (RestClientResponseException e) {
      log.warn(
          "Vonage API error: status={}, body={}",
          e.getStatusCode().value(),
          e.getResponseBodyAsString());
      throw new SmsProviderException("Vonage send failed: HTTP " + e.getStatusCode().value());
    } catch (RestClientException | JsonProcessingException e) {
      log.error("Vonage SMS send failed", e);
      throw new SmsProviderException("Vonage send failed: " + e.getMessage(), e);
    }
  }

  @Override
  public boolean isMock() {
    return false;
  }
}
