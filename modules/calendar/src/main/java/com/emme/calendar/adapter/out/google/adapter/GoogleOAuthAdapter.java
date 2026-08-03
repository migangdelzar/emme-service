package com.emme.calendar.adapter.out.google.adapter;

import com.emme.calendar.adapter.out.google.model.PersonaType;
import com.emme.calendar.adapter.out.google.oauth.TokenEncryptionService;
import com.emme.calendar.adapter.out.persistence.entity.GoogleOAuthTokenEntity;
import com.emme.calendar.adapter.out.persistence.repository.SpringDataGoogleOAuthTokenRepository;
import com.emme.calendar.configuration.GoogleHttpClient;
import com.emme.calendar.configuration.GoogleOAuthConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import okhttp3.FormBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Core OAuth lifecycle for Google service accounts. Handles authorization URL construction, code
 * exchange, token refresh, encrypted storage, revocation, and connectivity checks.
 *
 * <p>Tokens are encrypted with {@link TokenEncryptionService} before persistence. Tenant and user
 * identity come from the current security context via {@code TenantContextHolder} and {@code
 * UserContextHolder}.
 */
@Service
@Transactional
public class GoogleOAuthAdapter {

  private static final Logger log = LoggerFactory.getLogger(GoogleOAuthAdapter.class);
  private static final String AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth";
  private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
  private static final String REVOKE_URL = "https://oauth2.googleapis.com/revoke";

  private static final String STAFF_SCOPES =
      "https://www.googleapis.com/auth/calendar.events https://www.googleapis.com/auth/spreadsheets";
  private static final String CLIENT_SCOPES = "https://www.googleapis.com/auth/calendar.events";

  private final GoogleOAuthConfig config;
  private final TokenEncryptionService encryption;
  private final SpringDataGoogleOAuthTokenRepository tokenRepo;
  private final GoogleHttpClient httpClient;
  private final ObjectMapper mapper;

  public GoogleOAuthAdapter(
      GoogleOAuthConfig config,
      TokenEncryptionService encryption,
      SpringDataGoogleOAuthTokenRepository tokenRepo,
      ObjectMapper mapper,
      GoogleHttpClient httpClient) {
    this.config = config;
    this.encryption = encryption;
    this.tokenRepo = tokenRepo;
    this.httpClient = httpClient;
    this.mapper = mapper;
  }

  /**
   * Build the Google OAuth consent URL with scopes determined by {@link PersonaType}.
   *
   * @param personaType determines which scopes to request
   * @param state opaque value echoed back by Google for CSRF protection
   * @return the full authorization URL to redirect the user to
   */
  public String buildAuthorizationUrl(PersonaType personaType, String state) {
    String scopes = personaType == PersonaType.STAFF ? STAFF_SCOPES : CLIENT_SCOPES;

    return AUTH_URL
        + "?client_id="
        + urlEncode(config.clientId())
        + "&redirect_uri="
        + urlEncode(config.redirectUri())
        + "&response_type=code"
        + "&scope="
        + urlEncode(scopes)
        + "&access_type=offline"
        + "&prompt=consent"
        + "&state="
        + urlEncode(state);
  }

  /**
   * Exchange an authorization code for access and refresh tokens.
   *
   * @param code the authorization code from Google's redirect
   * @return parsed token response
   * @throws RuntimeException if the HTTP call fails or the response cannot be parsed
   */
  public TokenResponse exchangeCode(String code) {
    RequestBody body =
        new FormBody.Builder()
            .add("code", code)
            .add("client_id", config.clientId())
            .add("client_secret", config.clientSecret())
            .add("redirect_uri", config.redirectUri())
            .add("grant_type", "authorization_code")
            .build();

    return postTokenRequest(body);
  }

  /**
   * Refresh an expired access token using the stored (decrypted) refresh token.
   *
   * @param encryptedRefreshToken the encrypted refresh token from the database
   * @return parsed token response (new access token, may include a new refresh token)
   * @throws RuntimeException if decryption or the HTTP call fails
   */
  public TokenResponse refreshAccessToken(String encryptedRefreshToken) {
    String refreshToken = encryption.decrypt(encryptedRefreshToken);

    RequestBody body =
        new FormBody.Builder()
            .add("refresh_token", refreshToken)
            .add("client_id", config.clientId())
            .add("client_secret", config.clientSecret())
            .add("grant_type", "refresh_token")
            .build();

    return postTokenRequest(body);
  }

