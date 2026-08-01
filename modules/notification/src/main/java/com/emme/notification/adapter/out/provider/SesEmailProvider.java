package com.emme.notification.adapter.out.provider;

import com.emme.notification.configuration.NotificationProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
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
 * AWS SES v2 email provider using Signature V4 auth (pure Java, no AWS SDK).
 *
 * <p>Credentials are read from app.notification.ses.* typed configuration.
 *
 * <p>API: POST https://email.{region}.amazonaws.com/v2/email/outbound-emails
 *
 * <p>Configure via: app.notification.email.provider: ses
 */
@Component
@ConditionalOnProperty(name = "app.notification.email.provider", havingValue = "ses")
public class SesEmailProvider implements com.emme.notification.application.port.out.EmailSender {

  private static final Logger log = LoggerFactory.getLogger(SesEmailProvider.class);
  private static final String SERVICE = "ses";
  private static final String CONTENT_TYPE = "application/json";
  private static final DateTimeFormatter DATE_FMT =
      DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC);
  private static final DateTimeFormatter DATE_ONLY_FMT =
      DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneOffset.UTC);

  private final String accessKey;
  private final String secretKey;
  private final String region;
  private final OkHttpClient client;
  private final ObjectMapper mapper;
  private String apiBase;

  /** Production constructor — receives typed credentials from application configuration. */
  public SesEmailProvider(NotificationProperties properties) {
    this.accessKey = properties.ses().accessKey();
    this.secretKey = properties.ses().secretKey();
    this.region = properties.ses().region();
    this.client = new OkHttpClient();
    this.mapper = new ObjectMapper();

    boolean configured =
        accessKey != null
            && !accessKey.isBlank()
            && secretKey != null
            && !secretKey.isBlank()
            && region != null
            && !region.isBlank();

    if (!configured) {
      log.warn(
          "AWS SES credentials not configured (AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY, AWS_REGION). "
              + "Emails will NOT be sent. Set app.notification.email.provider=mock for dev.");
    }

    this.apiBase =
        region != null && !region.isBlank() ? "https://email." + region + ".amazonaws.com" : null;
  }

  /** Test constructor — injects HTTP client and overrides base URL. */
  public SesEmailProvider(
      OkHttpClient client, String accessKey, String secretKey, String region, String apiBase) {
    this.accessKey = accessKey;
    this.secretKey = secretKey;
    this.region = region;
    this.client = client;
    this.mapper = new ObjectMapper();
    this.apiBase = apiBase;
  }

  @Override
  public String name() {
    return "ses";
  }

  @Override
  public String send(String to, String subject, String body, String html) {
    if (accessKey == null
        || accessKey.isBlank()
        || secretKey == null
        || secretKey.isBlank()
        || region == null
        || region.isBlank()) {
      throw new EmailProviderException(
          "AWS SES not configured: missing AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY, or AWS_REGION");
    }

    try {
      Map<String, Object> emailContent = new TreeMap<>();
      if (html != null && !html.isBlank()) {
        emailContent.put("Html", Map.of("Charset", "UTF-8", "Data", html));
      }
      emailContent.put("Text", Map.of("Charset", "UTF-8", "Data", body));

      Map<String, Object> message = new TreeMap<>();
      message.put("Subject", Map.of("Charset", "UTF-8", "Data", subject));
      message.put("Body", emailContent);

      Map<String, Object> payload =
          Map.of(
              "FromEmailAddress", "noreply@emme.app",
              "Destination", Map.of("ToAddresses", List.of(to)),
              "Content", Map.of("Simple", message));

      String jsonBody = mapper.writeValueAsString(payload);
      byte[] bodyBytes = jsonBody.getBytes(StandardCharsets.UTF_8);
      String bodyHash = sha256Hex(bodyBytes);

      String path = "/v2/email/outbound-emails";
      String host = region + ".amazonaws.com";
      String serviceHost = "email." + region + ".amazonaws.com";

      // Override host/url when using MockWebServer (test constructor sets apiBase)
      String effectiveApiBase = apiBase != null ? apiBase : "https://" + serviceHost;
      String effectiveHost =
          apiBase != null
              ? apiBase.replace("https://", "").replace("http://", "").replaceAll("/$", "")
              : host;

      Instant now = Instant.now();
      String amzDate = DATE_FMT.format(now);
      String dateStamp = DATE_ONLY_FMT.format(now);

      String authorization =
          buildSignatureV4(
              accessKey,
              secretKey,
              region,
              serviceHost,
              effectiveHost,
              path,
              bodyHash,
              amzDate,
              dateStamp);

      Request req =
          new Request.Builder()
              .url(effectiveApiBase + path)
              .header("Authorization", authorization)
              .header("Content-Type", CONTENT_TYPE)
              .header("X-Amz-Date", amzDate)
              .header("X-Amz-Content-Sha256", bodyHash)
              .post(RequestBody.create(jsonBody, MediaType.get(CONTENT_TYPE)))
              .build();

      try (Response res = client.newCall(req).execute()) {
        String responseBody = res.body() != null ? res.body().string() : "";
        String messageId = res.header("X-Amzn-Message-Id");

        if (!res.isSuccessful()) {
          throw new EmailProviderException(
              "AWS SES send failed: HTTP " + res.code() + " — " + responseBody);
        }

        log.info("AWS SES email sent: {} subject='{}' messageId={}", to, subject, messageId);
        return messageId != null ? messageId : "ses-" + System.currentTimeMillis();
      }
    } catch (IOException e) {
      throw new EmailProviderException("AWS SES send failed: " + e.getMessage(), e);
    }
  }

  // ── AWS Signature V4 ──

  public static String buildSignatureV4(
      String accessKey,
      String secretKey,
      String region,
      String serviceHost,
      String effectiveHost,
      String canonicalUri,
      String payloadHash,
      String amzDate,
      String dateStamp) {

    String credentialScope = dateStamp + "/" + region + "/" + SERVICE + "/aws4_request";

    String canonicalRequest =
        "POST\n"
            + canonicalUri
            + "\n\n"
            + "content-type:"
            + CONTENT_TYPE
            + "\n"
            + "host:"
            + serviceHost
            + "\n"
            + "x-amz-content-sha256:"
            + payloadHash
            + "\n"
            + "x-amz-date:"
            + amzDate
            + "\n"
            + "\n"
            + "content-type;host;x-amz-content-sha256;x-amz-date\n"
            + payloadHash;

    String stringToSign =
        "AWS4-HMAC-SHA256\n"
            + amzDate
            + "\n"
            + credentialScope
            + "\n"
            + sha256Hex(canonicalRequest.getBytes(StandardCharsets.UTF_8));

    byte[] signingKey = getSignatureKey(secretKey, dateStamp, region, SERVICE);
    String signature = hmacSha256Hex(signingKey, stringToSign);

    return "AWS4-HMAC-SHA256 "
        + "Credential="
        + accessKey
        + "/"
        + credentialScope
        + ", "
        + "SignedHeaders=content-type;host;x-amz-content-sha256;x-amz-date, "
        + "Signature="
        + signature;
  }

  public static byte[] getSignatureKey(
      String secretKey, String dateStamp, String region, String service) {
    byte[] kSecret = ("AWS4" + secretKey).getBytes(StandardCharsets.UTF_8);
    byte[] kDate = hmacSha256(kSecret, dateStamp);
    byte[] kRegion = hmacSha256(kDate, region);
    byte[] kService = hmacSha256(kRegion, service);
    return hmacSha256(kService, "aws4_request");
  }

  public static String sha256Hex(byte[] data) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      byte[] hash = md.digest(data);
      return HexFormat.of().formatHex(hash);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 not available", e);
    }
  }

  static byte[] hmacSha256(byte[] key, String data) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(key, "HmacSHA256"));
      return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
    } catch (Exception e) {
      throw new IllegalStateException("HMAC-SHA256 not available", e);
    }
  }

  static String hmacSha256Hex(byte[] key, String data) {
    return HexFormat.of().formatHex(hmacSha256(key, data));
  }
}
