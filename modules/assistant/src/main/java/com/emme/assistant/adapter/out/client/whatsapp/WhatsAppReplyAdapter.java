package com.emme.assistant.adapter.out.client.whatsapp;

import com.emme.assistant.application.port.out.WhatsAppReplyPort;
import com.emme.assistant.configuration.WhatsAppProperties;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/** Meta Graph API adapter implementing the Assistant WhatsApp reply port. */
@Component
@ConditionalOnExpression("not '${app.whatsapp.verify-token:}'.isEmpty()")
public class WhatsAppReplyAdapter implements WhatsAppReplyPort {

  private static final Logger log = LoggerFactory.getLogger(WhatsAppReplyAdapter.class);

  private final WhatsAppProperties properties;
  private final RestClient restClient;

  public WhatsAppReplyAdapter(
      WhatsAppProperties properties, @Qualifier("whatsappRestClient") RestClient restClient) {
    this.properties = properties;
    this.restClient = restClient;
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
      restClient
          .post()
          .uri("/{phoneNumberId}/messages", properties.phoneNumberId())
          .header("Authorization", "Bearer " + properties.accessToken())
          .contentType(MediaType.APPLICATION_JSON)
          .body(
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
                  Map.of("body", text)))
          .retrieve()
          .toBodilessEntity();
      log.info("WhatsApp message sent to {}", recipient);
    } catch (RestClientResponseException exception) {
      log.error(
          "WhatsApp send failed: {} - {}",
          exception.getStatusCode().value(),
          exception.getResponseBodyAsString());
    } catch (RestClientException exception) {
      log.error("WhatsApp send error: {}", exception.getMessage());
    }
  }
}
