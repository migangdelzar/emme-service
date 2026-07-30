package com.emme.notification.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * MessageBird SMS provider via REST API.
 *
 * <p>Activated when app.notification.sms.provider=messagebird. Auth: AccessKey header.
 *
 * <p>Env vars: MESSAGEBIRD_API_KEY, MESSAGEBIRD_ORIGINATOR (optional, fallback "Emme")
 */
@Component
@ConditionalOnProperty(name = "app.notification.sms.provider", havingValue = "messagebird")
public class MessageBirdProvider implements SmsProvider {

  private static final Logger log = LoggerFactory.getLogger(MessageBirdProvider.class);
  private static final String PRODUCTION_API_BASE = "https://rest.messagebird.com";
  private static final MediaType JSON = MediaType.get("application/json");

  private final String apiKey;
  private final String originator;
  private final OkHttpClient client;
  private final String apiBase;
  private final ObjectMapper mapper;

  public MessageBirdProvider() {
    this(
        new OkHttpClient(),
        PRODUCTION_API_BASE,
        System.getenv("MESSAGEBIRD_API_KEY"),
        System.getenv("MESSAGEBIRD_ORIGINATOR"));
  }

  /** Package-private constructor for testing with custom API base, client, and credentials. */
  public MessageBirdProvider(
      OkHttpClient client, String apiBase, String apiKey, String originator) {
    this.apiKey = notBlank(apiKey) ? apiKey : null;
    this.originator = notBlank(originator) ? originator : "Emme";
    this.client = client;
    this.apiBase = apiBase;
    this.mapper = new ObjectMapper();
    log.info(
        "MessageBirdProvider initialized — apiBase={}, apiKeyPresent={}",
        apiBase,
        notBlank(apiKey));
  }

  private static boolean notBlank(String s) {
    return s != null && !s.isBlank();
  }

  @Override
  public String name() {
    return "messagebird";
  }

  @Override
  public String send(String to, String message) {
    if (apiKey == null || apiKey.isBlank()) {
      log.warn("MESSAGEBIRD_API_KEY not set — cannot send SMS");
      return "messagebird-error: API_KEY not configured";
    }

    try {
      Map<String, Object> jsonBody =
          Map.of(
              "recipients", List.of(to),
              "originator", originator,
              "body", message);

      Request request =
          new Request.Builder()
              .url(apiBase + "/messages")
              .header("Authorization", "AccessKey " + apiKey)
              .header("Content-Type", "application/json")
              .post(RequestBody.create(mapper.writeValueAsString(jsonBody), JSON))
              .build();

      try (Response response = client.newCall(request).execute()) {
        String responseBody = response.body() != null ? response.body().string() : "";
        if (!response.isSuccessful()) {
          log.warn("MessageBird API error: status={}, body={}", response.code(), responseBody);
          return "messagebird-error: HTTP " + response.code();
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> result = mapper.readValue(responseBody, Map.class);
        String msgId = (String) result.getOrDefault("id", "unknown");
        log.info("MessageBird SMS sent — id: {}, to: {}", msgId, to);
        return "messagebird-" + msgId;
      }
    } catch (IOException e) {
      log.error("MessageBird SMS send failed", e);
      return "messagebird-error: " + e.getMessage();
    }
  }

  @Override
  public boolean isMock() {
    return false;
  }
}
