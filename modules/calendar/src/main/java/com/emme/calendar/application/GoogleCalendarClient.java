package com.emme.calendar.application;

import com.emme.calendar.api.TokenSource;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Google Calendar API client using service account JWT authentication. Pure HTTP + java.security —
 * no Google SDK dependency.
 */
@Component
public class GoogleCalendarClient {

  private static final Logger log = LoggerFactory.getLogger(GoogleCalendarClient.class);

  private static final String PRODUCTION_TOKEN_URL = "https://oauth2.googleapis.com/token";
  private static final String PRODUCTION_FREE_BUSY_URL =
      "https://www.googleapis.com/calendar/v3/freeBusy";
  private static final String SCOPE = "https://www.googleapis.com/auth/calendar.readonly";
  private static final long TOKEN_CACHE_SECONDS = 59 * 60; // 59 min (1h with buffer)
  private static final MediaType FORM_URLENCODED =
      MediaType.get("application/x-www-form-urlencoded");
  private static final MediaType JSON = MediaType.get("application/json");

  private final OkHttpClient client;
  private final ObjectMapper mapper;
  private final String tokenUrl;
  private final String freeBusyUrl;
  private final String clientEmail;
  private final PrivateKey privateKey;
  private final boolean configured;

  private TokenSource userTokenSource;

  private String cachedToken;
  private long tokenExpiresAt;

  /**
   * Production constructor — reads GOOGLE_SA_JSON_BASE64 env var, with optional user OAuth token
   * source.
   */
  @Autowired
  public GoogleCalendarClient(Optional<TokenSource> userTokenSource) {
    this(
        new OkHttpClient(),
        new ObjectMapper(),
        System.getenv("GOOGLE_SA_JSON_BASE64"),
        PRODUCTION_TOKEN_URL,
        PRODUCTION_FREE_BUSY_URL);
    this.userTokenSource = userTokenSource.orElse(null);
  }

  /** Test constructor — inject HTTP client, base64-encoded SA JSON, and endpoint URLs. */
  public GoogleCalendarClient(
      OkHttpClient client,
      ObjectMapper mapper,
      String saJsonBase64,
      String tokenUrl,
      String freeBusyUrl) {
    this.client = client;
    this.mapper = mapper;
    this.tokenUrl = tokenUrl;
    this.freeBusyUrl = freeBusyUrl;
    this.userTokenSource = null;
    if (saJsonBase64 != null && !saJsonBase64.isBlank()) {
      try {
        String json = new String(Base64.getDecoder().decode(saJsonBase64), StandardCharsets.UTF_8);
        JsonNode node = mapper.readTree(json);
        this.clientEmail = node.get("client_email").asText();
        String privateKeyPem = node.get("private_key").asText();
        this.privateKey = parsePrivateKey(privateKeyPem);
        this.configured = true;
        this.tokenExpiresAt = 0;
        log.info("GoogleCalendarClient configured for {}", clientEmail);
      } catch (Exception e) {
        log.error("Failed to parse GOOGLE_SA_JSON_BASE64: {}", e.getMessage());
        throw new RuntimeException("Invalid Google service account JSON", e);
      }
    } else {
      log.warn("GOOGLE_SA_JSON_BASE64 not set — Google Calendar integration disabled");
      this.clientEmail = null;
      this.privateKey = null;
      this.configured = false;
    }
  }

  /** Whether the client has valid credentials (service account or user OAuth). */
  public boolean isConfigured() {
    return (userTokenSource != null && userTokenSource.isConfigured()) || configured;
  }

  /**
   * Get access token, preferring user OAuth when available.
   *
   * @param preferUser if true and userTokenSource is configured, use it; otherwise fall back to
   *     service account
   */
  public String getAccessToken(boolean preferUser) throws Exception {
    if (preferUser && userTokenSource != null && userTokenSource.isConfigured()) {
      return userTokenSource.getAccessToken();
    }
    return getServiceAccountAccessToken();
  }

