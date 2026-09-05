package com.emme.notification.adapter.out.provider.sms;

import com.emme.notification.configuration.NotificationProperties;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * Twilio SMS provider via REST API.
 *
 * <p>Activated when app.notification.sms.provider=twilio. Auth: HTTP Basic with
 * AccountSID:AuthToken.
 *
 * <p>Configuration: app.notification.twilio.*
 */
@Component
@ConditionalOnProperty(name = "app.notification.sms.provider", havingValue = "twilio")
public class TwilioSmsProvider implements com.emme.notification.application.port.out.SmsSender {

  private static final Logger log = LoggerFactory.getLogger(TwilioSmsProvider.class);
  private static final String PRODUCTION_API_BASE = "https://api.twilio.com/2010-04-01";

  private final String accountSid;
  private final String authToken;
  private final String fromNumber;
  private final RestClient client;
  private final String apiBase;

  public TwilioSmsProvider(
      NotificationProperties properties, @Qualifier("notificationRestClient") RestClient client) {
    this(
        client,
        PRODUCTION_API_BASE,
        properties.twilio().accountSid(),
        properties.twilio().authToken(),
        properties.twilio().fromNumber());
  }

  /** Package-private constructor for testing with custom API base, HTTP client, and credentials. */
  public TwilioSmsProvider(
      RestClient client, String apiBase, String accountSid, String authToken, String fromNumber) {
    this.accountSid = notBlank(accountSid) ? accountSid : null;
    this.authToken = notBlank(authToken) ? authToken : null;
    this.fromNumber = notBlank(fromNumber) ? fromNumber : null;
    this.client = client;
    this.apiBase = apiBase;
    log.info(
        "TwilioSmsProvider initialized — apiBase={}, accountSidPresent={}, fromNumberPresent={}",
        apiBase,
        notBlank(accountSid),
        notBlank(fromNumber));
  }

  private static boolean notBlank(String s) {
    return s != null && !s.isBlank();
  }

  @Override
  public String name() {
    return "twilio";
  }

  @Override
  public String send(String to, String message) {
    if (accountSid == null || accountSid.isBlank()) {
      log.warn("TWILIO_ACCOUNT_SID not set — cannot send SMS");
      throw new SmsProviderException("Twilio ACCOUNT_SID not configured");
    }
    if (authToken == null || authToken.isBlank()) {
      log.warn("TWILIO_AUTH_TOKEN not set — cannot send SMS");
      throw new SmsProviderException("Twilio AUTH_TOKEN not configured");
    }
    if (fromNumber == null || fromNumber.isBlank()) {
      log.warn("TWILIO_FROM_NUMBER not set — cannot send SMS");
      throw new SmsProviderException("Twilio FROM_NUMBER not configured");
    }

    try {
      String credentials = accountSid + ":" + authToken;
      String basicAuth =
          "Basic "
              + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

      MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
      body.add("To", to);
      body.add("From", fromNumber);
      body.add("Body", message);

      String url = apiBase + "/Accounts/" + accountSid + "/Messages.json";
      String responseBody =
          client
              .post()
              .uri(url)
              .header("Authorization", basicAuth)
              .contentType(MediaType.APPLICATION_FORM_URLENCODED)
              .body(body)
              .retrieve()
              .body(String.class);
      String sid = extractSid(responseBody == null ? "" : responseBody);
      log.info("Twilio SMS sent — sid: {}, to: {}", sid, to);
      return "twilio-" + sid;
    } catch (RestClientResponseException e) {
      log.warn(
          "Twilio API error: status={}, body={}",
          e.getStatusCode().value(),
          e.getResponseBodyAsString());
      throw new SmsProviderException("Twilio send failed: HTTP " + e.getStatusCode().value());
    } catch (RestClientException e) {
      log.error("Twilio SMS send failed", e);
      throw new SmsProviderException("Twilio send failed: " + e.getMessage(), e);
    }
  }

  private static String extractSid(String json) {
    int idx = json.indexOf("\"sid\"");
    if (idx < 0) return "unknown";
    int start = json.indexOf("\"", idx + 6) + 1;
    int end = json.indexOf("\"", start);
    return start > 0 && end > start ? json.substring(start, end) : "unknown";
  }

  @Override
  public boolean isMock() {
    return false;
  }
}
