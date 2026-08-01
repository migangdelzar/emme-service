package com.emme.identity.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Typed Identity settings used by application-level Keycloak orchestration. */
@ConfigurationProperties(prefix = "app.keycloak")
public class IdentityKeycloakProperties {

  private String baseUrl = "http://localhost:18080";
  private String issuerUri = "http://localhost:18080/realms/emme";
  private String clientId = "emme-salon-app";
  private String adminRealm = "master";
  private String adminUsername = "admin";
  private String adminPassword = "";
  private String defaultRealm = "emme";

  public String getBaseUrl() {
    return baseUrl;
  }

  public void setBaseUrl(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  public String getIssuerUri() {
    return issuerUri;
  }

  public void setIssuerUri(String issuerUri) {
    this.issuerUri = issuerUri;
  }

  public String getClientId() {
    return clientId;
  }

  public void setClientId(String clientId) {
    this.clientId = clientId;
  }

  public String getAdminRealm() {
    return adminRealm;
  }

  public void setAdminRealm(String adminRealm) {
    this.adminRealm = adminRealm;
  }

  public String getAdminUsername() {
    return adminUsername;
  }

  public void setAdminUsername(String adminUsername) {
    this.adminUsername = adminUsername;
  }

  public String getAdminPassword() {
    return adminPassword;
  }

  public void setAdminPassword(String adminPassword) {
    this.adminPassword = adminPassword;
  }

  public String getDefaultRealm() {
    return defaultRealm;
  }

  public void setDefaultRealm(String defaultRealm) {
    this.defaultRealm = defaultRealm;
  }
}
