package com.emme.e2eprovisioner;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

/**
 * Provisions E2E tenants by calling the platform API.
 *
 * <p>Reads tenant templates from a JSON file. Creates a Keycloak realm + admin user
 * for each tenant via the platform's Keycloak Admin API, then calls POST /api/tenants
 * on the platform to trigger the event-driven provisioning chain (schema migration
 * and realm provisioning handled by the platform).
 *
 * <p><strong>Prerequisites:</strong>
 * <ul>
 *   <li>PostgreSQL with emme_core schema migrated</li>
 *   <li>Keycloak running with master realm admin accessible</li>
 *   <li>Platform healthy on the configured URL</li>
 * </ul>
 *
 * <p>Usage:
 * <pre>
 * java -cp ... com.emme.e2eprovisioner.E2eProvisionerApplication tenants.json
 * </pre>
 *
 * <p>Template format:
 * <pre>
 * {
 *   "platformUrl": "http://localhost:8081",
 *   "keycloakUrl": "http://localhost:18080",
 *   "keycloakAdminUsername": "admin",
 *   "keycloakAdminPassword": "e2e-admin-password",
 *   "databaseUrl": "jdbc:postgresql://localhost:5432/emme",
 *   "databaseUsername": "emme",
 *   "databasePassword": "emme",
 *   "webOrigin": "http://localhost:3000",
 *   "tenants": [
 *     {
 *       "slug": "e2e-studio",
 *       "name": "E2E Studio",
 *       "ownerUsername": "e2e-owner",
 *       "ownerPassword": "E2e-Studio-Owner-2026!",
 *       "ownerEmail": "e2e-owner@e2e-studio.local"
 *     },
 *     {
 *       "slug": "e2e-salon",
 *       "name": "E2E Salon",
 *       "ownerUsername": "e2e-owner-salon",
 *       "ownerPassword": "E2e-Salon-Owner-2026!",
 *       "ownerEmail": "e2e-owner-salon@e2e-salon.local"
 *     }
 *   ]
 * }
 * </pre>
 */
public final class E2eProvisionerApplication {

  private static final String API_VERSION = "1.0";
  private static final HttpClient HTTP = HttpClient.newBuilder()
      .connectTimeout(Duration.ofSeconds(10)).build();
  private static final ObjectMapper JSON = new ObjectMapper();

  private E2eProvisionerApplication() {}

  public static void main(String[] args) throws Exception {
    var templatePath = args.length > 0 ? args[0] : "tenants-e2e.json";
    var template = JSON.readTree(E2eProvisionerApplication.class
        .getClassLoader().getResourceAsStream(templatePath));
    if (template == null) {
      System.err.println("Template not found: " + templatePath);
      System.exit(1);
    }

    var platformUrl = template.get("platformUrl").asText();
    var keycloakUrl = template.get("keycloakUrl").asText();
    var adminUser = template.get("keycloakAdminUsername").asText();
    var adminPass = template.get("keycloakAdminPassword").asText();
    var dbUrl = template.get("databaseUrl").asText();
    var dbUser = template.get("databaseUsername").asText();
    var dbPass = template.get("databasePassword").asText();
    var webOrigin = template.get("webOrigin").asText();

    var keycloak = new com.emme.e2eprovisioner.keycloak.HttpKeycloakAdminClient(
        HTTP, JSON, keycloakUrl, adminUser, adminPass);

    var ds = new org.springframework.jdbc.datasource.DriverManagerDataSource();
    ds.setDriverClassName("org.postgresql.Driver");
    ds.setUrl(dbUrl);
    ds.setUsername(dbUser);
    ds.setPassword(dbPass);
    var seeder = com.emme.e2eprovisioner.tenant.JdbcTenantSeeder.create(ds);

    System.out.println("=== E2E Provisioner ===");

    for (var tenant : template.get("tenants")) {
      var slug = tenant.get("slug").asText();
      var name = tenant.get("name").asText();
      var ownerUser = tenant.get("ownerUsername").asText();
      var ownerPass = tenant.get("ownerPassword").asText();
      var ownerEmail = tenant.get("ownerEmail").asText();
      var schema = slug.replaceAll("[^a-z0-9-]", "").replace("-", "_");

      System.out.println("\nProvisioning: " + slug);

      // 1. Create Keycloak realm + admin user (infrastructure bootstrap)
      var realmConfig = new com.emme.e2eprovisioner.keycloak.RealmConfiguration(
          ownerUser, ownerPass, java.util.UUID.randomUUID(), slug, webOrigin);
      var userRef = keycloak.provisionTenantOwner(realmConfig);

      // 2. Seed tenant record in DB (needed for platform auth)
      var tenantId = seeder.ensureTenant(slug, name);
      seeder.cleanTenantData(tenantId, schema);
      seeder.activateOwnerMembership(tenantId, userRef, schema);

      // 3. Authenticate against platform
      var loginBody = JSON.createObjectNode()
          .put("email", ownerEmail)
          .put("password", ownerPass);
      var loginReq = HttpRequest.newBuilder()
          .uri(URI.create(platformUrl + "/api/auth/login"))
          .header("Content-Type", "application/json")
          .header("API-Version", API_VERSION)
          .header("X-Tenant-Slug", slug)
          .POST(HttpRequest.BodyPublishers.ofString(loginBody.toString()))
          .build();
      var loginResp = HTTP.send(loginReq, HttpResponse.BodyHandlers.ofString());
      if (loginResp.statusCode() != 200) {
        System.err.println("  Auth failed for " + slug);
        continue;
      }
      var token = JSON.readTree(loginResp.body()).get("accessToken").asText();

      // 4. Create tenant via platform API (triggers event-driven provisioning)
      var createBody = JSON.createObjectNode()
          .put("slug", slug)
          .put("name", name);
      var createReq = HttpRequest.newBuilder()
          .uri(URI.create(platformUrl + "/api/tenants"))
          .header("Content-Type", "application/json")
          .header("API-Version", API_VERSION)
          .header("Authorization", "Bearer " + token)
          .header("X-Tenant-Slug", "emme-core")
          .POST(HttpRequest.BodyPublishers.ofString(createBody.toString()))
          .build();
      var createResp = HTTP.send(createReq, HttpResponse.BodyHandlers.ofString());
      if (createResp.statusCode() >= 400 && createResp.statusCode() != 409) {
        System.out.println("  API response: " + createResp.body());
      }

      System.out.println("  ✓ Provisioned: " + slug + " (" + tenantId + ")");
    }

    System.out.println("\n=== Done ===");
  }
}
