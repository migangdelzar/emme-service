package com.emme.tenancy.api.usecase;

import java.util.UUID;

/** Creates a tenant membership linking a Keycloak user to a tenant role. */
@FunctionalInterface
public interface EnsureTenantMembershipUseCase {
  void ensure(UUID tenantId, String userReference, String roleCode);
}
