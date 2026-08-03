package com.emme.identity.application.port.out;

import java.util.List;

/** Immutable, framework-free settings used by tenant Identity-provider provisioning. */
public record IdentityRealmProvisioningSettings(
    String clientId,
    List<String> redirectUris,
    String initialAdminUsername,
    String initialAdminPassword,
    String initialAdminRole,
    List<String> defaultRoles,
    int maxAttempts,
    long retryDelayMillis) {

  public IdentityRealmProvisioningSettings {
    redirectUris = List.copyOf(redirectUris);
    defaultRoles = List.copyOf(defaultRoles);
  }
}
