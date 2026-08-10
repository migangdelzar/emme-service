package com.emme.e2eprovisioner;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Provisions E2E tenants by calling the platform API only.
 *
 * <p>Creates tenants via POST /api/tenants. The platform handles everything: schema migration,
 * Keycloak realm creation, and admin user provisioning via its event-driven provisioning chain.
 *
 * <p><strong>Prerequisite:</strong> emme-core realm with the global admin user must exist.
 *
 * <p>User matrix (after platform provisions everything):
 *
 * <pre>
 *   emme-core:  admin@emme-core.local / E2e-Platform-Admin-2026!  (bootstrap)
 *   e2e-studio: admin@e2e-studio.local and owner@e2e-studio.local (created by platform)
 *   e2e-salon:  admin@e2e-salon.local and owner@e2e-salon.local  (created by platform)
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
    var adminEmail = "admin@emme-core.local";
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
    var existingTenantSlugs = fetchExistingTenantSlugs(platformUrl, token);

    System.out.println("=== E2E Provisioner ===");
    for (var tenant : template.get("tenants")) {
      var slug = tenant.get("slug").asText();
      var name = tenant.get("name").asText();
      if (existingTenantSlugs.contains(slug)) {
        System.out.println("  ⚠ " + slug + " (already exists)");
        continue;
      }
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
        existingTenantSlugs.add(slug);
      } else if (detail.contains("already exists")) {
        System.out.println("  ⚠ " + slug + " (already exists)");
      } else {
        System.out.println("  ✗ " + slug + ": " + detail);
      }
    }
    writeProvisionedAuthArtifacts(template);
    System.out.println(
        "\nPlatform creates schemas, realms, and tenant admin/owner users via event chain.");
  }

  private static void writeProvisionedAuthArtifacts(
      com.fasterxml.jackson.databind.JsonNode template) throws Exception {
    var outputDirectory =
        System.getenv().getOrDefault("E2E_PROVISIONER_AUTH_DIR", "build/provisioned-auth");
    var adminUsername =
        firstNonBlank(
            System.getenv("E2E_TENANT_ADMIN_USERNAME"),
            System.getenv("APP_KEYCLOAK_PROVISIONING_INITIAL_ADMIN_USERNAME"),
            "admin");
    var ownerUsername =
        firstNonBlank(
            System.getenv("E2E_TENANT_OWNER_USERNAME"),
            System.getenv("APP_KEYCLOAK_PROVISIONING_INITIAL_OWNER_USERNAME"),
            "owner");
    var adminPassword =
        firstNonBlank(
            System.getenv("E2E_TENANT_ADMIN_PASSWORD"),
            System.getenv("APP_KEYCLOAK_PROVISIONING_INITIAL_ADMIN_PASSWORD"));
    var ownerPassword =
        firstNonBlank(
            System.getenv("E2E_TENANT_OWNER_PASSWORD"),
            System.getenv("APP_KEYCLOAK_PROVISIONING_INITIAL_OWNER_PASSWORD"));

    if (adminPassword == null || ownerPassword == null) {
      System.out.println(
          "  ⚠ Auth artifacts not written: set E2E_TENANT_ADMIN_PASSWORD and "
              + "E2E_TENANT_OWNER_PASSWORD (or the APP_KEYCLOAK_PROVISIONING_* equivalents).");
      return;
    }

    var writer = new ProvisionedAuthArtifactWriter(JSON, Path.of(outputDirectory));
    for (var tenant : template.get("tenants")) {
      var slug = tenant.get("slug").asText();
      var artifact =
          writer.write(
              slug,
              Map.of(
                  "admin",
                  new ProvisionedAuthArtifactWriter.Credentials(
                      qualifyUsername(adminUsername, slug), adminPassword),
                  "owner",
                  new ProvisionedAuthArtifactWriter.Credentials(
                      qualifyUsername(ownerUsername, slug), ownerPassword)));
      System.out.println("  ✓ auth artifact " + artifact);
    }
  }

  private static Set<String> fetchExistingTenantSlugs(String platformUrl, String token)
      throws Exception {
    var response =
        HTTP.send(
            HttpRequest.newBuilder()
                .uri(URI.create(platformUrl + "/api/tenants"))
                .header("API-Version", API_VERSION)
                .header("Authorization", "Bearer " + token)
                .header("X-Tenant-Slug", "emme-core")
                .GET()
                .build(),
            HttpResponse.BodyHandlers.ofString());
    return response.statusCode() == 200 ? readTenantSlugs(response.body()) : new LinkedHashSet<>();
  }

  static Set<String> readTenantSlugs(String responseBody) {
    try {
      var slugs = new LinkedHashSet<String>();
      for (var tenant : JSON.readTree(responseBody)) {
        if (tenant.hasNonNull("slug")) slugs.add(tenant.get("slug").asText());
      }
      return slugs;
    } catch (java.io.IOException exception) {
      throw new IllegalArgumentException("Invalid tenant list response", exception);
    }
  }

  private static String qualifyUsername(String username, String tenantSlug) {
    return username.contains("@") ? username : username + "@" + tenantSlug + ".local";
  }

  private static String firstNonBlank(String... values) {
    for (var value : values) {
      if (value != null && !value.isBlank()) return value;
    }
    return null;
  }
}
