package com.emme.identity.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Typed Identity settings used by application-level Keycloak orchestration. */
@ConfigurationProperties(prefix = "app.keycloak")
public class IdentityKeycloakProperties {

  private String defaultRealm = "emme";

  public String getDefaultRealm() {
    return defaultRealm;
  }

  public void setDefaultRealm(String defaultRealm) {
    this.defaultRealm = defaultRealm;
  }
}
