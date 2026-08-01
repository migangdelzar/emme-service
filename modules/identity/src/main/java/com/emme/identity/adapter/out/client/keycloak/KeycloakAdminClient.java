package com.emme.identity.adapter.out.client.keycloak;

import com.emme.identity.configuration.IdentityKeycloakProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.springframework.stereotype.Component;

@Component
public class KeycloakAdminClient {

  private final OkHttpClient httpClient;
  private final ObjectMapper objectMapper;
  private final String baseUrl;
  private final String adminRealm;
  private final String adminUser;
  private final String adminPassword;

  public KeycloakAdminClient(
      IdentityKeycloakProperties properties, ObjectMapper objectMapper, OkHttpClient httpClient) {
    this.baseUrl = properties.getBaseUrl();
    this.adminRealm = properties.getAdminRealm();
    this.adminUser = properties.getAdminUsername();
    this.adminPassword = properties.getAdminPassword();
    this.objectMapper = objectMapper;
    this.httpClient = httpClient;
  }

  /** Get admin token from master realm. */
  public String getAdminToken() throws IOException {
    var body =
        new FormBody.Builder()
            .add("grant_type", "password")
            .add("client_id", "admin-cli")
            .add("username", adminUser)
            .add("password", adminPassword)
            .build();
    var req =
        new Request.Builder()
            .url(baseUrl + "/realms/" + adminRealm + "/protocol/openid-connect/token")
            .post(body)
            .build();
    try (var resp = httpClient.newCall(req).execute()) {
      if (!resp.isSuccessful()) throw new IOException("Admin token failed: HTTP " + resp.code());
      var node = objectMapper.readTree(resp.body().string());
      return node.get("access_token").asText();
    }
  }

  /** Create a new realm. */
  public void createRealm(String realmName, String displayName) throws IOException {
    var token = getAdminToken();
    var body =
        Map.of(
            "realm", realmName,
            "enabled", true,
            "displayName", displayName);
    var req =
        new Request.Builder()
            .url(baseUrl + "/admin/realms")
            .header("Authorization", "Bearer " + token)
            .header("Content-Type", "application/json")
            .post(
                RequestBody.create(
                    objectMapper.writeValueAsString(body), MediaType.get("application/json")))
            .build();
    try (var resp = httpClient.newCall(req).execute()) {
      if (resp.code() == 409) return; // realm already exists
      if (resp.code() != 201) throw new IOException("Realm create failed: HTTP " + resp.code());
    }
  }

  /** Create an OAuth2 client in the given realm. */
  public void createClient(String realm, String clientId, List<String> redirectUris)
      throws IOException {
    var token = getAdminToken();
    var body =
        Map.of(
            "clientId", clientId,
            "redirectUris", redirectUris,
            "directAccessGrantsEnabled", true,
            "publicClient", true,
            "standardFlowEnabled", true,
            "serviceAccountsEnabled", false);
    var req =
        new Request.Builder()
            .url(baseUrl + "/admin/realms/" + realm + "/clients")
            .header("Authorization", "Bearer " + token)
            .header("Content-Type", "application/json")
            .post(
                RequestBody.create(
                    objectMapper.writeValueAsString(body), MediaType.get("application/json")))
            .build();
    try (var resp = httpClient.newCall(req).execute()) {
      if (resp.code() == 409) return;
      if (resp.code() != 201) throw new IOException("Client create failed: HTTP " + resp.code());
    }
  }

  /** Create a realm-level role. */
  public void createRealmRole(String realm, String roleName) throws IOException {
    var token = getAdminToken();
    var body = Map.of("name", roleName);
    var req =
        new Request.Builder()
            .url(baseUrl + "/admin/realms/" + realm + "/roles")
            .header("Authorization", "Bearer " + token)
            .header("Content-Type", "application/json")
            .post(
                RequestBody.create(
                    objectMapper.writeValueAsString(body), MediaType.get("application/json")))
            .build();
    try (var resp = httpClient.newCall(req).execute()) {
      if (resp.code() == 409) return;
      if (resp.code() != 201) throw new IOException("Role create failed: HTTP " + resp.code());
    }
  }

  /** Create a user with password and assign a realm role. Returns user ID. */
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
    var userReq =
        new Request.Builder()
            .url(baseUrl + "/admin/realms/" + realm + "/users")
            .header("Authorization", "Bearer " + token)
            .header("Content-Type", "application/json")
            .post(
                RequestBody.create(
                    objectMapper.writeValueAsString(userBody), MediaType.get("application/json")))
            .build();
    try (var resp = httpClient.newCall(userReq).execute()) {
      if (resp.code() == 409) {
        // User already exists — look up their ID
        var searchReq =
            new Request.Builder()
                .url(baseUrl + "/admin/realms/" + realm + "/users?username=" + username)
                .header("Authorization", "Bearer " + token)
                .get()
                .build();
        try (var searchResp = httpClient.newCall(searchReq).execute()) {
          var results = objectMapper.readTree(searchResp.body().string());
          if (results.size() > 0) {
            userId = results.get(0).get("id").asText();
          } else {
            throw new IOException("User exists but cannot find ID for: " + username);
          }
        }
      } else if (resp.code() == 201) {
        var location = resp.header("Location");
        if (location == null) throw new IOException("No Location header in user create response");
        userId = location.substring(location.lastIndexOf('/') + 1);
      } else {
        throw new IOException("User create failed: HTTP " + resp.code());
      }
    }

    // Step 2: Set password via reset-password endpoint
    var pwdBody = Map.of("type", "password", "value", password, "temporary", false);
    var pwdReq =
        new Request.Builder()
            .url(baseUrl + "/admin/realms/" + realm + "/users/" + userId + "/reset-password")
            .header("Authorization", "Bearer " + token)
            .header("Content-Type", "application/json")
            .put(
                RequestBody.create(
                    objectMapper.writeValueAsString(pwdBody), MediaType.get("application/json")))
            .build();
    try (var resp = httpClient.newCall(pwdReq).execute()) {
      if (resp.code() != 204) throw new IOException("Password set failed: HTTP " + resp.code());
    }

    // Step 3: Look up role ID
    var roleReq =
        new Request.Builder()
            .url(baseUrl + "/admin/realms/" + realm + "/roles/" + roleName)
            .header("Authorization", "Bearer " + token)
            .get()
            .build();
    String roleId;
    try (var resp = httpClient.newCall(roleReq).execute()) {
      if (!resp.isSuccessful()) throw new IOException("Role lookup failed: HTTP " + resp.code());
      var node = objectMapper.readTree(resp.body().string());
      roleId = node.get("id").asText();
    }

    // Step 4: Assign role to user
    var assignBody = List.of(Map.of("id", roleId, "name", roleName));
    var assignReq =
        new Request.Builder()
            .url(baseUrl + "/admin/realms/" + realm + "/users/" + userId + "/role-mappings/realm")
            .header("Authorization", "Bearer " + token)
            .header("Content-Type", "application/json")
            .post(
                RequestBody.create(
                    objectMapper.writeValueAsString(assignBody), MediaType.get("application/json")))
            .build();
    try (var resp = httpClient.newCall(assignReq).execute()) {
      if (resp.code() != 204) throw new IOException("Role assignment failed: HTTP " + resp.code());
    }
    return userId;
  }
}
