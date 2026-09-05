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
 * MessageBird SMS provider via REST API.
 *
 * <p>Activated when app.notification.sms.provider=messagebird. Auth: AccessKey header.
 *
 * <p>Configuration: app.notification.messagebird.*
 */
@Component
@ConditionalOnProperty(name = "app.notification.sms.provider", havingValue = "messagebird")
public class MessageBirdProvider implements com.emme.notification.application.port.out.SmsSender {

  private static final Logger log = LoggerFactory.getLogger(MessageBirdProvider.class);
  private static final String PRODUCTION_API_BASE = "https://rest.messagebird.com";
  private final String apiKey;
  private final String originator;
  private final RestClient client;
  private final String apiBase;
  private final ObjectMapper mapper;

  public MessageBirdProvider(
      NotificationProperties properties,
      @Qualifier("notificationRestClient") RestClient client,
      ObjectMapper mapper) {
    this(
        client,
        PRODUCTION_API_BASE,
        properties.messagebird().apiKey(),
        properties.messagebird().originator(),
        mapper);
  }

  /** Package-private constructor for testing with custom API base, client, and credentials. */
  public MessageBirdProvider(
      RestClient client, String apiBase, String apiKey, String originator, ObjectMapper mapper) {
    this.apiKey = notBlank(apiKey) ? apiKey : null;
    this.originator = notBlank(originator) ? originator : "Emme";
    this.client = client;
    this.apiBase = apiBase;
    this.mapper = mapper;
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
      throw new SmsProviderException("MessageBird API_KEY not configured");
    }

    try {
      Map<String, Object> jsonBody =
          Map.of(
              "recipients", List.of(to),
              "originator", originator,
              "body", message);

      String responseBody =
          client
              .post()
              .uri(apiBase + "/messages")
              .header("Authorization", "AccessKey " + apiKey)
              .contentType(MediaType.APPLICATION_JSON)
              .body(jsonBody)
              .retrieve()
              .body(String.class);
      @SuppressWarnings("unchecked")
      Map<String, Object> result =
          mapper.readValue(responseBody == null ? "" : responseBody, Map.class);
      String msgId = (String) result.getOrDefault("id", "unknown");
      log.info("MessageBird SMS sent — id: {}, to: {}", msgId, to);
      return "messagebird-" + msgId;
    } catch (RestClientResponseException e) {
      log.warn(
          "MessageBird API error: status={}, body={}",
          e.getStatusCode().value(),
          e.getResponseBodyAsString());
      throw new SmsProviderException("MessageBird send failed: HTTP " + e.getStatusCode().value());
    } catch (RestClientException | JsonProcessingException e) {
      log.error("MessageBird SMS send failed", e);
      throw new SmsProviderException("MessageBird send failed: " + e.getMessage(), e);
    }
  }

  @Override
  public boolean isMock() {
    return false;
  }
}
