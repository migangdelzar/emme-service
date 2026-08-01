package com.emme.identity.application.process;

import com.emme.identity.api.usecase.ProvisionTenantIdentityUseCase;
import com.emme.identity.application.port.out.IdentityProviderAdministrationPort;
import com.emme.identity.application.port.out.IdentityRealmProvisioningConfigurationPort;
import com.emme.identity.application.port.out.IdentityRealmProvisioningSettings;
import com.emme.identity.application.port.out.RetryDelayPort;
import com.emme.tenancy.api.event.TenantCreated;
import com.emme.tenancy.api.usecase.TenantApi;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class KeycloakRealmProvisioningProcessManager implements ProvisionTenantIdentityUseCase {

  private static final Logger log =
      LoggerFactory.getLogger(KeycloakRealmProvisioningProcessManager.class);
  private final IdentityProviderAdministrationPort administrationPort;
  private final TenantApi tenantApi;
  private final IdentityRealmProvisioningSettings settings;
  private final RetryDelayPort retryDelayPort;

  public KeycloakRealmProvisioningProcessManager(
      IdentityProviderAdministrationPort administrationPort,
      TenantApi tenantApi,
      IdentityRealmProvisioningConfigurationPort configuration,
      RetryDelayPort retryDelayPort) {
    this.administrationPort = administrationPort;
    this.tenantApi = tenantApi;
    this.settings = configuration.settings();
    this.retryDelayPort = retryDelayPort;
  }

  @Override
  public void provision(TenantCreated event) {
    validateConfiguration();
    String realm = "emme-" + event.slug();
    log.info("Provisioning Keycloak realm: {}", realm);

    for (int attempt = 1; attempt <= settings.maxAttempts(); attempt++) {
      try {
        administrationPort.createRealm(realm, event.name());
        log.info("  Realm created: {}", realm);

        administrationPort.createClient(realm, settings.clientId(), settings.redirectUris());
        log.info("  Client created: {}", settings.clientId());

        for (String role : settings.defaultRoles()) {
          administrationPort.createRealmRole(realm, role);
        }
        log.info("  Roles seeded: {}", settings.defaultRoles());

        administrationPort.createUser(
            realm,
            settings.initialAdminUsername(),
            event.adminEmail(),
            settings.initialAdminPassword(),
            settings.initialAdminRole());
        log.info("  Admin user created: {}", event.adminEmail());

        tenantApi.updateIdentityRealm(event.tenantId(), realm);
        log.info("Tenant {} provisioned with realm {}", event.slug(), realm);
        return;

      } catch (Exception e) {
        log.warn(
            "Realm provisioning attempt {}/{} failed for {}: {}",
            attempt,
            settings.maxAttempts(),
            event.slug(),
            e.getMessage());
        if (attempt == settings.maxAttempts()) {
          throw new RuntimeException(
              "Failed to provision realm for tenant "
                  + event.slug()
                  + " after "
                  + settings.maxAttempts()
                  + " attempts",
              e);
        }
        try {
          retryDelayPort.await(Duration.ofMillis(settings.retryDelayMillis() * attempt));
        } catch (InterruptedException ie) {
          Thread.currentThread().interrupt();
          return;
        }
      }
    }
  }

  private void validateConfiguration() {
    if (settings.initialAdminPassword() == null || settings.initialAdminPassword().isBlank()) {
      throw new IllegalStateException("Identity realm provisioning password is not configured");
    }
    if (settings.maxAttempts() < 1) {
      throw new IllegalStateException("Identity realm provisioning max attempts must be positive");
    }
    if (settings.retryDelayMillis() < 0) {
      throw new IllegalStateException("Identity realm provisioning retry delay cannot be negative");
    }
  }
}
