package com.emme.identity.adapter.in.messaging.consumer;

import com.emme.identity.application.port.out.IdentityProviderAdministrationPort;
import com.emme.identity.application.port.out.IdentityRealmProvisioningConfigurationPort;
import com.emme.identity.application.port.out.IdentityRealmProvisioningSettings;
import com.emme.identity.application.port.out.TenantIdentityRealmPort;
import com.emme.tenancy.api.event.TenantRealmReady;
import com.emme.tenancy.api.event.TenantSchemaReady;
import com.emme.tenancy.api.usecase.EnsureTenantMembershipUseCase;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(
    name = "app.keycloak.provisioning.enabled",
    havingValue = "true",
    matchIfMissing = false)
public class TenantRealmProvisioningListener {

  private static final Logger log = LoggerFactory.getLogger(TenantRealmProvisioningListener.class);

  private final IdentityProviderAdministrationPort administrationPort;
  private final TenantIdentityRealmPort tenantIdentityRealmPort;
  private final IdentityRealmProvisioningSettings settings;
  private final ApplicationEventPublisher eventPublisher;
  private final EnsureTenantMembershipUseCase ensureMembership;

  public TenantRealmProvisioningListener(
      IdentityProviderAdministrationPort administrationPort,
      TenantIdentityRealmPort tenantIdentityRealmPort,
      IdentityRealmProvisioningConfigurationPort configuration,
      ApplicationEventPublisher eventPublisher,
      EnsureTenantMembershipUseCase ensureMembership) {
    this.administrationPort = administrationPort;
    this.tenantIdentityRealmPort = tenantIdentityRealmPort;
    this.settings = configuration.settings();
    this.eventPublisher = eventPublisher;
    this.ensureMembership = ensureMembership;
  }

  @ApplicationModuleListener(id = "identity.tenant-schema-ready.realm-provisioning")
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void onTenantSchemaReady(TenantSchemaReady event) {
    log.info("Provisioning Keycloak realm for tenant {} (slug={})", event.tenantId(), event.slug());

    String realm = "emme-" + event.slug();

    try {
      administrationPort.createRealm(realm, event.slug());

      String clientId = settings.clientId();
      administrationPort.createClient(realm, clientId, settings.redirectUris());

      for (String role : settings.defaultRoles()) {
        administrationPort.createRealmRole(realm, role);
      }

      String adminUsername = settings.initialAdminUsername();
      String adminEmail = adminUsername + "@" + event.slug() + ".local";
      String adminReference =
          administrationPort.createUser(
              realm,
              adminUsername,
              adminEmail,
              settings.initialAdminPassword(),
              settings.initialAdminRole());

      String ownerUsername = settings.initialOwnerUsername();
      String ownerEmail = ownerUsername + "@" + event.slug() + ".local";
      String ownerReference =
          administrationPort.createUser(
              realm,
              ownerUsername,
              ownerEmail,
              settings.initialOwnerPassword(),
              settings.initialOwnerRole());

      ensureMembership.ensure(event.tenantId(), adminReference, settings.initialAdminRole());
      ensureMembership.ensure(event.tenantId(), ownerReference, settings.initialOwnerRole());

      tenantIdentityRealmPort.updateRealm(event.tenantId(), realm);
      log.info("Keycloak realm {} provisioned for tenant {}", realm, event.tenantId());

      eventPublisher.publishEvent(
          new TenantRealmReady(UUID.randomUUID(), event.tenantId(), event.slug(), realm));
    } catch (Exception e) {
      log.error("Realm provisioning failed for tenant {}: {}", event.tenantId(), e.getMessage());
      throw new RuntimeException("Failed to provision realm for tenant " + event.slug(), e);
    }
  }
}
