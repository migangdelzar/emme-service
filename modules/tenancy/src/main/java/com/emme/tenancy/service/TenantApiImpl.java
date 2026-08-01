package com.emme.tenancy.service;

import com.emme.tenancy.api.result.TenantInfo;
import com.emme.tenancy.api.usecase.TenantApi;
import com.emme.tenancy.entity.Tenant;
import com.emme.tenancy.entity.TenantRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class TenantApiImpl implements TenantApi {

  private final TenantRepository tenantRepository;

  TenantApiImpl(TenantRepository tenantRepository) {
    this.tenantRepository = tenantRepository;
  }

  @Override
  public TenantInfo getTenantInfo(UUID tenantId) {
    Tenant t =
        tenantRepository
            .findById(tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + tenantId));
    return toTenantInfo(t);
  }

  @Override
  public List<TenantInfo> getAllTenants() {
    return tenantRepository.findAll().stream().map(this::toTenantInfo).toList();
  }

  @Override
  public List<TenantInfo> getActiveTenants() {
    return tenantRepository.findByStatus("ACTIVE").stream().map(this::toTenantInfo).toList();
  }

  @Override
  public UUID getTenantIdBySlug(String slug) {
    return tenantRepository
        .findBySlug(slug)
        .map(Tenant::getId)
        .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + slug));
  }

  @Override
  @Transactional
  public void updateIdentityRealm(UUID tenantId, String identityRealm) {
    Tenant tenant =
        tenantRepository
            .findById(tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + tenantId));
    tenant.setKeycloakRealm(identityRealm);
  }

  private TenantInfo toTenantInfo(Tenant t) {
    return new TenantInfo(
        t.getId(),
        t.getSlug(),
        t.getName(),
        null,
        t.getStatus().name(),
        null,
        t.getKeycloakRealm());
  }
}
