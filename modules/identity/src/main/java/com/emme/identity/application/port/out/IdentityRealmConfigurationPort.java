package com.emme.identity.application.port.out;

/** Configuration capability required to select the default Identity realm. */
@FunctionalInterface
public interface IdentityRealmConfigurationPort {

  String defaultRealm();
}
