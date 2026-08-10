package com.emme.identity.adapter.out.persistence.adapter;

import com.emme.identity.adapter.out.persistence.mapper.RolePersistenceMapper;
import com.emme.identity.adapter.out.persistence.repository.SpringDataRoleRepository;
import com.emme.identity.application.port.out.RoleRepository;
import com.emme.identity.domain.model.Role;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Implements Identity role lookup through the persistence adapter boundary. */
@Component
public class RolePersistenceAdapter implements RoleRepository {

  private final SpringDataRoleRepository repository;
  private final RolePersistenceMapper mapper;

  public RolePersistenceAdapter(SpringDataRoleRepository repository, RolePersistenceMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Override
  public Optional<Role> findById(UUID roleId) {
    return repository.findById(roleId).map(mapper::toDomain);
  }
}
