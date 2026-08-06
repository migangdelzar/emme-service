package com.emme.e2eprovisioner.keycloak;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

/** HTTP adapter for the Keycloak Admin REST API used only by disposable E2E setup. */
public final class HttpKeycloakAdminClient implements KeycloakAdminClient {

  private final HttpClient httpClient;
  private final ObjectMapper objectMapper;
  private final String baseUrl;
  private final String adminUsername;
  private final String adminPassword;

  public HttpKeycloakAdminClient(
      HttpClient httpClient,
      ObjectMapper objectMapper,
      String baseUrl,
      String adminUsername,
      String adminPassword) {
    this.httpClient = Objects.requireNonNull(httpClient);
    this.objectMapper = Objects.requireNonNull(objectMapper);
    this.baseUrl = stripTrailingSlash(Objects.requireNonNull(baseUrl));
    this.adminUsername = Objects.requireNonNull(adminUsername);
    this.adminPassword = Objects.requireNonNull(adminPassword);
  }

  @Override
  public String provisionTenantOwner(RealmConfiguration configuration)
      throws IOException, InterruptedException {
    var realmName = "emme-" + configuration.tenantSlug();
    var adminToken = requestAdminToken();
    var realmResponse =
        send(
            HttpRequest.newBuilder(uri("/admin/realms"))
                .header("Authorization", "Bearer " + adminToken)
                .header("Content-Type", "application/json")
                .POST(
                    HttpRequest.BodyPublishers.ofString(
                        RealmDocumentFactory.create(configuration).toString()))
                .build());
    if (realmResponse.statusCode() != 201 && realmResponse.statusCode() != 409) {
      throw unexpectedStatus("realm creation", realmResponse);
    }

    var userDoc = objectMapper.createObjectNode();
    userDoc.put("username", configuration.username());
    userDoc.put("email", configuration.username() + "@" + configuration.tenantSlug() + ".local");
    userDoc.put("emailVerified", true);
    userDoc.put("enabled", true);
    userDoc.put("firstName", "E2E");
    userDoc.put("lastName", "Owner");
    userDoc.putArray("requiredActions");
    userDoc
        .putArray("credentials")
        .addObject()
        .put("type", "password")
        .put("value", configuration.password())
        .put("temporary", false);
    userDoc.putArray("realmRoles").add("business_owner");
    var attributes = userDoc.putObject("attributes");
    attributes.put("tenant_id", configuration.tenantId().toString());
    attributes.put("tenant_slug", configuration.tenantSlug());

    // Check if user already exists
    var existingResponse =
        send(
            HttpRequest.newBuilder(
                    uri(
                        "/admin/realms/"
                            + realmName
                            + "/users?username="
                            + URLEncoder.encode(configuration.username(), StandardCharsets.UTF_8)))
                .header("Authorization", "Bearer " + adminToken)
                .GET()
                .build());
    if (existingResponse.statusCode() == 200) {
      var existing = objectMapper.readTree(existingResponse.body());
      if (existing.isArray() && !existing.isEmpty() && existing.get(0).hasNonNull("id")) {
        return existing.get(0).path("id").asText();
      }
    }

    // Create user
    var createResponse =
        send(
            HttpRequest.newBuilder(uri("/admin/realms/" + realmName + "/users"))
                .header("Authorization", "Bearer " + adminToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(userDoc.toString()))
                .build());
    if (createResponse.statusCode() != 201 && createResponse.statusCode() != 409) {
      throw unexpectedStatus("user creation", createResponse);
    }

    // Look up created user to get ID
    var lookupResponse =
        send(
            HttpRequest.newBuilder(
                    uri(
                        "/admin/realms/"
                            + realmName
                            + "/users?username="
                            + URLEncoder.encode(configuration.username(), StandardCharsets.UTF_8)))
                .header("Authorization", "Bearer " + adminToken)
                .GET()
                .build());
    if (lookupResponse.statusCode() != 200) {
      throw unexpectedStatus("user lookup after creation", lookupResponse);
    }
    var users = objectMapper.readTree(lookupResponse.body());
    if (!users.isArray() || users.isEmpty() || !users.get(0).hasNonNull("id")) {
      throw new IOException("Keycloak tenant-owner user was not found after creation");
    }
    return users.get(0).path("id").asText();
  }

