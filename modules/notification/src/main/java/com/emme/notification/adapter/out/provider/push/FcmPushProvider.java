package com.emme.notification.adapter.out.provider.push;

import com.emme.notification.configuration.NotificationProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
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
 * Firebase Cloud Messaging (FCM) push provider using pure HTTP + OAuth2. No Firebase Admin SDK
 * dependency. Authenticates via service account JWT assertion exchanged for an OAuth2 access token.
 *
 * <p>Configuration: app.notification.fcm.service-account and app.notification.fcm.project-id. The
 * project ID falls back to the service-account JSON when it is not configured.
 *
 * <p>Property: app.notification.push.provider=fcm
 */
@Component
@ConditionalOnProperty(name = "app.notification.push.provider", havingValue = "fcm")
public class FcmPushProvider implements com.emme.notification.application.port.out.PushSender {

  private static final Logger log = LoggerFactory.getLogger(FcmPushProvider.class);
  static final String DEFAULT_TOKEN_URL = "https://oauth2.googleapis.com/token";
  private static final String DEFAULT_FCM_URL =
      "https://fcm.googleapis.com/v1/projects/%s/messages:send";
  private static final String SCOPE = "https://www.googleapis.com/auth/firebase.messaging";

  private final String projectId;
  private final String clientEmail;
  private final PrivateKey privateKey;
  private final String tokenUrl;
  private final String fcmUrl;
  private final RestClient client;
  private final ObjectMapper mapper;

  /** Production constructor — receives typed credentials from application configuration. */
  public FcmPushProvider(
      NotificationProperties properties,
      @Qualifier("notificationRestClient") RestClient client,
      ObjectMapper mapper) {
    this(
        client,
        mapper,
        DEFAULT_TOKEN_URL,
        null,
        loadClientEmail(properties.fcm(), mapper),
        loadProjectId(properties.fcm(), mapper),
        loadPrivateKey(properties.fcm(), mapper));
  }

  /** Full constructor for testing — all values injected directly. */
  public FcmPushProvider(
      RestClient client,
      ObjectMapper mapper,
      String tokenUrl,
      String fcmBaseUrl,
      String clientEmail,
      String projectId,
      PrivateKey privateKey) {
    this.client = client;
    this.mapper = mapper;
    this.tokenUrl = tokenUrl;
    this.clientEmail = clientEmail;
    this.projectId = projectId;
    this.privateKey = privateKey;
    this.fcmUrl = fcmBaseUrl != null ? fcmBaseUrl : String.format(DEFAULT_FCM_URL, this.projectId);

    log.info("FCM push provider initialized — project={} email={}", projectId, clientEmail);
  }

  private static String loadClientEmail(
      NotificationProperties.Fcm properties, ObjectMapper mapper) {
    return safeGet(loadServiceAccount(properties, mapper), "client_email", String.class);
  }

  private static String loadProjectId(NotificationProperties.Fcm properties, ObjectMapper mapper) {
    Map<String, Object> sa = loadServiceAccount(properties, mapper);
    return properties.projectId() != null && !properties.projectId().isBlank()
        ? properties.projectId()
        : safeGet(sa, "project_id", String.class);
  }

  private static PrivateKey loadPrivateKey(
      NotificationProperties.Fcm properties, ObjectMapper mapper) {
    return loadPrivateKey(
        safeGet(loadServiceAccount(properties, mapper), "private_key", String.class));
  }

  /** Parses service account JSON from typed application configuration. */
  private static Map<String, Object> loadServiceAccount(
      NotificationProperties.Fcm properties, ObjectMapper mapper) {
    String saBase64 = properties.serviceAccount();
    if (saBase64 == null || saBase64.isBlank()) {
      throw new PushProviderException(
          "app.notification.fcm.service-account is required for FCM push provider");
    }
    try {
      byte[] json = Base64.getDecoder().decode(saBase64);
      @SuppressWarnings("unchecked")
      Map<String, Object> parsed = mapper.readValue(json, Map.class);
      return parsed;
    } catch (IOException e) {
      throw new PushProviderException(
          "Failed to parse FCM_SERVICE_ACCOUNT_BASE64 as base64-encoded JSON: " + e.getMessage(),
          e);
    }
  }

  @Override
  public String name() {
    return "fcm";
  }

  @Override
  public String send(String deviceToken, String title, String body, Map<String, String> data) {
    try {
      String accessToken = fetchAccessToken();
      String messageId = sendMessage(accessToken, deviceToken, title, body, data);
      log.info("FCM push sent — id={} token={}", messageId, deviceToken);
      return messageId;
    } catch (IOException | RestClientException e) {
      throw new PushProviderException("FCM push failed: " + e.getMessage(), e);
    }
  }

  // ── OAuth2 token acquisition ──

