package com.emme.notification.adapter.out.client.email;

import com.emme.notification.configuration.NotificationProperties;
import com.emme.notification.configuration.NotificationHttpClient;
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
  private final NotificationHttpClient client;
  private final ObjectMapper mapper;
  private String apiBase;

  /** Production constructor — receives typed credentials from application configuration. */
  public SendGridProvider(
      NotificationProperties properties, NotificationHttpClient client, ObjectMapper mapper) {
    this.apiKey = properties.sendgrid().apiKey();
    this.client = client;
    this.mapper = mapper;
    this.apiBase = "https://api.sendgrid.com";
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

      Request req =
          new Request.Builder()
              .url(apiBase + "/v3/mail/send")
              .header("Authorization", "Bearer " + apiKey)
              .header("Content-Type", "application/json")
              .post(
                  RequestBody.create(
                      mapper.writeValueAsString(payload), MediaType.get("application/json")))
              .build();

      try (Response res = client.newCall(req).execute()) {
        String messageId = res.header("X-Message-Id");
        String responseBody = res.body() != null ? res.body().string() : "";

        if (!res.isSuccessful()) {
          throw new EmailProviderException(
              "SendGrid send failed: HTTP " + res.code() + " — " + responseBody);
        }

        log.info("SendGrid email sent: {} subject='{}' messageId={}", to, subject, messageId);
        return messageId != null ? messageId : "sendgrid-" + System.currentTimeMillis();
      }
    } catch (IOException e) {
      throw new EmailProviderException("SendGrid send failed: " + e.getMessage(), e);
    }
  }
}
