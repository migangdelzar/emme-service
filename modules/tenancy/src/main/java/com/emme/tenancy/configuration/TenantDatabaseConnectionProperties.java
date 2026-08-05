package com.emme.tenancy.configuration;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "emme.tenancy.tenant-datasource")
public record TenantDatabaseConnectionProperties(
    @NotBlank String url,
    @NotBlank String username,
    @NotBlank String password,
    @NotBlank String driverClassName) {

  public TenantDatabaseConnectionProperties {
    if (driverClassName == null) driverClassName = "org.postgresql.Driver";
  }

  public static TenantDatabaseConnectionProperties defaults() {
    return new TenantDatabaseConnectionProperties("", "emme", "emme", "org.postgresql.Driver");
  }
}
