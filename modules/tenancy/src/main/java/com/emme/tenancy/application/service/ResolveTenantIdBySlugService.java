package com.emme.tenancy.application.service;

import com.emme.tenancy.api.query.ResolveTenantIdBySlugQuery;
import com.emme.tenancy.api.usecase.ResolveTenantIdBySlugUseCase;
import com.emme.tenancy.application.port.out.TenantRepository;
import com.emme.tenancy.domain.model.Tenant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ResolveTenantIdBySlugService implements ResolveTenantIdBySlugUseCase {
  private final TenantRepository repository;

  public ResolveTenantIdBySlugService(TenantRepository repository) {
    this.repository = repository;
  }

  @Override
  public UUID resolve(ResolveTenantIdBySlugQuery query) {
    return repository
        .findBySlug(query.slug())
        .map(Tenant::id)
        .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + query.slug()));
  }
}
