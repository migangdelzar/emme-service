package com.emme.e2eprovisioner;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Provisions E2E tenants by calling the platform API only.
 *
 * <p>Creates tenants via POST /api/tenants. The platform handles everything: schema migration,
 * Keycloak realm creation, and admin user provisioning via its event-driven provisioning chain.
 *
 * <p><strong>Prerequisite:</strong> emme-core realm with platform-admin user must exist.
 *
 * <p>User matrix (after platform provisions everything):
 *
 * <pre>
 *   emme-core:  platform-admin@emme-core.local / E2e-Platform-Admin-2026!  (bootstrap)
 *   e2e-studio: owner@e2e-studio.local / E2e-Tenant-Owner-2026!            (created by platform)
 *   e2e-salon:  owner@e2e-salon.local / E2e-Tenant-Owner-2026!             (created by platform)
 * </pre>
 */
public final class E2eProvisionerApplication {

  private static final String API_VERSION = "1.0";
  private static final HttpClient HTTP =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
  private static final ObjectMapper JSON = new ObjectMapper();

  private E2eProvisionerApplication() {}

  public static void main(String[] args) throws Exception {
    var template =
        JSON.readTree(
            E2eProvisionerApplication.class
                .getClassLoader()
                .getResourceAsStream("tenants-e2e.json"));

    var platformUrl = template.get("platformUrl").asText();
    var adminEmail = "platform-admin@emme-core.local";
    var adminPass = System.getenv().getOrDefault("E2E_ADMIN_PASSWORD", "E2e-Platform-Admin-2026!");

    var loginBody = JSON.createObjectNode().put("email", adminEmail).put("password", adminPass);
    var loginResp =
        HTTP.send(
            HttpRequest.newBuilder()
                .uri(URI.create(platformUrl + "/api/auth/login"))
                .header("Content-Type", "application/json")
                .header("API-Version", API_VERSION)
                .header("X-Tenant-Slug", "emme-core")
                .POST(HttpRequest.BodyPublishers.ofString(loginBody.toString()))
                .build(),
            HttpResponse.BodyHandlers.ofString());

    if (loginResp.statusCode() != 200) {
      System.err.println("Auth failed: " + loginResp.body());
      System.exit(1);
    }
    var token = JSON.readTree(loginResp.body()).get("accessToken").asText();

    System.out.println("=== E2E Provisioner ===");
    for (var tenant : template.get("tenants")) {
      var slug = tenant.get("slug").asText();
      var name = tenant.get("name").asText();
      var body = JSON.createObjectNode().put("slug", slug).put("name", name);
      var resp =
          HTTP.send(
              HttpRequest.newBuilder()
                  .uri(URI.create(platformUrl + "/api/tenants"))
                  .header("Content-Type", "application/json")
                  .header("API-Version", API_VERSION)
                  .header("Authorization", "Bearer " + token)
                  .header("X-Tenant-Slug", "emme-core")
                  .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                  .build(),
              HttpResponse.BodyHandlers.ofString());

      var result = JSON.readTree(resp.body());
      var detail = result.path("detail").asText("");
      if (resp.statusCode() == 200 || resp.statusCode() == 201) {
        System.out.println("  ✓ " + slug);
      } else if (detail.contains("already exists")) {
        System.out.println("  ⚠ " + slug + " (already exists)");
      } else {
        System.out.println("  ✗ " + slug + ": " + detail);
      }
    }
    System.out.println("\nPlatform creates schemas, realms, and admin users via event chain.");
  }
}
