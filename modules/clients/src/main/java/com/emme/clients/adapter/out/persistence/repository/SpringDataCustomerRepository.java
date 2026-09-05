package com.emme.clients.adapter.out.persistence.repository;

import com.emme.clients.adapter.out.persistence.entity.CustomerEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataCustomerRepository extends JpaRepository<CustomerEntity, UUID> {
  Optional<CustomerEntity> findByEmail(String email);

  List<CustomerEntity> findByNameContainingIgnoreCase(String name);
}
