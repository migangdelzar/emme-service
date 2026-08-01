package com.emme.identity.adapter.out.persistence.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.identity.adapter.out.persistence.entity.PermissionEntity;
import com.emme.identity.domain.model.Permission;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PermissionPersistenceMapperTest {

  private final PermissionPersistenceMapper mapper = new PermissionPersistenceMapper();

  @Test
  void mapsPermissionEntityToDomainWithoutExposingJpaTypes() {
    PermissionEntity entity =
        PermissionEntity.restore(
            UUID.randomUUID(),
            "quotes.read",
            "Read quotes",
            "Read quote data",
            true,
            Instant.EPOCH,
            Instant.EPOCH);

    Permission permission = mapper.toDomain(entity);

    assertThat(permission.id()).isEqualTo(entity.getId());
    assertThat(permission.code()).isEqualTo("quotes.read");
    assertThat(permission.description()).isEqualTo("Read quote data");
  }

  @Test
  void mapsNewPermissionToEntity() {
    Permission permission = new Permission("quotes.read", "Read quotes", "Read quote data");

    PermissionEntity entity = mapper.toEntity(permission);

    assertThat(entity.getCode()).isEqualTo("quotes.read");
    assertThat(entity.getDescription()).isEqualTo("Read quote data");
  }
}
