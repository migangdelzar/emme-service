package com.emme.identity.configuration;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Typed settings for provisioning a tenant's Identity provider realm. */
@ConfigurationProperties(prefix = "app.keycloak.provisioning")
public class IdentityRealmProvisioningProperties {

  private String clientId = "emme-salon-app";
  private List<String> redirectUris = List.of("http://localhost:8080/*", "http://localhost:3000/*");
  private String initialAdminUsername = "admin";
  private String initialAdminPassword = "";
  private String initialAdminRole = "business_owner";
  private List<String> defaultRoles =
      List.of("business_owner", "nail_artist", "front_desk", "read_only");
  private int maxAttempts = 3;
  private long retryDelayMillis = 2_000L;

  public String getClientId() {
    return clientId;
  }

  public void setClientId(String clientId) {
    this.clientId = clientId;
  }

  public List<String> getRedirectUris() {
    return redirectUris;
  }

  public void setRedirectUris(List<String> redirectUris) {
    this.redirectUris = List.copyOf(redirectUris);
  }

  public String getInitialAdminUsername() {
    return initialAdminUsername;
  }

  public void setInitialAdminUsername(String initialAdminUsername) {
    this.initialAdminUsername = initialAdminUsername;
  }

  public String getInitialAdminPassword() {
    return initialAdminPassword;
  }

  public void setInitialAdminPassword(String initialAdminPassword) {
    this.initialAdminPassword = initialAdminPassword;
  }

  public String getInitialAdminRole() {
    return initialAdminRole;
  }

  public void setInitialAdminRole(String initialAdminRole) {
    this.initialAdminRole = initialAdminRole;
  }

  public List<String> getDefaultRoles() {
    return defaultRoles;
  }

  public void setDefaultRoles(List<String> defaultRoles) {
    this.defaultRoles = List.copyOf(defaultRoles);
  }

  public int getMaxAttempts() {
    return maxAttempts;
  }

  public void setMaxAttempts(int maxAttempts) {
    this.maxAttempts = maxAttempts;
  }

  public long getRetryDelayMillis() {
    return retryDelayMillis;
  }

  public void setRetryDelayMillis(long retryDelayMillis) {
    this.retryDelayMillis = retryDelayMillis;
  }
}
