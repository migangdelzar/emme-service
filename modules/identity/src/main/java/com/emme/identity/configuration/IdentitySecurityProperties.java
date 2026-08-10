package com.emme.identity.configuration;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/** Typed, externally configurable security settings owned by Identity. */
@Validated
@ConfigurationProperties(prefix = "app.identity.security")
public record IdentitySecurityProperties(
    @NotEmpty List<@NotBlank String> allowedOrigins,
    @NotEmpty List<@NotBlank String> allowedMethods,
    @NotEmpty List<@NotBlank String> allowedHeaders,
    boolean allowCredentials,
    @Min(0) long maxAgeSeconds,
    @NotBlank String logoutSuccessUrl,
    @NotBlank String cspConnectSource) {

  public IdentitySecurityProperties(
      @DefaultValue({
            "http://localhost:5173",
            "http://localhost:3000",
            "http://localhost:8100",
            "capacitor://localhost"
          })
          List<String> allowedOrigins,
      @DefaultValue({"GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"})
          List<String> allowedMethods,
      @DefaultValue({"Authorization", "Content-Type", "X-Tenant-Id"}) List<String> allowedHeaders,
      @DefaultValue("true") boolean allowCredentials,
      @DefaultValue("3600") long maxAgeSeconds,
      @DefaultValue(
              "http://localhost:18080/realms/emme/protocol/openid-connect/logout?redirect_uri=http://localhost:3000")
          String logoutSuccessUrl,
      @DefaultValue("http://localhost:*") String cspConnectSource) {
    this.allowedOrigins = List.copyOf(Objects.requireNonNull(allowedOrigins, "allowedOrigins"));
    this.allowedMethods = List.copyOf(Objects.requireNonNull(allowedMethods, "allowedMethods"));
    this.allowedHeaders = List.copyOf(Objects.requireNonNull(allowedHeaders, "allowedHeaders"));
    this.allowCredentials = allowCredentials;
    this.maxAgeSeconds = maxAgeSeconds;
    this.logoutSuccessUrl = logoutSuccessUrl;
    this.cspConnectSource = cspConnectSource;
  }

  public static IdentitySecurityProperties defaults() {
    return new IdentitySecurityProperties(
        List.of(
            "http://localhost:5173",
            "http://localhost:3000",
            "http://localhost:8100",
            "capacitor://localhost"),
        List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"),
        List.of("Authorization", "Content-Type", "X-Tenant-Id"),
        true,
        3600L,
        "http://localhost:18080/realms/emme/protocol/openid-connect/logout?redirect_uri=http://localhost:3000",
        "http://localhost:*");
  }
}
