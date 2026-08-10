package com.emme.identity.configuration;

import com.emme.identity.application.port.out.IdentityRealmProvisioningConfigurationPort;
import com.emme.identity.application.port.out.IdentityRealmProvisioningSettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Maps Spring provisioning properties to the application-owned configuration boundary. */
@Configuration
public class IdentityProvisioningConfiguration {

  @Bean
  public IdentityRealmProvisioningConfigurationPort identityRealmProvisioningConfiguration(
      IdentityRealmProvisioningProperties properties) {
    IdentityRealmProvisioningSettings settings =
        new IdentityRealmProvisioningSettings(
            properties.clientId(),
            properties.redirectUris(),
            properties.initialAdminUsername(),
            properties.initialAdminPassword(),
            properties.initialAdminRole(),
            properties.initialOwnerUsername(),
            properties.initialOwnerPassword(),
            properties.initialOwnerRole(),
            properties.defaultRoles(),
            properties.maxAttempts(),
            properties.retryDelayMillis());
    return () -> settings;
  }
}
