package com.emme.tenancy.entity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DatabaseRegistryRepository extends JpaRepository<DatabaseRegistry, UUID> {

  Optional<DatabaseRegistry> findByName(String name);

  List<DatabaseRegistry> findByIsActiveTrue();
}
