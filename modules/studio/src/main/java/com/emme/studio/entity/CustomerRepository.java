package com.emme.studio.entity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID> {
  List<Customer> findByTenantId(UUID tenantId);

  Optional<Customer> findByTenantIdAndPhone(UUID tenantId, String phone);

  Optional<Customer> findByTenantIdAndEmail(UUID tenantId, String email);

  List<Customer> findByTenantIdAndNameContainingIgnoreCase(UUID tenantId, String name);
}
