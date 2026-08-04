package com.emme.e2eprovisioner.keycloak;

import java.io.IOException;

/** Port used by the E2E provisioner to create and inspect Keycloak identities. */
public interface KeycloakAdminClient {

  String provisionTenantOwner(RealmConfiguration configuration)
      throws IOException, InterruptedException;
}
