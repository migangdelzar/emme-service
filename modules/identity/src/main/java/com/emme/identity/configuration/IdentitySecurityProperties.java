package com.emme.identity.configuration;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Typed, externally configurable security settings owned by Identity. */
@ConfigurationProperties(prefix = "app.identity.security")
public class IdentitySecurityProperties {

  private List<String> allowedOrigins =
      List.of(
          "http://localhost:5173",
          "http://localhost:3000",
          "http://localhost:8100",
          "capacitor://localhost");
  private List<String> allowedMethods = List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
  private List<String> allowedHeaders = List.of("Authorization", "Content-Type", "X-Tenant-Id");
  private boolean allowCredentials = true;
  private long maxAgeSeconds = 3600L;
  private String logoutSuccessUrl =
      "http://localhost:18080/realms/emme/protocol/openid-connect/logout"
          + "?redirect_uri=http://localhost:3000";
  private String cspConnectSource = "http://localhost:*";

  public List<String> getAllowedOrigins() {
    return allowedOrigins;
  }

  public void setAllowedOrigins(List<String> allowedOrigins) {
    this.allowedOrigins = List.copyOf(allowedOrigins);
  }

  public List<String> getAllowedMethods() {
    return allowedMethods;
  }

  public void setAllowedMethods(List<String> allowedMethods) {
    this.allowedMethods = List.copyOf(allowedMethods);
  }

  public List<String> getAllowedHeaders() {
    return allowedHeaders;
  }

  public void setAllowedHeaders(List<String> allowedHeaders) {
    this.allowedHeaders = List.copyOf(allowedHeaders);
  }

  public boolean isAllowCredentials() {
    return allowCredentials;
  }

  public void setAllowCredentials(boolean allowCredentials) {
    this.allowCredentials = allowCredentials;
  }

  public long getMaxAgeSeconds() {
    return maxAgeSeconds;
  }

  public void setMaxAgeSeconds(long maxAgeSeconds) {
    this.maxAgeSeconds = maxAgeSeconds;
  }

  public String getLogoutSuccessUrl() {
    return logoutSuccessUrl;
  }

  public void setLogoutSuccessUrl(String logoutSuccessUrl) {
    this.logoutSuccessUrl = logoutSuccessUrl;
  }

  public String getCspConnectSource() {
    return cspConnectSource;
  }

  public void setCspConnectSource(String cspConnectSource) {
    this.cspConnectSource = cspConnectSource;
  }
}
