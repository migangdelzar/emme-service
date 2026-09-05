package com.emme.services.adapter.out.persistence.repository;

import com.emme.services.adapter.out.persistence.entity.ServiceEntity;
import com.emme.services.domain.model.ServiceStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataServiceRepository extends JpaRepository<ServiceEntity, UUID> {
  List<ServiceEntity> findByTenantId(UUID tenantId);

  List<ServiceEntity> findByTenantIdAndStatus(UUID tenantId, ServiceStatus status);

  Optional<ServiceEntity> findByTenantIdAndCode(UUID tenantId, String code);

  Optional<ServiceEntity> findByTenantIdAndId(UUID tenantId, UUID id);
}
