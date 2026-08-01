package com.emme.identity.infrastructure;

import com.emme.tenancy.api.event.TenantCreated;
import com.emme.tenancy.api.usecase.TenantApi;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class KeycloakRealmProvisioner {

  private static final Logger log = LoggerFactory.getLogger(KeycloakRealmProvisioner.class);
  private static final List<String> DEFAULT_ROLES =
      List.of("business_owner", "nail_artist", "front_desk", "read_only");
  private static final int MAX_RETRIES = 3;

  private final KeycloakAdminClient kc;
  private final TenantApi tenantApi;

  public KeycloakRealmProvisioner(KeycloakAdminClient kc, TenantApi tenantApi) {
    this.kc = kc;
    this.tenantApi = tenantApi;
  }

  @EventListener
  @Transactional
  public void onTenantCreated(TenantCreated event) {
    String realm = "emme-" + event.slug();
    log.info("Provisioning Keycloak realm: {}", realm);

    for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
      try {
        kc.createRealm(realm, event.name());
        log.info("  Realm created: {}", realm);

        kc.createClient(
            realm, "emme-salon-app", List.of("http://localhost:8080/*", "http://localhost:3000/*"));
        log.info("  Client created: emme-salon-app");

        for (String role : DEFAULT_ROLES) {
          kc.createRealmRole(realm, role);
        }
        log.info("  Roles seeded: {}", DEFAULT_ROLES);

        kc.createUser(realm, "admin", event.adminEmail(), "admin123", "business_owner");
        log.info("  Admin user created: {}", event.adminEmail());

        tenantApi.updateIdentityRealm(event.tenantId(), realm);
        log.info("Tenant {} provisioned with realm {}", event.slug(), realm);
        return;

      } catch (Exception e) {
        log.warn(
            "Realm provisioning attempt {}/{} failed for {}: {}",
            attempt,
            MAX_RETRIES,
            event.slug(),
            e.getMessage());
        if (attempt == MAX_RETRIES) {
          throw new RuntimeException(
              "Failed to provision realm for tenant "
                  + event.slug()
                  + " after "
                  + MAX_RETRIES
                  + " attempts",
              e);
        }
        try {
          Thread.sleep(2000L * attempt);
        } catch (InterruptedException ie) {
          Thread.currentThread().interrupt();
          return;
        }
      }
    }
  }
}
