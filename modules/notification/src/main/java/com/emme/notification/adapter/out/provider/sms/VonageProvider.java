package com.emme.notification.adapter.out.provider.sms;

import com.emme.notification.configuration.NotificationHttpClient;
import com.emme.notification.configuration.NotificationProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

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
  private static final MediaType JSON = MediaType.get("application/json");

  private final String apiKey;
  private final String apiSecret;
  private final String fromNumber;
  private final NotificationHttpClient client;
  private final String apiBase;
  private final ObjectMapper mapper;

  public VonageProvider(
      NotificationProperties properties, NotificationHttpClient client, ObjectMapper mapper) {
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
      NotificationHttpClient client,
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
      return "vonage-error: API_KEY not configured";
    }
    if (apiSecret == null || apiSecret.isBlank()) {
      log.warn("VONAGE_API_SECRET not set — cannot send SMS");
      return "vonage-error: API_SECRET not configured";
    }

    try {
      Map<String, Object> jsonBody =
          Map.of(
              "api_key", apiKey,
              "api_secret", apiSecret,
              "from", fromNumber,
              "to", to,
              "text", message);

      Request request =
          new Request.Builder()
              .url(apiBase + "/sms/json")
              .header("Content-Type", "application/json")
              .post(RequestBody.create(mapper.writeValueAsString(jsonBody), JSON))
              .build();

      try (Response response = client.newCall(request).execute()) {
        String responseBody = response.body() != null ? response.body().string() : "";
        if (!response.isSuccessful()) {
          log.warn("Vonage API error: status={}, body={}", response.code(), responseBody);
          return "vonage-error: HTTP " + response.code();
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> result = mapper.readValue(responseBody, Map.class);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> messages = (List<Map<String, Object>>) result.get("messages");
        String msgId =
            messages != null && !messages.isEmpty()
                ? (String) messages.get(0).getOrDefault("message-id", "unknown")
                : "unknown";
        log.info("Vonage SMS sent — id: {}, to: {}", msgId, to);
        return "vonage-" + msgId;
      }
    } catch (IOException e) {
      log.error("Vonage SMS send failed", e);
      return "vonage-error: " + e.getMessage();
    }
  }

  @Override
  public boolean isMock() {
    return false;
  }
}
