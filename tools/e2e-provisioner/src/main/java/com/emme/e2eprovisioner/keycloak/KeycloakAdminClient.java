package com.emme.e2eprovisioner.keycloak;

import java.io.IOException;

/** Port used by the E2E provisioner to create and inspect Keycloak identities. */
public interface KeycloakAdminClient {

  String provisionTenantOwner(RealmConfiguration configuration)
      throws IOException, InterruptedException;

  String createUser(String realm, String username, String email, String password,
      String firstName, String lastName, String role, java.util.UUID tenantId, String tenantSlug)
      throws IOException, InterruptedException;
}
