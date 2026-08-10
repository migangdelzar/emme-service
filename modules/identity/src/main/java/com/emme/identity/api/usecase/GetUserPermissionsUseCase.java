package com.emme.identity.api.usecase;

import java.util.Set;
import java.util.UUID;

/** Public Identity capability for resolving permissions in a tenant context. */
public interface GetUserPermissionsUseCase {

  Set<String> getPermissions(String userReference, UUID tenantId);
}
