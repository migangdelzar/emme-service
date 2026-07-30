package com.emme.tenancy.entity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, UUID> {

  Optional<Tenant> findBySlug(String slug);

  boolean existsBySlug(String slug);

  List<Tenant> findByStatus(String status);

  @Query("SELECT t.databaseId FROM Tenant t WHERE t.id = :tenantId")
  Optional<UUID> findDatabaseIdByTenantId(@Param("tenantId") UUID tenantId);
}