  @Override
  public String createUser(String realm, String username, String email, String password,
      String firstName, String lastName, String role, UUID tenantId, String tenantSlug)
      throws IOException, InterruptedException {
    var adminToken = requestAdminToken();
    var realmName = "emme-" + realm;

    var userDoc = objectMapper.createObjectNode();
    userDoc.put("username", username);
    userDoc.put("email", email);
    userDoc.put("emailVerified", true);
    userDoc.put("enabled", true);
    userDoc.put("firstName", firstName);
    userDoc.put("lastName", lastName);
    userDoc.putArray("requiredActions");
    userDoc.putArray("credentials").addObject()
        .put("type", "password").put("value", password).put("temporary", false);
    userDoc.putArray("realmRoles").add(role);
    var attributes = userDoc.putObject("attributes");
    attributes.put("tenant_id", tenantId.toString());
    attributes.put("tenant_slug", tenantSlug);

    return upsertUser(realmName, username, userDoc, adminToken);
  }

  private String upsertUser(String realmName, String username, ObjectNode userDoc, String adminToken)
      throws IOException, InterruptedException {
    // Check if user already exists
    var existing = send(HttpRequest.newBuilder(
            uri("/admin/realms/" + realmName + "/users?username="
                + URLEncoder.encode(username, StandardCharsets.UTF_8)))
        .header("Authorization", "Bearer " + adminToken).GET().build());
    if (existing.statusCode() == 200) {
      var users = objectMapper.readTree(existing.body());
      if (users.isArray() && !users.isEmpty() && users.get(0).hasNonNull("id")) {
        return users.get(0).path("id").asText();
      }
    }

    var create = send(HttpRequest.newBuilder(uri("/admin/realms/" + realmName + "/users"))
        .header("Authorization", "Bearer " + adminToken)
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(userDoc.toString())).build());
    if (create.statusCode() != 201 && create.statusCode() != 409) {
      throw unexpectedStatus("user creation", create);
    }

    var lookup = send(HttpRequest.newBuilder(
            uri("/admin/realms/" + realmName + "/users?username="
                + URLEncoder.encode(username, StandardCharsets.UTF_8)))
        .header("Authorization", "Bearer " + adminToken).GET().build());
    if (lookup.statusCode() != 200) {
      throw unexpectedStatus("user lookup", lookup);
    }
    var users = objectMapper.readTree(lookup.body());
    if (!users.isArray() || users.isEmpty() || !users.get(0).hasNonNull("id")) {
      throw new IOException("Keycloak user not found after creation: " + username);
    }
    return users.get(0).path("id").asText();
  }

  private String requestAdminToken() throws IOException, InterruptedException {
    var body =
        "grant_type=password"
            + "&client_id=admin-cli"
            + "&username="
            + URLEncoder.encode(adminUsername, StandardCharsets.UTF_8)
            + "&password="
            + URLEncoder.encode(adminPassword, StandardCharsets.UTF_8);
    var response =
        send(
            HttpRequest.newBuilder(uri("/realms/master/protocol/openid-connect/token"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .timeout(Duration.ofSeconds(15))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build());
    if (response.statusCode() != 200) {
      throw unexpectedStatus("admin token request", response);
    }
    JsonNode token = objectMapper.readTree(response.body()).path("access_token");
    if (token.isMissingNode() || token.asText().isBlank()) {
      throw new IOException("Keycloak admin token response did not contain access_token");
    }
    return token.asText();
  }

  private HttpResponse<String> send(HttpRequest request) throws IOException, InterruptedException {
    return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
  }

  private URI uri(String path) {
    return URI.create(baseUrl + path);
  }

  private static IOException unexpectedStatus(String operation, HttpResponse<String> response) {
    return new IOException(
        "Keycloak "
            + operation
            + " failed with HTTP "
            + response.statusCode()
            + ": "
            + response.body().substring(0, Math.min(response.body().length(), 500)));
  }

  private static String stripTrailingSlash(String value) {
    return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
  }
}
