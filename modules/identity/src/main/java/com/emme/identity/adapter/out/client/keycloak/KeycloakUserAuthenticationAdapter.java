package com.emme.identity.adapter.out.client.keycloak;

import com.emme.identity.api.exception.IdentityAuthenticationException;
import com.emme.identity.api.exception.IdentityProviderUnavailableException;
import com.emme.identity.api.result.UserClaimsResult;
import com.emme.identity.api.result.UserTokenResult;
import com.emme.identity.application.port.out.UserAuthenticationPort;
import com.emme.identity.configuration.IdentityKeycloakProperties;
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
import org.springframework.stereotype.Component;

/** Keycloak adapter for password grants and user-info retrieval. */
@Component
public final class KeycloakUserAuthenticationAdapter implements UserAuthenticationPort {

  private final OkHttpClient httpClient;
  private final ObjectMapper objectMapper;
  private final String baseUrl;
  private final String clientId;
  private final IdentityKeycloakProperties properties;

  public KeycloakUserAuthenticationAdapter(
      OkHttpClient httpClient, ObjectMapper objectMapper, IdentityKeycloakProperties properties) {
    int realmIndex = properties.getIssuerUri().indexOf("/realms/");
    if (realmIndex < 0) {
      throw new IllegalArgumentException("Keycloak issuer URI must contain /realms/");
    }
    this.httpClient = httpClient;
    this.objectMapper = objectMapper;
    this.baseUrl = properties.getIssuerUri().substring(0, realmIndex);
    this.clientId = properties.getClientId();
    this.properties = properties;
  }

  @Override
  public UserTokenResult authenticate(String realm, String username, String password) {
    String tokenUrl = baseUrl + "/realms/" + realm + "/protocol/openid-connect/token";
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
        throw new IdentityAuthenticationException("Invalid credentials");
      }
      JsonNode json = objectMapper.readTree(response.body().string());
      return new UserTokenResult(
          json.get("access_token").asText(),
          json.has("refresh_token") ? json.get("refresh_token").asText() : null,
          json.has("id_token") ? json.get("id_token").asText() : null);
    } catch (IOException exception) {
      throw new IdentityProviderUnavailableException(
          "Authentication provider request failed", exception);
    }
  }

  @Override
  public UserClaimsResult getUserClaims(String accessToken) {
    Request request =
        new Request.Builder()
            .url(userInfoUrlFromToken(accessToken))
            .get()
            .header("Authorization", "Bearer " + accessToken)
            .build();

    try (Response response = httpClient.newCall(request).execute()) {
      if (!response.isSuccessful()) {
        throw new IdentityAuthenticationException("Failed to get user info");
      }
      @SuppressWarnings("unchecked")
      Map<String, Object> claims = objectMapper.readValue(response.body().string(), Map.class);
      return new UserClaimsResult(claims);
    } catch (IOException exception) {
      throw new IdentityProviderUnavailableException("Failed to get user info", exception);
    }
  }

  private String userInfoUrlFromToken(String accessToken) {
    try {
      JWT jwt = JWTParser.parse(accessToken);
      String issuer = jwt.getJWTClaimsSet().getIssuer();
      return issuer + "/protocol/openid-connect/userinfo";
    } catch (ParseException exception) {
      return baseUrl
          + "/realms/"
          + properties.getDefaultRealm()
          + "/protocol/openid-connect/userinfo";
    }
  }
}
