package com.emme.identity.application.port.out;

import java.util.Set;
import java.util.UUID;

/** Authorization capability required to resolve a user's permissions in a tenant. */
@FunctionalInterface
public interface PermissionPort {

  Set<String> findPermissionCodesForUserInTenant(String userReference, UUID tenantId);
}
