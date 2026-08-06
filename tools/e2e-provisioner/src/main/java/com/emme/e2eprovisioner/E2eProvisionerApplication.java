package com.emme.e2eprovisioner;

import com.emme.e2eprovisioner.keycloak.HttpKeycloakAdminClient;
import com.emme.e2eprovisioner.keycloak.RealmConfiguration;
import com.emme.e2eprovisioner.tenant.JdbcTenantSeeder;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/**
 * Provisions a disposable E2E environment by calling the platform API.
 *
 * <p>Only Keycloak realm bootstrapping is direct (infrastructure, not application logic).
 * Tenant creation, schema migration, realm provisioning, and data seeding are all handled
 * by the platform's event-driven provisioning chain via POST /api/tenants, then we seed
 * the admin user membership directly in the DB so the platform can authenticate.
 */
public final class E2eProvisionerApplication {

  private static final String API_VERSION = "1.0";
  private static final HttpClient HTTP = HttpClient.newBuilder()
      .connectTimeout(Duration.ofSeconds(10)).build();
  private static final ObjectMapper JSON = new ObjectMapper();

  private E2eProvisionerApplication() {}

  public static void main(String[] args) throws Exception {
    var env = Environment.fromSystem();

    // Bootstrap: create Keycloak realm + admin user (infrastructure setup, not app logic)
    var keycloak = new HttpKeycloakAdminClient(HTTP, JSON,
        env.keycloakUrl(), env.keycloakAdminUsername(), env.keycloakAdminPassword());
    var tenantId = UUID.randomUUID();
    var realmConfig = new RealmConfiguration(
        env.ownerUsername(), env.ownerPassword(), tenantId,
        env.tenantSlug(), env.webOrigin());
    var userRef = keycloak.provisionTenantOwner(realmConfig);

    // Seed tenant in DB so platform can authenticate the user
    var dataSource = new DriverManagerDataSource();
    dataSource.setDriverClassName("org.postgresql.Driver");
    dataSource.setUrl(env.databaseUrl());
    dataSource.setUsername(env.databaseUsername());
    dataSource.setPassword(env.databasePassword());
    var tenantSeeder = JdbcTenantSeeder.create(dataSource);
    tenantSeeder.ensureTenant(env.tenantSlug(), env.tenantName());

    var schemaName = env.tenantSlug().replaceAll("[^a-z0-9-]", "").replace("-", "_");
    tenantSeeder.cleanTenantData(tenantId, schemaName);
    tenantSeeder.activateOwnerMembership(tenantId, userRef, schemaName);

    // Authenticate against platform
    String token = platformPost(env, "/api/auth/login",
        JSON.createObjectNode()
            .put("email", env.ownerUsername() + "@" + env.tenantSlug() + ".local")
            .put("password", env.ownerPassword()),
        env.tenantSlug())
        .get("accessToken").asText();

    // Create tenants via platform API → event chain handles schema + realm provisioning
    for (String slug : new String[]{"e2e-studio", "e2e-salon"}) {
      platformPost(env, "/api/tenants",
          JSON.createObjectNode().put("slug", slug).put("name", slug.startsWith("e2e-studio") ? "E2E Studio" : "E2E Salon"),
          token);
      System.out.println("Created tenant via API: " + slug);
    }

    System.out.println("E2E environment provisioned via platform API");
  }

  private static com.fasterxml.jackson.databind.JsonNode platformPost(
      Environment env, String path, com.fasterxml.jackson.databind.node.ObjectNode body,
      String token) throws Exception {
    return platformPost(env, path, body, token, null);
  }

  private static com.fasterxml.jackson.databind.JsonNode platformPost(
      Environment env, String path, com.fasterxml.jackson.databind.node.ObjectNode body,
      String token, String tenantSlug) throws Exception {
    var builder = HttpRequest.newBuilder()
        .uri(URI.create(env.platformUrl() + path))
        .header("Content-Type", "application/json")
        .header("API-Version", API_VERSION);
    if (token != null) builder.header("Authorization", "Bearer " + token);
    if (tenantSlug != null) builder.header("X-Tenant-Slug", tenantSlug);
    var request = builder.POST(HttpRequest.BodyPublishers.ofString(body.toString())).build();
    var response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
    if (response.statusCode() >= 400) {
      throw new RuntimeException(path + " failed: HTTP " + response.statusCode() + " " + response.body());
    }
    return JSON.readTree(response.body());
  }

  private record Environment(
      String keycloakUrl, String keycloakAdminUsername, String keycloakAdminPassword,
      String ownerUsername, String ownerPassword,
      String tenantSlug, String tenantName, String webOrigin,
      String platformUrl, String databaseUrl, String databaseUsername, String databasePassword) {

    static Environment fromSystem() {
      return new Environment(
          required("KEYCLOAK_URL", "http://127.0.0.1:18080"),
          required("KEYCLOAK_ADMIN_USERNAME", "admin"),
          required("KEYCLOAK_ADMIN_PASSWORD"),
          required("E2E_OWNER_USERNAME"),
          required("E2E_OWNER_PASSWORD"),
          required("E2E_TENANT_SLUG", "e2e-studio"),
          required("E2E_TENANT_NAME", "E2E Studio"),
          required("E2E_WEB_ORIGIN", "http://localhost:3000"),
          required("E2E_PLATFORM_URL", "http://127.0.0.1:8081"),
          required("E2E_DATABASE_URL", "jdbc:postgresql://127.0.0.1:5432/emme"),
          required("E2E_DATABASE_USERNAME", "emme"),
          required("E2E_DATABASE_PASSWORD", "emme"));
    }

    private static String required(String name) {
      var value = System.getenv(name);
      if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " must be configured");
      return value;
    }

    private static String required(String name, String fallback) {
      var value = System.getenv(name);
      return value == null || value.isBlank() ? fallback : value;
    }
  }
}
