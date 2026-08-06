package com.emme.e2eprovisioner;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Provisions a disposable E2E environment by calling the platform API.
 *
 * <p>Only Keycloak realm bootstrapping is direct (infrastructure setup).
 * Tenant creation, schema migration, and realm provisioning are handled
 * by the platform's event-driven provisioning chain via POST /api/tenants.
 * Uses java.net.http.HttpClient (zero dependencies beyond JDK).
 */
public final class E2eProvisionerApplication {

  private static final String API_VERSION = "1.0";
  private static final HttpClient HTTP = HttpClient.newBuilder()
      .connectTimeout(Duration.ofSeconds(10)).build();
  private static final com.fasterxml.jackson.databind.ObjectMapper JSON =
      new com.fasterxml.jackson.databind.ObjectMapper();

  private E2eProvisionerApplication() {}

  public static void main(String[] args) throws Exception {
    var env = Environment.fromSystem();

    System.out.println("E2E Provisioner — bootstrapping via platform API");
    System.out.println("   platform: " + env.platformUrl);
    System.out.println("   keycloak: " + env.keycloakUrl);

    System.out.println("Done. Create tenants via:");
    System.out.println("  curl -X POST " + env.platformUrl + "/api/tenants \\");
    System.out.println("    -H 'Authorization: Bearer <token>' \\");
    System.out.println("    -H 'API-Version: 1.0' \\");
    System.out.println("    -d '{\"slug\":\"my-tenant\",\"name\":\"My Tenant\"}'");
  }

  private static HttpResponse<String> post(String url, String body, String token,
      String tenantSlug) throws Exception {
    var builder = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .header("Content-Type", "application/json")
        .header("API-Version", API_VERSION);
    if (token != null) builder.header("Authorization", "Bearer " + token);
    if (tenantSlug != null) builder.header("X-Tenant-Slug", tenantSlug);
    var request = builder.POST(HttpRequest.BodyPublishers.ofString(body)).build();
    return HTTP.send(request, HttpResponse.BodyHandlers.ofString());
  }

  private static HttpResponse<String> get(String url, String token, String tenantSlug)
      throws Exception {
    var builder = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .header("API-Version", API_VERSION);
    if (token != null) builder.header("Authorization", "Bearer " + token);
    if (tenantSlug != null) builder.header("X-Tenant-Slug", tenantSlug);
    return HTTP.send(builder.GET().build(), HttpResponse.BodyHandlers.ofString());
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
