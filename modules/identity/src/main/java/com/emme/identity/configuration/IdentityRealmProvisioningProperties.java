package com.emme.identity.configuration;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/** Typed settings for provisioning a tenant's Identity provider realm. */
@Validated
@ConfigurationProperties(prefix = "app.keycloak.provisioning")
public record IdentityRealmProvisioningProperties(
    @NotBlank String clientId,
    @NotEmpty List<@NotBlank String> redirectUris,
    @NotBlank String initialAdminUsername,
    String initialAdminPassword,
    @NotBlank String initialAdminRole,
    @NotEmpty List<@NotBlank String> defaultRoles,
    @Min(1) int maxAttempts,
    @Min(0) long retryDelayMillis) {

  public IdentityRealmProvisioningProperties(
      @DefaultValue("salon-app") String clientId,
      @DefaultValue({"http://localhost:8080/*", "http://localhost:3000/*"})
          List<String> redirectUris,
      @DefaultValue("admin") String initialAdminUsername,
      @DefaultValue("") String initialAdminPassword,
      @DefaultValue("business_owner") String initialAdminRole,
      @DefaultValue({"business_owner", "nail_artist", "front_desk", "read_only"})
          List<String> defaultRoles,
      @DefaultValue("3") int maxAttempts,
      @DefaultValue("2000") long retryDelayMillis) {
    this.clientId = clientId;
    this.redirectUris = List.copyOf(Objects.requireNonNull(redirectUris, "redirectUris"));
    this.initialAdminUsername = initialAdminUsername;
    this.initialAdminPassword = initialAdminPassword;
    this.initialAdminRole = initialAdminRole;
    this.defaultRoles = List.copyOf(Objects.requireNonNull(defaultRoles, "defaultRoles"));
    this.maxAttempts = maxAttempts;
    this.retryDelayMillis = retryDelayMillis;
  }

  public static IdentityRealmProvisioningProperties defaults() {
    return new IdentityRealmProvisioningProperties(
        "salon-app",
        List.of("http://localhost:8080/*", "http://localhost:3000/*"),
        "admin",
        "",
        "business_owner",
        List.of("business_owner", "nail_artist", "front_desk", "read_only"),
        3,
        2_000L);
  }
}
