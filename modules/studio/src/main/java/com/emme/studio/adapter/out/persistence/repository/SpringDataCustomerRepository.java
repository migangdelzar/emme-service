package com.emme.studio.adapter.out.persistence.repository;

import com.emme.studio.adapter.out.persistence.entity.CustomerEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataCustomerRepository extends JpaRepository<CustomerEntity, UUID> {
  List<CustomerEntity> findByTenantId(UUID tenantId);

  Optional<CustomerEntity> findByTenantIdAndPhone(UUID tenantId, String phone);

  Optional<CustomerEntity> findByTenantIdAndEmail(UUID tenantId, String email);

  List<CustomerEntity> findByTenantIdAndNameContainingIgnoreCase(UUID tenantId, String name);
}
