package com.emme.identity.application.process;

import com.emme.identity.application.port.out.IdentityProviderAdministrationPort;
import com.emme.identity.application.port.out.RetryDelayPort;
import com.emme.identity.configuration.IdentityRealmProvisioningProperties;
import com.emme.tenancy.api.event.TenantCreated;
import com.emme.tenancy.api.usecase.TenantApi;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class KeycloakRealmProvisioningProcessManager {

  private static final Logger log =
      LoggerFactory.getLogger(KeycloakRealmProvisioningProcessManager.class);
  private final IdentityProviderAdministrationPort administrationPort;
  private final TenantApi tenantApi;
  private final IdentityRealmProvisioningProperties properties;
  private final RetryDelayPort retryDelayPort;

  public KeycloakRealmProvisioningProcessManager(
      IdentityProviderAdministrationPort administrationPort,
      TenantApi tenantApi,
      IdentityRealmProvisioningProperties properties,
      RetryDelayPort retryDelayPort) {
    this.administrationPort = administrationPort;
    this.tenantApi = tenantApi;
    this.properties = properties;
    this.retryDelayPort = retryDelayPort;
  }

  public void provision(TenantCreated event) {
    validateConfiguration();
    String realm = "emme-" + event.slug();
    log.info("Provisioning Keycloak realm: {}", realm);

    for (int attempt = 1; attempt <= properties.getMaxAttempts(); attempt++) {
      try {
        administrationPort.createRealm(realm, event.name());
        log.info("  Realm created: {}", realm);

        administrationPort.createClient(
            realm, properties.getClientId(), properties.getRedirectUris());
        log.info("  Client created: {}", properties.getClientId());

        for (String role : properties.getDefaultRoles()) {
          administrationPort.createRealmRole(realm, role);
        }
        log.info("  Roles seeded: {}", properties.getDefaultRoles());

        administrationPort.createUser(
            realm,
            properties.getInitialAdminUsername(),
            event.adminEmail(),
            properties.getInitialAdminPassword(),
            properties.getInitialAdminRole());
        log.info("  Admin user created: {}", event.adminEmail());

        tenantApi.updateIdentityRealm(event.tenantId(), realm);
        log.info("Tenant {} provisioned with realm {}", event.slug(), realm);
        return;

      } catch (Exception e) {
        log.warn(
            "Realm provisioning attempt {}/{} failed for {}: {}",
            attempt,
            properties.getMaxAttempts(),
            event.slug(),
            e.getMessage());
        if (attempt == properties.getMaxAttempts()) {
          throw new RuntimeException(
              "Failed to provision realm for tenant "
                  + event.slug()
                  + " after "
                  + properties.getMaxAttempts()
                  + " attempts",
              e);
        }
        try {
          retryDelayPort.await(Duration.ofMillis(properties.getRetryDelayMillis() * attempt));
        } catch (InterruptedException ie) {
          Thread.currentThread().interrupt();
          return;
        }
      }
    }
  }

  private void validateConfiguration() {
    if (properties.getInitialAdminPassword() == null
        || properties.getInitialAdminPassword().isBlank()) {
      throw new IllegalStateException("Identity realm provisioning password is not configured");
    }
    if (properties.getMaxAttempts() < 1) {
      throw new IllegalStateException("Identity realm provisioning max attempts must be positive");
    }
    if (properties.getRetryDelayMillis() < 0) {
      throw new IllegalStateException("Identity realm provisioning retry delay cannot be negative");
    }
  }
}
