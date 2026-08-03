package com.emme.identity.application.port.out;

/** Configuration capability required by tenant Identity-provider provisioning. */
@FunctionalInterface
public interface IdentityRealmProvisioningConfigurationPort {

  IdentityRealmProvisioningSettings settings();
}
