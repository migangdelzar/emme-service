package com.emme.identity.configuration;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/** Typed Identity settings used by application-level Keycloak orchestration. */
@Validated
@ConfigurationProperties(prefix = "app.keycloak")
public record IdentityKeycloakProperties(
    @NotBlank String baseUrl,
    @NotBlank String issuerUri,
    String jwkSetBaseUrl,
    @NotBlank String clientId,
    @NotBlank String adminRealm,
    @NotBlank String adminUsername,
    String adminPassword,
    @NotBlank String defaultRealm,
    @NotBlank String customerIssuerUri,
    @NotBlank String customerClientId) {

  public IdentityKeycloakProperties(
      @DefaultValue("http://localhost:18080") String baseUrl,
      @DefaultValue("http://localhost:18080/realms/emme") String issuerUri,
      @DefaultValue("") String jwkSetBaseUrl,
      @DefaultValue("emme-salon-app") String clientId,
      @DefaultValue("master") String adminRealm,
      @DefaultValue("admin") String adminUsername,
      @DefaultValue("") String adminPassword,
      @DefaultValue("emme") String defaultRealm,
      @DefaultValue("http://localhost:18080/realms/emme-customers") String customerIssuerUri,
      @DefaultValue("emme-customer-app") String customerClientId) {
    this.baseUrl = baseUrl;
    this.issuerUri = issuerUri;
    this.jwkSetBaseUrl = jwkSetBaseUrl;
    this.clientId = clientId;
    this.adminRealm = adminRealm;
    this.adminUsername = adminUsername;
    this.adminPassword = adminPassword;
    this.defaultRealm = defaultRealm;
    this.customerIssuerUri = customerIssuerUri;
    this.customerClientId = customerClientId;
  }

  public static IdentityKeycloakProperties defaults() {
    return new IdentityKeycloakProperties(
        "http://localhost:18080",
        "http://localhost:18080/realms/emme",
        "",
        "emme-salon-app",
        "master",
        "admin",
        "",
        "emme",
        "http://localhost:18080/realms/emme-customers",
        "emme-customer-app");
  }
}
