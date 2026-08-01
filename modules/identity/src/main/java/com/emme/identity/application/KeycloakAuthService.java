package com.emme.identity.application;

import com.emme.tenancy.api.usecase.TenantApi;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;
import java.io.IOException;
import java.text.ParseException;
import java.util.Map;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class KeycloakAuthService {

  private static final Logger log = LoggerFactory.getLogger(KeycloakAuthService.class);

  private final OkHttpClient httpClient = new OkHttpClient();
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final String baseUrl;
  private final String clientId;
  private final String defaultRealm;
  private final TenantApi tenantApi;

  public KeycloakAuthService(
      @Value("${spring.security.oauth2.client.provider.keycloak.issuer-uri}") String issuerUri,
      @Value("${spring.security.oauth2.client.registration.keycloak.client-id}") String clientId,
      @Value("${app.keycloak.default-realm:emme}") String defaultRealm,
      TenantApi tenantApi) {
    this.baseUrl = issuerUri.substring(0, issuerUri.indexOf("/realms/"));
    this.clientId = clientId;
    this.defaultRealm = defaultRealm;
    this.tenantApi = tenantApi;
  }

  public record TokenResult(String accessToken, String refreshToken, String idToken) {}

  public TokenResult authenticate(String username, String password) throws IOException {
    String realm = resolveRealm(username);
    String tokenUrl = baseUrl + "/realms/" + realm + "/protocol/openid-connect/token";
    log.debug("Authenticating user '{}' against realm '{}'", username, realm);

    RequestBody body =
        new FormBody.Builder()
            .add("client_id", clientId)
            .add("grant_type", "password")
            .add("username", username)
            .add("password", password)
            .add("scope", "openid profile email")
            .build();

    Request request =
        new Request.Builder()
            .url(tokenUrl)
            .post(body)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .build();

    try (Response response = httpClient.newCall(request).execute()) {
      if (!response.isSuccessful()) {
        throw new AuthenticationException("Invalid credentials");
      }
      JsonNode json = objectMapper.readTree(response.body().string());
      return new TokenResult(
          json.get("access_token").asText(),
          json.has("refresh_token") ? json.get("refresh_token").asText() : null,
          json.has("id_token") ? json.get("id_token").asText() : null);
    }
  }

  public Map<String, Object> getUserInfo(String accessToken) throws IOException {
    String userInfoUrl = userInfoUrlFromToken(accessToken);

    Request request =
        new Request.Builder()
            .url(userInfoUrl)
            .get()
            .header("Authorization", "Bearer " + accessToken)
            .build();

    try (Response response = httpClient.newCall(request).execute()) {
      if (!response.isSuccessful()) {
        throw new AuthenticationException("Failed to get user info");
      }
      String body = response.body().string();
      @SuppressWarnings("unchecked")
      Map<String, Object> claims = objectMapper.readValue(body, Map.class);
      return claims;
    }
  }

  private String userInfoUrlFromToken(String accessToken) {
    try {
      JWT jwt = JWTParser.parse(accessToken);
      String iss = jwt.getJWTClaimsSet().getIssuer();
      return iss + "/protocol/openid-connect/userinfo";
    } catch (ParseException e) {
      log.warn("Failed to parse JWT issuer, falling back to default realm", e);
      return baseUrl + "/realms/" + defaultRealm + "/protocol/openid-connect/userinfo";
    }
  }

  private String resolveRealm(String email) {
    // Platform admin check — if email is in the platform domain
    if (email.endsWith("@emme.app")
        && !email.contains("@demo-salon")
        && !email.contains("@studio-a")) {
      return defaultRealm;
    }
    // Try to find tenant by email domain
    String domain = email.substring(email.indexOf('@') + 1);
    for (var tenant : tenantApi.getAllTenants()) {
      if (domain.contains(tenant.slug())) {
        return tenant.identityRealm();
      }
    }
    return defaultRealm;
  }

  public static class AuthenticationException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public AuthenticationException(String message) {
      super(message);
    }
  }
}
