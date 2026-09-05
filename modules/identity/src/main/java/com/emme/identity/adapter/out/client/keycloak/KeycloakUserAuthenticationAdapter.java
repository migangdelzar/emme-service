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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/** Keycloak adapter for password grants and user-info retrieval. */
@Component
public final class KeycloakUserAuthenticationAdapter implements UserAuthenticationPort {

  private final RestClient httpClient;
  private final ObjectMapper objectMapper;
  private final String baseUrl;
  private final IdentityKeycloakProperties properties;

  public KeycloakUserAuthenticationAdapter(
      @Qualifier("identityRestClient") RestClient httpClient,
      ObjectMapper objectMapper,
      IdentityKeycloakProperties properties) {
    int realmIndex = properties.issuerUri().indexOf("/realms/");
    if (realmIndex < 0) {
      throw new IllegalArgumentException("Keycloak issuer URI must contain /realms/");
    }
    this.httpClient = httpClient;
    this.objectMapper = objectMapper;
    this.baseUrl = properties.issuerUri().substring(0, realmIndex);
    this.properties = properties;
  }

  @Override
  public UserTokenResult authenticate(String realm, String username, String password) {
    String tokenUrl = baseUrl + "/realms/" + realm + "/protocol/openid-connect/token";
    MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
    body.add("client_id", clientIdForRealm(realm));
    body.add("grant_type", "password");
    body.add("username", username);
    body.add("password", password);
    body.add("scope", "openid profile email");

    try {
      String responseBody =
          httpClient
              .post()
              .uri(tokenUrl)
              .contentType(MediaType.APPLICATION_FORM_URLENCODED)
              .body(body)
              .retrieve()
              .body(String.class);
      JsonNode json = objectMapper.readTree(responseBody == null ? "" : responseBody);
      return new UserTokenResult(
          json.get("access_token").asText(),
          json.has("refresh_token") ? json.get("refresh_token").asText() : null,
          json.has("id_token") ? json.get("id_token").asText() : null);
    } catch (RestClientResponseException exception) {
      if (exception.getStatusCode().is4xxClientError()) {
        throw new IdentityAuthenticationException("Invalid credentials");
      }
      throw new IdentityProviderUnavailableException(
          "Authentication provider request failed", exception);
    } catch (RestClientException exception) {
      throw new IdentityProviderUnavailableException(
          "Authentication provider request failed", exception);
    } catch (IOException exception) {
      throw new IdentityProviderUnavailableException(
          "Authentication provider request failed", exception);
    }
  }

  private String clientIdForRealm(String realm) {
    return properties.defaultRealm().equals(realm)
        ? properties.platformClientId()
        : properties.clientId();
  }

  @Override
  public UserClaimsResult getUserClaims(String accessToken) {
    try {
      String responseBody =
          httpClient
              .get()
              .uri(userInfoUrlFromToken(accessToken))
              .header("Authorization", "Bearer " + accessToken)
              .retrieve()
              .body(String.class);
      @SuppressWarnings("unchecked")
      Map<String, Object> claims =
          objectMapper.readValue(responseBody == null ? "" : responseBody, Map.class);
      return new UserClaimsResult(claims);
    } catch (RestClientResponseException exception) {
      if (exception.getStatusCode().is4xxClientError()) {
        throw new IdentityAuthenticationException("Failed to get user info");
      }
      throw new IdentityProviderUnavailableException("Failed to get user info", exception);
    } catch (RestClientException exception) {
      throw new IdentityProviderUnavailableException("Failed to get user info", exception);
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
      return baseUrl + "/realms/" + properties.defaultRealm() + "/protocol/openid-connect/userinfo";
    }
  }
}