  /**
   * Store or update encrypted OAuth tokens in the database.
   *
   * <p>If the user already has a token for the given persona type, it is updated in-place.
   * Otherwise a new entity is persisted.
   *
   * @param tenantId the tenant identifier
   * @param userId the Keycloak user subject
   * @param personaType STAFF or CLIENT
   * @param tokens the token response from Google
   */
  public void storeToken(
      UUID tenantId, String userId, PersonaType personaType, TokenResponse tokens) {
    Instant expiresAt = Instant.now().plusSeconds(tokens.expiresIn());

    Optional<GoogleOAuthTokenEntity> existing =
        tokenRepo.findByTenantIdAndUserIdAndPersonaType(tenantId, userId, personaType);

    if (existing.isPresent()) {
      GoogleOAuthTokenEntity token = existing.get();
      token.setAccessToken(encryption.encrypt(tokens.accessToken()));
      if (tokens.refreshToken() != null && !tokens.refreshToken().isEmpty()) {
        token.setRefreshToken(encryption.encrypt(tokens.refreshToken()));
      }
      token.setScopes(tokens.scope());
      token.setExpiresAt(expiresAt);
      if (tokens.email() != null) {
        token.setProviderEmail(tokens.email());
      }
      tokenRepo.save(token);
      log.info("Updated Google OAuth token for userId={} persona={}", userId, personaType);
    } else {
      GoogleOAuthTokenEntity token =
          new GoogleOAuthTokenEntity(
              tenantId,
              userId,
              personaType,
              encryption.encrypt(tokens.accessToken()),
              tokens.refreshToken() != null && !tokens.refreshToken().isEmpty()
                  ? encryption.encrypt(tokens.refreshToken())
                  : null,
              tokens.scope(),
              expiresAt);
      if (tokens.email() != null) {
        token.setProviderEmail(tokens.email());
      }
      tokenRepo.save(token);
      log.info("Created Google OAuth token for userId={} persona={}", userId, personaType);
    }
  }

