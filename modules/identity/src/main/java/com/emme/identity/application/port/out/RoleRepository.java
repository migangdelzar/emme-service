package com.emme.identity.application.port.out;

import com.emme.identity.domain.model.Role;
import java.util.Optional;
import java.util.UUID;

/** Role lookup capability required when creating an Identity membership. */
public interface RoleRepository {

  Optional<Role> findById(UUID roleId);
}
