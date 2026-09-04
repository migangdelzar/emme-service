package com.emme.identity.application.port.out;

import com.emme.identity.domain.model.Role;
import java.util.Optional;
import java.util.UUID;

/** Role persistence capability required by Identity membership workflows. */
public interface RoleRepository {

  Role save(Role role);

  Optional<Role> findById(UUID roleId);

  Optional<Role> findByCode(String code);
}
