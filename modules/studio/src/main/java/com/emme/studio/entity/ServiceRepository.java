package com.emme.studio.entity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ServiceRepository extends JpaRepository<Service, UUID> {
  List<Service> findByTenantId(UUID tenantId);

  List<Service> findByTenantIdAndStatus(UUID tenantId, ServiceStatus status);

  Optional<Service> findByTenantIdAndCode(UUID tenantId, String code);
}
