package com.emme.tenancy.application.service;

import com.emme.tenancy.api.usecase.ResolveTenantDatabaseIdUseCase;
import com.emme.tenancy.application.port.out.TenantRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ResolveTenantDatabaseIdService implements ResolveTenantDatabaseIdUseCase {
  private final TenantRepository repository;

  public ResolveTenantDatabaseIdService(TenantRepository repository) {
    this.repository = repository;
  }

  @Override
  public UUID resolve(UUID tenantId) {
    return repository
        .findDatabaseIdByTenantId(tenantId)
        .orElseThrow(() -> new IllegalArgumentException("Tenant database not found: " + tenantId));
  }
}
