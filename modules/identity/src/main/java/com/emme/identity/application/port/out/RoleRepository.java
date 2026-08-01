package com.emme.identity.application.port.out;

import java.util.Optional;
import java.util.UUID;

/** Role lookup capability required when creating an Identity membership. */
public interface RoleRepository {

  Optional<RoleReference> findById(UUID roleId);
}
