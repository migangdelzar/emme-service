package com.emme.tenancy.application.service;

import com.emme.tenancy.api.result.TenantInfo;
import com.emme.tenancy.api.usecase.TenantApi;
import com.emme.tenancy.application.port.out.TenantRepository;
import com.emme.tenancy.domain.model.Tenant;
import com.emme.tenancy.domain.model.TenantStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implements the public Tenant API with application-owned domain contracts. */
@Service
@Transactional(readOnly = true)
class TenantApiService implements TenantApi {

  private final TenantRepository tenantRepository;

  TenantApiService(TenantRepository tenantRepository) {
    this.tenantRepository = tenantRepository;
  }

  @Override
  public TenantInfo getTenantInfo(UUID tenantId) {
    Tenant tenant =
        tenantRepository
            .findById(tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + tenantId));
    return toTenantInfo(tenant);
  }

  @Override
  public List<TenantInfo> getAllTenants() {
    return tenantRepository.findAll().stream().map(this::toTenantInfo).toList();
  }

  @Override
  public List<TenantInfo> getActiveTenants() {
    return tenantRepository.findByStatus(TenantStatus.ACTIVE).stream()
        .map(this::toTenantInfo)
        .toList();
  }

  @Override
  public UUID getTenantIdBySlug(String slug) {
    return tenantRepository
        .findBySlug(slug)
        .map(Tenant::id)
        .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + slug));
  }

  @Override
  @Transactional
  public void updateIdentityRealm(UUID tenantId, String identityRealm) {
    Tenant tenant =
        tenantRepository
            .findById(tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + tenantId));
    tenant.changeIdentityRealm(identityRealm);
    tenantRepository.save(tenant);
  }

  private TenantInfo toTenantInfo(Tenant tenant) {
    return new TenantInfo(
        tenant.id(),
        tenant.slug(),
        tenant.name(),
        null,
        tenant.status().name(),
        null,
        tenant.keycloakRealm());
  }
}
