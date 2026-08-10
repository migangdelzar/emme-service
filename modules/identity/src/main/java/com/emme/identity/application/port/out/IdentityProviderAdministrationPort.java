package com.emme.identity.application.port.out;

import java.io.IOException;
import java.util.List;

/** Identity-provider administration capabilities required by realm provisioning. */
public interface IdentityProviderAdministrationPort {

  void createRealm(String realmName, String displayName) throws IOException;

  void createClient(String realm, String clientId, List<String> redirectUris) throws IOException;

  void createRealmRole(String realm, String roleName) throws IOException;

  String createUser(String realm, String username, String email, String password, String roleName)
      throws IOException;
}
