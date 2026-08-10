package com.emme.identity.adapter.out.persistence.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.identity.adapter.out.persistence.entity.RoleEntity;
import com.emme.identity.domain.model.Role;
import com.emme.identity.domain.model.RoleScope;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RolePersistenceMapperTest {

  private final RolePersistenceMapper mapper = new RolePersistenceMapper();

  @Test
  void mapsRoleEntityToDomainWithoutExposingJpaTypes() {
    RoleEntity entity =
        RoleEntity.restore(
            UUID.randomUUID(),
            "admin",
            "Administrator",
            RoleScope.PLATFORM,
            true,
            Instant.EPOCH,
            Instant.EPOCH);

    Role role = mapper.toDomain(entity);

    assertThat(role.id()).isEqualTo(entity.getId());
    assertThat(role.code()).isEqualTo("admin");
    assertThat(role.scope()).isEqualTo(RoleScope.PLATFORM);
  }

  @Test
  void mapsNewRoleToEntity() {
    Role role = new Role("staff", "Staff", RoleScope.TENANT);

    RoleEntity entity = mapper.toEntity(role);

    assertThat(entity.getCode()).isEqualTo("staff");
    assertThat(entity.getScope()).isEqualTo(RoleScope.TENANT);
  }
}
