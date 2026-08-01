package com.emme.notification.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.notification")
public record NotificationProperties(
    Email email,
    Smtp smtp,
    Sendgrid sendgrid,
    Ses ses,
    Sms sms,
    Twilio twilio,
    Messagebird messagebird,
    Vonage vonage,
    Push push,
    Fcm fcm,
    Apns apns) {

  public record Email(String provider, String from) {}

  public record Smtp(String host, int port, String username, String password) {}

  public record Sendgrid(String apiKey) {}

  public record Ses(String accessKey, String secretKey, String region) {}

  public record Sms(String provider) {}

  public record Twilio(String accountSid, String authToken, String fromNumber) {}

  public record Messagebird(String apiKey, String originator) {}

  public record Vonage(String apiKey, String apiSecret, String fromNumber) {}

  public record Push(String provider) {}

  public record Fcm(String serviceAccount, String projectId) {}

  public record Apns(
      String keyId, String teamId, String privateKey, String bundleId, boolean sandbox) {}
}
