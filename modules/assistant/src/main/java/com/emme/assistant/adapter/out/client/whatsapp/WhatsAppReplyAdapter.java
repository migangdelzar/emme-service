package com.emme.assistant.adapter.out.client.whatsapp;

import com.emme.assistant.ai.configuration.AiHttpClient;
import com.emme.assistant.application.port.out.WhatsAppReplyPort;
import com.emme.assistant.configuration.WhatsAppProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Map;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

/** Meta Graph API adapter implementing the Assistant WhatsApp reply port. */
@Component
@ConditionalOnExpression("not '${app.whatsapp.verify-token:}'.isEmpty()")
public class WhatsAppReplyAdapter implements WhatsAppReplyPort {

  private static final Logger log = LoggerFactory.getLogger(WhatsAppReplyAdapter.class);

  private final WhatsAppProperties properties;
  private final AiHttpClient httpClient;
  private final ObjectMapper objectMapper;

  public WhatsAppReplyAdapter(
      WhatsAppProperties properties, AiHttpClient httpClient, ObjectMapper objectMapper) {
    this.properties = properties;
    this.httpClient = httpClient;
    this.objectMapper = objectMapper;
  }

  @Override
  public void send(String recipient, String text) {
    if (properties.accessToken().isBlank()
        || properties.phoneNumberId().isBlank()
        || properties.apiBaseUrl().isBlank()) {
      log.warn("WhatsApp credentials not configured — cannot send reply");
      return;
    }

    try {
      String body =
          objectMapper.writeValueAsString(
              Map.of(
                  "messaging_product",
                  "whatsapp",
                  "recipient_type",
                  "individual",
                  "to",
                  recipient,
                  "type",
                  "text",
                  "text",
                  Map.of("body", text)));
      Request request =
          new Request.Builder()
              .url(properties.apiBaseUrl() + "/" + properties.phoneNumberId() + "/messages")
              .header("Authorization", "Bearer " + properties.accessToken())
              .header("Content-Type", "application/json")
              .post(RequestBody.create(body, MediaType.get("application/json")))
              .build();

      try (Response response = httpClient.newCall(request).execute()) {
        if (!response.isSuccessful()) {
          log.error(
              "WhatsApp send failed: {} - {}",
              response.code(),
              response.body() != null ? response.body().string() : "no body");
        } else {
          log.info("WhatsApp message sent to {}", recipient);
        }
      }
    } catch (IOException exception) {
      log.error("WhatsApp send error: {}", exception.getMessage());
    }
  }
}
