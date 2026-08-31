package com.emme.tenancy.adapter.out.persistence.repository;

import com.emme.tenancy.adapter.out.persistence.entity.TenantEntity;
import com.emme.tenancy.domain.model.TenantStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataTenantRepository extends JpaRepository<TenantEntity, UUID> {

  Optional<TenantEntity> findBySlug(String slug);

  Optional<TenantEntity> findByKeycloakRealm(String keycloakRealm);

  boolean existsBySlug(String slug);

  List<TenantEntity> findByStatusOrderByIdAsc(TenantStatus status);

  @Query("SELECT t.databaseId FROM TenantEntity t WHERE t.id = :tenantId")
  Optional<UUID> findDatabaseIdByTenantId(@Param("tenantId") UUID tenantId);
}
