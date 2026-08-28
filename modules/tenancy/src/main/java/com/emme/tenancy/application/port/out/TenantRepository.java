package com.emme.tenancy.application.port.out;

import com.emme.tenancy.domain.model.Tenant;
import com.emme.tenancy.domain.model.TenantStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Persistence capability required by the Tenancy application layer. */
public interface TenantRepository {

  boolean existsBySlug(String slug);

  Tenant save(Tenant tenant);

  Optional<Tenant> findById(UUID tenantId);

  Optional<Tenant> findBySlug(String slug);

  Optional<Tenant> findByIdentityRealm(String identityRealm);

  List<Tenant> findAll();

  List<Tenant> findByStatus(TenantStatus status);

  Optional<UUID> findDatabaseIdByTenantId(UUID tenantId);
}
