package com.emme.identity.adapter.out.client.keycloak;

import com.emme.identity.application.port.out.IdentityProviderAdministrationPort;
import com.emme.identity.configuration.IdentityKeycloakProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class KeycloakAdminClient implements IdentityProviderAdministrationPort {

  private final RestClient httpClient;
  private final ObjectMapper objectMapper;
  private final String baseUrl;
  private final String adminRealm;
  private final String adminUser;
  private final String adminPassword;

  public KeycloakAdminClient(
      IdentityKeycloakProperties properties,
      ObjectMapper objectMapper,
      @Qualifier("identityRestClient") RestClient httpClient) {
    this.baseUrl = properties.baseUrl();
    this.adminRealm = properties.adminRealm();
    this.adminUser = properties.adminUsername();
    this.adminPassword = properties.adminPassword();
    this.objectMapper = objectMapper;
    this.httpClient = httpClient;
  }

  /** Get admin token from master realm. */
  public String getAdminToken() throws IOException {
    MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
    body.add("grant_type", "password");
    body.add("client_id", "admin-cli");
    body.add("username", adminUser);
    body.add("password", adminPassword);
    try {
      String responseBody =
          httpClient
              .post()
              .uri(baseUrl + "/realms/" + adminRealm + "/protocol/openid-connect/token")
              .contentType(MediaType.APPLICATION_FORM_URLENCODED)
              .body(body)
              .retrieve()
              .body(String.class);
      var node = objectMapper.readTree(responseBody == null ? "" : responseBody);
      return node.get("access_token").asText();
    } catch (RestClientResponseException exception) {
      throw new IOException(
          "Admin token failed: HTTP " + exception.getStatusCode().value(), exception);
    } catch (RestClientException exception) {
      throw new IOException("Admin token request failed", exception);
    }
  }

  /** Create a new realm. */
  @Override
  public void createRealm(String realmName, String displayName) throws IOException {
    var token = getAdminToken();
    var body = realmRepresentation(realmName, displayName);
    executeCreate("Realm", baseUrl + "/admin/realms", token, body, HttpMethod.POST, 201);
  }

  static Map<String, Object> realmRepresentation(String realmName, String displayName) {
    return Map.of(
        "realm",
        realmName,
        "enabled",
        true,
        "displayName",
        displayName,
        "accessTokenLifespan",
        3_600);
  }

  /** Create an OAuth2 client in the given realm. */
  @Override
  public void createClient(String realm, String clientId, List<String> redirectUris)
      throws IOException {
    var token = getAdminToken();
    var body = clientRepresentation(clientId, redirectUris);
    executeCreate(
        "Client",
        baseUrl + "/admin/realms/" + realm + "/clients",
        token,
        body,
        HttpMethod.POST,
        201);
  }

  static Map<String, Object> clientRepresentation(String clientId, List<String> redirectUris) {
    return Map.of(
        "clientId", clientId,
        "redirectUris", redirectUris,
        "directAccessGrantsEnabled", true,
        "publicClient", true,
        "standardFlowEnabled", true,
        "serviceAccountsEnabled", false,
        "protocolMappers",
            List.of(
                Map.of(
                    "name",
                    clientId + "-audience",
                    "protocol",
                    "openid-connect",
                    "protocolMapper",
                    "oidc-audience-mapper",
                    "config",
                    Map.of(
                        "included.client.audience", clientId,
                        "id.token.claim", "false",
                        "access.token.claim", "true"))));
  }

  /** Create a realm-level role. */
  @Override
  public void createRealmRole(String realm, String roleName) throws IOException {
    var token = getAdminToken();
    var body = Map.of("name", roleName);
    executeCreate(
        "Role", baseUrl + "/admin/realms/" + realm + "/roles", token, body, HttpMethod.POST, 201);
  }

  /** Create a user with password and assign a realm role. Returns user ID. */
  @Override
  public String createUser(
      String realm, String username, String email, String password, String roleName)
      throws IOException {
    var token = getAdminToken();
    String userId;

    // Step 1: Create user (no credentials — set password separately)
    var userBody =
        Map.of(
            "username",
            username,
            "email",
            email,
            "emailVerified",
            true,
            "enabled",
            true,
            "firstName",
            "Admin",
            "lastName",
            "User",
            "requiredActions",
            List.of());
    try {
      var response =
          httpClient
              .post()
              .uri(baseUrl + "/admin/realms/" + realm + "/users")
              .header("Authorization", "Bearer " + token)
              .contentType(MediaType.APPLICATION_JSON)
              .body(userBody)
              .exchange((request, clientResponse) -> clientResponse);
      int status = response.getStatusCode().value();
      if (status == 409) {
        // User already exists — look up their ID
        String searchBody =
            httpClient
                .get()
                .uri(baseUrl + "/admin/realms/" + realm + "/users?username=" + username)
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .body(String.class);
        var results = objectMapper.readTree(searchBody == null ? "" : searchBody);
        if (results.size() > 0) {
          userId = results.get(0).get("id").asText();
        } else {
          throw new IOException("User exists but cannot find ID for: " + username);
        }
      } else if (status == 201) {
        var location = response.getHeaders().getFirst("Location");
        if (location == null) throw new IOException("No Location header in user create response");
        userId = location.substring(location.lastIndexOf('/') + 1);
      } else {
        throw new IOException("User create failed: HTTP " + status);
      }
    } catch (RestClientResponseException exception) {
      throw new IOException(
          "User create failed: HTTP " + exception.getStatusCode().value(), exception);
    } catch (RestClientException exception) {
      throw new IOException("User create request failed", exception);
    }

    // Step 2: Set password via reset-password endpoint
    var pwdBody = Map.of("type", "password", "value", password, "temporary", false);
    executeCreate(
        "Password set",
        baseUrl + "/admin/realms/" + realm + "/users/" + userId + "/reset-password",
        token,
        pwdBody,
        HttpMethod.PUT,
        204);

    // Step 3: Look up role ID
    String roleId;
    try {
      String responseBody =
          httpClient
              .get()
              .uri(baseUrl + "/admin/realms/" + realm + "/roles/" + roleName)
              .header("Authorization", "Bearer " + token)
              .retrieve()
              .body(String.class);
      var node = objectMapper.readTree(responseBody == null ? "" : responseBody);
      roleId = node.get("id").asText();
    } catch (RestClientResponseException exception) {
      throw new IOException(
          "Role lookup failed: HTTP " + exception.getStatusCode().value(), exception);
    } catch (RestClientException exception) {
      throw new IOException("Role lookup request failed", exception);
    }

    // Step 4: Assign role to user
    var assignBody = List.of(Map.of("id", roleId, "name", roleName));
    executeCreate(
        "Role assignment",
        baseUrl + "/admin/realms/" + realm + "/users/" + userId + "/role-mappings/realm",
        token,
        assignBody,
        HttpMethod.POST,
        204);
    return userId;
  }

  private void executeCreate(
      String operation,
      String uri,
      String token,
      Object body,
      HttpMethod method,
      int expectedStatus)
      throws IOException {
    try {
      var request = httpClient.method(method).uri(uri).header("Authorization", "Bearer " + token);
      request
          .contentType(MediaType.APPLICATION_JSON)
          .body(body)
          .exchange(
              (ignored, response) -> {
                int status = response.getStatusCode().value();
                if (status == 409 || status == expectedStatus) return null;
                throw new IOException(operation + " failed: HTTP " + status);
              });
    } catch (RestClientResponseException exception) {
      if (exception.getStatusCode().value() == 409) return;
      throw new IOException(
          operation + " failed: HTTP " + exception.getStatusCode().value(), exception);
    } catch (RestClientException exception) {
      throw new IOException(operation + " request failed", exception);
    }
  }
}
