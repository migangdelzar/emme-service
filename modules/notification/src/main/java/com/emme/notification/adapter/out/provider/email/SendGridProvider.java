package com.emme.notification.adapter.out.provider.email;

import com.emme.notification.configuration.NotificationProperties;
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
 * SendGrid Mail Send API v3 integration.
 *
 * <p>Auth: Bearer token via app.notification.sendgrid.api-key. API: POST
 * https://api.sendgrid.com/v3/mail/send
 *
 * <p>Configure via: app.notification.email.provider: sendgrid
 */
@Component
@ConditionalOnProperty(name = "app.notification.email.provider", havingValue = "sendgrid")
public class SendGridProvider implements com.emme.notification.application.port.out.EmailSender {

  private static final Logger log = LoggerFactory.getLogger(SendGridProvider.class);

  private final String apiKey;
  private final RestClient client;
  private String apiBase;

  /** Production constructor — receives typed credentials from application configuration. */
  public SendGridProvider(
      NotificationProperties properties, @Qualifier("notificationRestClient") RestClient client) {
    this(client, properties.sendgrid().apiKey(), "https://api.sendgrid.com");
  }

  public SendGridProvider(RestClient client, String apiKey, String apiBase) {
    this.apiKey = apiKey;
    this.client = client;
    this.apiBase = apiBase;
  }

  @Override
  public String name() {
    return "sendgrid";
  }

  @Override
  public String send(String to, String subject, String body, String html) {
    if (apiKey == null || apiKey.isBlank()) {
      throw new EmailProviderException("SENDGRID_API_KEY not configured");
    }

    try {
      Map<String, Object> personalization =
          Map.of("to", List.of(Map.of("email", to)), "subject", subject);

      Map<String, Object> from =
          Map.of(
              "email", "noreply@emme.app",
              "name", "Emme");

      Map<String, Object> payload =
          Map.of(
              "personalizations",
              List.of(personalization),
              "from",
              from,
              "subject",
              subject,
              "content",
              List.of(
                  Map.of("type", "text/plain", "value", body),
                  Map.of("type", "text/html", "value", html != null ? html : body)));

      var response =
          client
              .post()
              .uri(apiBase + "/v3/mail/send")
              .header("Authorization", "Bearer " + apiKey)
              .contentType(MediaType.APPLICATION_JSON)
              .body(payload)
              .retrieve()
              .toBodilessEntity();
      String messageId = response.getHeaders().getFirst("X-Message-Id");
      log.info("SendGrid email sent: {} subject='{}' messageId={}", to, subject, messageId);
      return messageId != null ? messageId : "sendgrid-" + System.currentTimeMillis();
    } catch (RestClientResponseException e) {
      throw new EmailProviderException(
          "SendGrid send failed: HTTP "
              + e.getStatusCode().value()
              + " — "
              + e.getResponseBodyAsString(),
          e);
    } catch (RestClientException e) {
      throw new EmailProviderException("SendGrid send failed: " + e.getMessage(), e);
    }
  }
}
