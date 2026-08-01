package com.emme.identity.adapter.out.persistence.adapter;

import com.emme.identity.adapter.out.persistence.repository.SpringDataRoleRepository;
import com.emme.identity.application.port.out.RoleReference;
import com.emme.identity.application.port.out.RoleRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Implements Identity role lookup through the persistence adapter boundary. */
@Component
public final class RolePersistenceAdapter implements RoleRepository {

  private final SpringDataRoleRepository repository;

  public RolePersistenceAdapter(SpringDataRoleRepository repository) {
    this.repository = repository;
  }

  @Override
  public Optional<RoleReference> findById(UUID roleId) {
    return repository.findById(roleId).map(role -> new RoleReference(role.getId(), role.getCode()));
  }
}
