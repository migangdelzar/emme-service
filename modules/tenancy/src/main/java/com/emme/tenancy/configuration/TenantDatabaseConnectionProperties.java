package com.emme.tenancy.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Typed connection settings used when creating tenant database pools. */
@ConfigurationProperties(prefix = "spring.datasource.tenant")
public record TenantDatabaseConnectionProperties(
    String url, String username, String password, String driverClassName) {

  public TenantDatabaseConnectionProperties(
      @DefaultValue("") String url,
      @DefaultValue("emme") String username,
      @DefaultValue("emme") String password,
      @DefaultValue("org.postgresql.Driver") String driverClassName) {
    this.url = url == null ? "" : url;
    this.username = username == null ? "emme" : username;
    this.password = password == null ? "emme" : password;
    this.driverClassName = driverClassName == null ? "org.postgresql.Driver" : driverClassName;
  }

  public static TenantDatabaseConnectionProperties defaults() {
    return new TenantDatabaseConnectionProperties("", "emme", "emme", "org.postgresql.Driver");
  }
}