  String fetchAccessToken() throws IOException {
    String assertion = buildJwtAssertion();

    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer");
    form.add("assertion", assertion);

    try {
      String respBody =
          client
              .post()
              .uri(tokenUrl)
              .contentType(MediaType.APPLICATION_FORM_URLENCODED)
              .body(form)
              .retrieve()
              .body(String.class);
      @SuppressWarnings("unchecked")
      Map<String, Object> tokenResp = mapper.readValue(respBody == null ? "" : respBody, Map.class);
      return (String) tokenResp.get("access_token");
    } catch (RestClientResponseException e) {
      throw new PushProviderException(
          "FCM OAuth2 token request failed: HTTP "
              + e.getStatusCode().value()
              + " — "
              + e.getResponseBodyAsString(),
          e);
    } catch (RestClientException | JsonProcessingException e) {
      throw new PushProviderException("FCM OAuth2 token request failed: " + e.getMessage(), e);
    }
  }

  // ── FCM message delivery ──

  @SuppressWarnings("unchecked")
  String sendMessage(
      String accessToken, String deviceToken, String title, String body, Map<String, String> data)
      throws IOException {
    Map<String, Object> payload = new LinkedHashMap<>();

    // message.token
    Map<String, Object> message = new LinkedHashMap<>();
    message.put("token", deviceToken);

    // message.notification
    Map<String, Object> notification = new LinkedHashMap<>();
    notification.put("title", title);
    notification.put("body", body);
    message.put("notification", notification);

    // message.data
    if (data != null && !data.isEmpty()) {
      Map<String, String> dataMap = new LinkedHashMap<>(data);
      message.put("data", dataMap);
    }

    payload.put("message", message);

    try {
      String respBody =
          client
              .post()
              .uri(fcmUrl)
              .header("Authorization", "Bearer " + accessToken)
              .contentType(MediaType.APPLICATION_JSON)
              .body(payload)
              .retrieve()
              .body(String.class);
      Map<String, Object> result = mapper.readValue(respBody == null ? "" : respBody, Map.class);
      return (String) result.get("name");
    } catch (RestClientResponseException e) {
      throw new PushProviderException(
          "FCM send failed: HTTP "
              + e.getStatusCode().value()
              + " — "
              + e.getResponseBodyAsString(),
          e);
    } catch (RestClientException | JsonProcessingException e) {
      throw new PushProviderException("FCM send failed: " + e.getMessage(), e);
    }
  }

  // ── JWT assertion builder ──

  String buildJwtAssertion() {
    try {
      long now = Instant.now().getEpochSecond();

      // Header
      String headerJson =
          mapper.writeValueAsString(
              Map.of(
                  "alg", "RS256",
                  "typ", "JWT"));
      String headerB64 = base64UrlEncode(headerJson.getBytes(StandardCharsets.UTF_8));

      // Claims
      Map<String, Object> claims = new LinkedHashMap<>();
      claims.put("iss", clientEmail);
      claims.put("scope", SCOPE);
      claims.put("aud", tokenUrl);
      claims.put("exp", now + 3600);
      claims.put("iat", now);

      String claimsJson = mapper.writeValueAsString(claims);
      String claimsB64 = base64UrlEncode(claimsJson.getBytes(StandardCharsets.UTF_8));

      String signingInput = headerB64 + "." + claimsB64;

      // Sign
      Signature sig = Signature.getInstance("SHA256withRSA");
      sig.initSign(privateKey);
      sig.update(signingInput.getBytes(StandardCharsets.UTF_8));
      String signatureB64 = base64UrlEncode(sig.sign());

      return signingInput + "." + signatureB64;
    } catch (Exception e) {
      throw new PushProviderException("Failed to build JWT assertion: " + e.getMessage(), e);
    }
  }

  // ── Helpers ──

  static String base64UrlEncode(byte[] data) {
    return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
  }

  static PrivateKey loadPrivateKey(String pkcs8Pem) {
    try {
      String key =
          pkcs8Pem
              .replace("-----BEGIN PRIVATE KEY-----", "")
              .replace("-----END PRIVATE KEY-----", "")
              .replaceAll("\\s", "");
      byte[] keyBytes = Base64.getDecoder().decode(key);
      PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
      KeyFactory kf = KeyFactory.getInstance("RSA");
      return kf.generatePrivate(spec);
    } catch (Exception e) {
      throw new PushProviderException("Failed to load FCM private key: " + e.getMessage(), e);
    }
  }

  @SuppressWarnings("unchecked")
  static <T> T safeGet(Map<String, Object> map, String key, Class<T> type) {
    Object val = map.get(key);
    if (val == null || !type.isInstance(val)) {
      throw new PushProviderException("FCM service account missing required field: " + key);
    }
    return (T) val;
  }
}
