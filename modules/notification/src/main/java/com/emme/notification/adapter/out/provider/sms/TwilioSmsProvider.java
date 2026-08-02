package com.emme.notification.adapter.out.provider.sms;

import com.emme.notification.configuration.NotificationHttpClient;
import com.emme.notification.configuration.NotificationProperties;
import java.io.IOException;
import java.util.Base64;
import okhttp3.FormBody;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

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
  private final NotificationHttpClient client;
  private final String apiBase;

  public TwilioSmsProvider(NotificationProperties properties, NotificationHttpClient client) {
    this(
        client,
        PRODUCTION_API_BASE,
        properties.twilio().accountSid(),
        properties.twilio().authToken(),
        properties.twilio().fromNumber());
  }

  /** Package-private constructor for testing with custom API base, HTTP client, and credentials. */
  public TwilioSmsProvider(
      NotificationHttpClient client,
      String apiBase,
      String accountSid,
      String authToken,
      String fromNumber) {
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
      return "twilio-error: ACCOUNT_SID not configured";
    }
    if (authToken == null || authToken.isBlank()) {
      log.warn("TWILIO_AUTH_TOKEN not set — cannot send SMS");
      return "twilio-error: AUTH_TOKEN not configured";
    }
    if (fromNumber == null || fromNumber.isBlank()) {
      log.warn("TWILIO_FROM_NUMBER not set — cannot send SMS");
      return "twilio-error: FROM_NUMBER not configured";
    }

    try {
      String credentials = accountSid + ":" + authToken;
      String basicAuth = "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes());

      FormBody body =
          new FormBody.Builder().add("To", to).add("From", fromNumber).add("Body", message).build();

      String url = apiBase + "/Accounts/" + accountSid + "/Messages.json";
      Request request =
          new Request.Builder().url(url).header("Authorization", basicAuth).post(body).build();

      try (Response response = client.newCall(request).execute()) {
        String responseBody = response.body() != null ? response.body().string() : "";
        if (!response.isSuccessful()) {
          log.warn("Twilio API error: status={}, body={}", response.code(), responseBody);
          return "twilio-error: HTTP " + response.code();
        }
        String sid = extractSid(responseBody);
        log.info("Twilio SMS sent — sid: {}, to: {}", sid, to);
        return "twilio-" + sid;
      }
    } catch (IOException e) {
      log.error("Twilio SMS send failed", e);
      return "twilio-error: " + e.getMessage();
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
