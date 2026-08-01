package com.emme.identity.adapter.out.persistence.repository;

import com.emme.identity.adapter.out.persistence.entity.Role;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataRoleRepository extends JpaRepository<Role, UUID> {
  Optional<Role> findByCode(String code);
}
