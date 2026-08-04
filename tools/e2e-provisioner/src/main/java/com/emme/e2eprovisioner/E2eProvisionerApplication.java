package com.emme.e2eprovisioner;

import com.emme.e2eprovisioner.keycloak.HttpKeycloakAdminClient;
import com.emme.e2eprovisioner.keycloak.RealmConfiguration;
import com.emme.e2eprovisioner.tenant.JdbcTenantSeeder;
import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/** Entrypoint for idempotent disposable tenant-owner provisioning in real E2E runs. */
public final class E2eProvisionerApplication {

  private E2eProvisionerApplication() {}

  public static void main(String[] args) throws Exception {
    var environment = Environment.fromSystem();
    var dataSource = new DriverManagerDataSource();
    dataSource.setDriverClassName("org.postgresql.Driver");
    dataSource.setUrl(environment.databaseUrl());
    dataSource.setUsername(environment.databaseUsername());
    dataSource.setPassword(environment.databasePassword());

    var tenantSeeder = JdbcTenantSeeder.create(dataSource);
    var tenantId = tenantSeeder.ensureTenant(environment.tenantSlug(), environment.tenantName());

    var keycloak =
        new HttpKeycloakAdminClient(
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build(),
            new com.fasterxml.jackson.databind.ObjectMapper(),
            environment.keycloakUrl(),
            environment.keycloakAdminUsername(),
            environment.keycloakAdminPassword());
    var userReference =
        keycloak.provisionTenantOwner(
            new RealmConfiguration(
                environment.ownerUsername(),
                environment.ownerPassword(),
                tenantId,
                environment.tenantSlug(),
                environment.webOrigin()));

    tenantSeeder.activateOwnerMembership(tenantId, userReference);
    System.out.printf(
        "Provisioned tenant-owner E2E environment for tenant %s (%s)%n",
        environment.tenantSlug(), tenantId);
  }

  private record Environment(
      String keycloakUrl,
      String keycloakAdminUsername,
      String keycloakAdminPassword,
      String ownerUsername,
      String ownerPassword,
      String tenantSlug,
      String tenantName,
      String webOrigin,
      String databaseUrl,
      String databaseUsername,
      String databasePassword) {

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
          required("E2E_DATABASE_URL", "jdbc:postgresql://127.0.0.1:5432/emme"),
          required("E2E_DATABASE_USERNAME", "emme"),
          required("E2E_DATABASE_PASSWORD", "emme"));
    }

    private static String required(String name) {
      var value = System.getenv(name);
      if (value == null || value.isBlank()) {
        throw new IllegalArgumentException(name + " must be configured");
      }
      return value;
    }

    private static String required(String name, String fallback) {
      var value = System.getenv(name);
      return value == null || value.isBlank() ? fallback : value;
    }
  }
}
