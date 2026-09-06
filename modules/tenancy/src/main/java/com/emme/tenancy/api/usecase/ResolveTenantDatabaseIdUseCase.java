package com.emme.tenancy.api.usecase;

import java.util.UUID;

public interface ResolveTenantDatabaseIdUseCase {
  UUID resolve(UUID tenantId);
}
