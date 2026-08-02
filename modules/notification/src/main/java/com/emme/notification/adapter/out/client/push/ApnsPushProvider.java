package com.emme.notification.adapter.out.client.push;

import com.emme.notification.configuration.NotificationProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
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
 * Apple Push Notification service (APNs) provider using pure HTTP + JWT. No Apple Push SDK
 * dependency. Authenticates via ES256-signed JWT token using the .p8 private key.
 *
 * <p>Configuration: app.notification.apns.*
 *
 * <p>Property: app.notification.push.provider=apns
 */
@Component
@ConditionalOnProperty(name = "app.notification.push.provider", havingValue = "apns")
public class ApnsPushProvider implements com.emme.notification.application.port.out.PushSender {

  private static final Logger log = LoggerFactory.getLogger(ApnsPushProvider.class);
  private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
  private static final String PROD_URL = "https://api.push.apple.com";
  private static final String SANDBOX_URL = "https://api.sandbox.push.apple.com";

  private final String apnsBase;
  private final String keyId;
  private final String teamId;
  private final String bundleId;
  private final PrivateKey privateKey;
  private final OkHttpClient client;
  private final ObjectMapper mapper;

  /** Production constructor — receives typed credentials from application configuration. */
  public ApnsPushProvider(NotificationProperties properties) {
    this(
        new OkHttpClient(),
        new ObjectMapper(),
        resolveApnsBase(properties.apns()),
        requireProperty(properties.apns().keyId(), "app.notification.apns.key-id"),
        requireProperty(properties.apns().teamId(), "app.notification.apns.team-id"),
        requireProperty(properties.apns().bundleId(), "app.notification.apns.bundle-id"),
        loadECPrivateKey(
            requireProperty(properties.apns().privateKey(), "app.notification.apns.private-key")));
  }

  /** Full constructor for testing — all values injected directly. */
  public ApnsPushProvider(
      OkHttpClient client,
      ObjectMapper mapper,
      String apnsBase,
      String keyId,
      String teamId,
      String bundleId,
      PrivateKey privateKey) {
    this.client = client;
    this.mapper = mapper;
    this.apnsBase = apnsBase;
    this.keyId = keyId;
    this.teamId = teamId;
    this.bundleId = bundleId;
    this.privateKey = privateKey;

    boolean sandbox = apnsBase != null && apnsBase.contains("sandbox");
    log.info(
        "APNs push provider initialized — team={} bundle={} sandbox={}", teamId, bundleId, sandbox);
  }

  private static String resolveApnsBase(NotificationProperties.Apns apns) {
    return apns.sandbox() ? SANDBOX_URL : PROD_URL;
  }

  // ── PushProvider contract ──

  @Override
  public String name() {
    return "apns";
  }

  @Override
  public String send(String deviceToken, String title, String body, Map<String, String> data) {
    try {
      String jwt = buildApnsJwt();
      String apnsId = sendRequest(jwt, deviceToken, title, body, data);
      log.info("APNs push sent — id={} token={}", apnsId, deviceToken);
      return apnsId;
    } catch (IOException e) {
      throw new PushProviderException("APNs push failed: " + e.getMessage(), e);
    }
  }

  // ── APNs HTTP request ──

  @SuppressWarnings("unchecked")
  String sendRequest(
      String jwt, String deviceToken, String title, String body, Map<String, String> data)
      throws IOException {
    Map<String, Object> payload = new LinkedHashMap<>();

    // aps
    Map<String, Object> aps = new LinkedHashMap<>();
    Map<String, String> alert = new LinkedHashMap<>();
    alert.put("title", title);
    alert.put("body", body);
    aps.put("alert", alert);
    payload.put("aps", aps);

    // custom data
    if (data != null && !data.isEmpty()) {
      payload.putAll(data);
    }

    String jsonBody = mapper.writeValueAsString(payload);
    String url = apnsBase + "/3/device/" + deviceToken;

    Request req =
        new Request.Builder()
            .url(url)
            .header("authorization", "bearer " + jwt)
            .header("apns-topic", bundleId)
            .header("apns-push-type", "alert")
            .header("content-type", "application/json")
            .post(RequestBody.create(jsonBody, JSON))
            .build();

    try (Response res = client.newCall(req).execute()) {
      if (!res.isSuccessful()) {
        String respBody = res.body() != null ? res.body().string() : "";
        throw new PushProviderException("APNs send failed: HTTP " + res.code() + " — " + respBody);
      }
      // APNs returns apns-id in response header on success
      String apnsId = res.header("apns-id");
      return apnsId != null ? apnsId : "unknown";
    }
  }

  // ── JWT token builder (ES256) ──

  public String buildApnsJwt() {
    try {
      long now = Instant.now().getEpochSecond();

      // Header
      String headerJson = mapper.writeValueAsString(Map.of("alg", "ES256", "kid", keyId));
      String headerB64 = base64UrlEncode(headerJson.getBytes(StandardCharsets.UTF_8));

      // Claims
      Map<String, Object> claims = new LinkedHashMap<>();
      claims.put("iss", teamId);
      claims.put("iat", now);

      String claimsJson = mapper.writeValueAsString(claims);
      String claimsB64 = base64UrlEncode(claimsJson.getBytes(StandardCharsets.UTF_8));

      String signingInput = headerB64 + "." + claimsB64;

      // Sign with ES256 (ECDSA using P-256 and SHA-256)
      Signature sig = Signature.getInstance("SHA256withECDSA");
      sig.initSign(privateKey);
      sig.update(signingInput.getBytes(StandardCharsets.UTF_8));
      String signatureB64 = base64UrlEncode(sig.sign());

      return signingInput + "." + signatureB64;
    } catch (Exception e) {
      throw new PushProviderException("Failed to build APNs JWT: " + e.getMessage(), e);
    }
  }

  // ── Helpers ──

  static String base64UrlEncode(byte[] data) {
    return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
  }

  static PrivateKey loadECPrivateKey(String base64Key) {
    try {
      byte[] keyBytes = Base64.getDecoder().decode(base64Key);
      PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
      KeyFactory kf = KeyFactory.getInstance("EC");
      return kf.generatePrivate(spec);
    } catch (Exception e) {
      throw new PushProviderException("Failed to load APNs private key: " + e.getMessage(), e);
    }
  }

  static String requireProperty(String value, String propertyName) {
    if (value == null || value.isBlank()) {
      throw new PushProviderException(
          "APNs property '" + propertyName + "' is required but not set");
    }
    return value;
  }
}