  /**
   * Obtain OAuth2 access token via JWT bearer assertion (service account). Token is cached for 59
   * minutes.
   */
  private String getServiceAccountAccessToken() throws Exception {
    if (!configured) {
      throw new IllegalStateException("GoogleCalendarClient not configured");
    }
    long now = Instant.now().getEpochSecond();
    if (cachedToken != null && now < tokenExpiresAt) {
      return cachedToken;
    }
    String jwt = buildJwt();
    String body =
        "grant_type="
            + java.net.URLEncoder.encode(
                "urn:ietf:params:oauth:grant-type:jwt-bearer", StandardCharsets.UTF_8)
            + "&assertion="
            + java.net.URLEncoder.encode(jwt, StandardCharsets.UTF_8);

    Request request =
        new Request.Builder()
            .url(tokenUrl)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .post(RequestBody.create(body, FORM_URLENCODED))
            .build();

    try (Response response = client.newCall(request).execute()) {
      if (!response.isSuccessful()) {
        String errorBody = response.body() != null ? response.body().string() : "";
        throw new RuntimeException(
            "Token request failed: HTTP " + response.code() + " — " + errorBody);
      }
      String responseBody = response.body().string();
      JsonNode node = mapper.readTree(responseBody);
      cachedToken = node.get("access_token").asText();
      tokenExpiresAt = now + TOKEN_CACHE_SECONDS;
      log.debug("Access token obtained, cached until {}", Instant.ofEpochSecond(tokenExpiresAt));
      return cachedToken;
    }
  }

  /**
   * Query free/busy for a calendar in a time range.
   *
   * @return list of busy time ranges; empty list on failure or if not configured
   */
  public List<CalendarService.TimeRange> freeBusy(
      String calendarId, String timeMin, String timeMax) {
    if (!configured) {
      log.warn("GoogleCalendarClient not configured — returning empty free/busy");
      return Collections.emptyList();
    }
    try {
      String token = getServiceAccountAccessToken();
      String requestBody =
          mapper.writeValueAsString(
              Map.of(
                  "timeMin", timeMin,
                  "timeMax", timeMax,
                  "items", List.of(Map.of("id", calendarId))));

      Request request =
          new Request.Builder()
              .url(freeBusyUrl)
              .header("Authorization", "Bearer " + token)
              .header("Content-Type", "application/json")
              .post(RequestBody.create(requestBody, JSON))
              .build();

      try (Response response = client.newCall(request).execute()) {
        if (!response.isSuccessful()) {
          String errorBody = response.body() != null ? response.body().string() : "";
          log.error("Free/busy request failed: HTTP {} — {}", response.code(), errorBody);
          return Collections.emptyList();
        }
        return parseBusyTimes(response.body().string(), calendarId);
      }
    } catch (Exception e) {
      log.error("Failed to fetch free/busy: {}", e.getMessage(), e);
      return Collections.emptyList();
    }
  }

  // --- private helpers ---

  /** Build a signed JWT for service account authentication. */
  private String buildJwt() throws Exception {
    long now = Instant.now().getEpochSecond();
    long exp = now + 3600;

    String header =
        Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(mapper.writeValueAsBytes(Map.of("alg", "RS256", "typ", "JWT")));
    String payload =
        Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(
                mapper.writeValueAsBytes(
                    Map.of(
                        "iss", clientEmail,
                        "scope", SCOPE,
                        "aud", tokenUrl,
                        "iat", now,
                        "exp", exp)));

    String signingInput = header + "." + payload;
    Signature sig = Signature.getInstance("SHA256withRSA");
    sig.initSign(privateKey);
    sig.update(signingInput.getBytes(StandardCharsets.UTF_8));
    String signature = Base64.getUrlEncoder().withoutPadding().encodeToString(sig.sign());

    return signingInput + "." + signature;
  }

  /** Parse PEM-encoded PKCS#8 private key from Google service account JSON. */
  private PrivateKey parsePrivateKey(String pem) throws Exception {
    String key =
        pem.replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replaceAll("\\s+", "");
    byte[] keyBytes = Base64.getDecoder().decode(key);
    PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
    KeyFactory kf = KeyFactory.getInstance("RSA");
    return kf.generatePrivate(spec);
  }

  /** Parse free/busy API response into TimeRange list. */
  private List<CalendarService.TimeRange> parseBusyTimes(String responseBody, String calendarId)
      throws Exception {
    JsonNode root = mapper.readTree(responseBody);
    JsonNode calendars = root.get("calendars");
    if (calendars == null) {
      return Collections.emptyList();
    }
    JsonNode cal = calendars.get(calendarId);
    if (cal == null) {
      return Collections.emptyList();
    }
    JsonNode busy = cal.get("busy");
    if (busy == null || !busy.isArray()) {
      return Collections.emptyList();
    }

    List<CalendarService.TimeRange> ranges = new ArrayList<>();
    for (JsonNode slot : busy) {
      String startStr = slot.get("start").asText();
      String endStr = slot.get("end").asText();
      LocalTime start = LocalTime.from(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(startStr));
      LocalTime end = LocalTime.from(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(endStr));
      ranges.add(new CalendarService.TimeRange(start, end));
    }
    return ranges;
  }
}