  /**
   * Get a valid (non-expired) access token, refreshing it automatically if necessary.
   *
   * <p>If the token has expired and a refresh token is available, a new access token is obtained
   * from Google, encrypted, and persisted before returning it.
   *
   * @param tenantId the tenant identifier
   * @param userId the Keycloak user subject
   * @param personaType STAFF or CLIENT
   * @return a decrypted, valid access token
   * @throws IllegalStateException if no token or no refresh token is available
   */
  public String getValidAccessToken(UUID tenantId, String userId, PersonaType personaType) {
    GoogleOAuthTokenEntity token =
        tokenRepo
            .findByTenantIdAndUserIdAndPersonaType(tenantId, userId, personaType)
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "No Google OAuth token found for userId="
                            + userId
                            + " persona="
                            + personaType));

    if (!token.isExpired()) {
      return encryption.decrypt(token.getAccessToken());
    }

    if (token.getRefreshToken() == null || token.getRefreshToken().isBlank()) {
      throw new IllegalStateException(
          "Access token expired and no refresh token available for userId="
              + userId
              + " persona="
              + personaType);
    }

    log.info("Refreshing expired access token for userId={} persona={}", userId, personaType);

    TokenResponse refreshed = refreshAccessToken(token.getRefreshToken());
    Instant expiresAt = Instant.now().plusSeconds(refreshed.expiresIn());

    token.setAccessToken(encryption.encrypt(refreshed.accessToken()));
    if (refreshed.refreshToken() != null && !refreshed.refreshToken().isEmpty()) {
      token.setRefreshToken(encryption.encrypt(refreshed.refreshToken()));
    }
    if (refreshed.scope() != null && !refreshed.scope().isEmpty()) {
      token.setScopes(refreshed.scope());
    }
    token.setExpiresAt(expiresAt);
    tokenRepo.save(token);

    return refreshed.accessToken();
  }

  /**
   * Revoke the stored token at Google and delete it from the database.
   *
   * <p>If the token does not exist in the database, this method is a no-op.
   *
   * @param tenantId the tenant identifier
   * @param userId the Keycloak user subject
   * @param personaType STAFF or CLIENT
   */
  public void revokeToken(UUID tenantId, String userId, PersonaType personaType) {
    Optional<GoogleOAuthTokenEntity> maybeToken =
        tokenRepo.findByTenantIdAndUserIdAndPersonaType(tenantId, userId, personaType);

    if (maybeToken.isEmpty()) {
      log.info("No Google OAuth token to revoke for userId={} persona={}", userId, personaType);
      return;
    }

    GoogleOAuthTokenEntity token = maybeToken.get();

    if (token.getRefreshToken() != null && !token.getRefreshToken().isBlank()) {
      try {
        String decryptedRefresh = encryption.decrypt(token.getRefreshToken());
        String revokeUrl = REVOKE_URL + "?token=" + urlEncode(decryptedRefresh);
        Request request =
            new Request.Builder()
                .url(revokeUrl)
                .post(RequestBody.create(new byte[0], null))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
          if (response.isSuccessful()) {
            log.info("Revoked Google token for userId={} persona={}", userId, personaType);
          } else {
            log.warn(
                "Google revoke returned {} for userId={} persona={}: {}",
                response.code(),
                userId,
                personaType,
                response.body() != null ? response.body().string() : "");
          }
        }
      } catch (Exception e) {
        log.warn(
            "Failed to revoke Google token remotely for userId={} persona={}: {}",
            userId,
            personaType,
            e.getMessage());
      }
    }

    tokenRepo.delete(token);
    log.info("Deleted Google OAuth token from DB for userId={} persona={}", userId, personaType);
  }

  /**
   * Check whether a user has a connected Google account for the given persona type.
   *
   * @param tenantId the tenant identifier
   * @param userId the Keycloak user subject
   * @param personaType STAFF or CLIENT
   * @return {@code true} if a token exists in the database
   */
  public boolean isConnected(UUID tenantId, String userId, PersonaType personaType) {
    return tokenRepo
        .findByTenantIdAndUserIdAndPersonaType(tenantId, userId, personaType)
        .isPresent();
  }

  // ---------------------------------------------------------------------------
  // Private helpers
  // ---------------------------------------------------------------------------

  /** Execute a POST to Google's token endpoint and parse the JSON response. */
  private TokenResponse postTokenRequest(RequestBody body) {
    Request request =
        new Request.Builder()
            .url(TOKEN_URL)
            .post(body)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .build();

    try (Response response = httpClient.newCall(request).execute()) {
      if (!response.isSuccessful()) {
        String errorBody = response.body() != null ? response.body().string() : "";
        throw new RuntimeException(
            "Google token endpoint returned HTTP " + response.code() + ": " + errorBody);
      }

      String json = response.body() != null ? response.body().string() : "";
      return parseTokenResponse(json);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException("Failed to communicate with Google token endpoint", e);
    }
  }

  /** Parse Google's OAuth token JSON response into a {@link TokenResponse}. */
  private TokenResponse parseTokenResponse(String json) throws Exception {
    JsonNode node = mapper.readTree(json);

    if (node.has("error")) {
      throw new RuntimeException(
          "Google OAuth error: "
              + node.get("error").asText()
              + " — "
              + (node.has("error_description") ? node.get("error_description").asText() : ""));
    }

    return new TokenResponse(
        node.get("access_token").asText(),
        node.has("refresh_token") ? node.get("refresh_token").asText() : "",
        node.has("scope") ? node.get("scope").asText() : "",
        node.get("expires_in").asLong(),
        node.has("email") ? node.get("email").asText() : null);
  }

  private static String urlEncode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }

  /**
   * Parsed response from Google's OAuth token endpoint.
   *
   * @param accessToken the short-lived bearer token
   * @param refreshToken the long-lived refresh token (only in initial exchange)
   * @param scope space-separated list of granted scopes
   * @param expiresIn seconds until the access token expires
   * @param email the Google account email (may be null)
   */
  public record TokenResponse(
      String accessToken, String refreshToken, String scope, long expiresIn, String email) {}

  /** Daily cleanup: purge tokens that expired 90+ days ago and were never refreshed. */
  @Scheduled(cron = "0 0 3 * * *") // daily at 3:00 AM
  @Transactional
  public void purgeOrphanedTokens() {
    long deleted =
        tokenRepo.purgeExpiredTokens(Instant.now().minus(90, java.time.temporal.ChronoUnit.DAYS));
    if (deleted > 0) {
      log.info("Purged {} orphaned Google OAuth tokens", deleted);
    }
  }
}
