package com.emme.tenancy.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Typed connection settings used when creating tenant database pools. */
@Component
@ConfigurationProperties(prefix = "spring.datasource")
public class TenantDatabaseConnectionProperties {

  private String url = "";
  private String username = "emme";
  private String password = "emme";
  private String driverClassName = "org.postgresql.Driver";

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getDriverClassName() {
    return driverClassName;
  }

  public void setDriverClassName(String driverClassName) {
    this.driverClassName = driverClassName;
  }
}
