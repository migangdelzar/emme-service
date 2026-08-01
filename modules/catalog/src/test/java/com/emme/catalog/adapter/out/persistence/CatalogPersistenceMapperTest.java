package com.emme.catalog.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.catalog.adapter.out.persistence.entity.CatalogItemEntity;
import com.emme.catalog.domain.model.CatalogItem;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CatalogPersistenceMapperTest {

  @Test
  void mapsCatalogItemWithoutMakingTheDomainModelAPersistenceEntity() {
    CatalogItem item =
        new CatalogItem(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "GEL-001",
            "Structured gel",
            "A durable finish",
            new BigDecimal("35.00"),
            "from",
            60,
            "gel");

    CatalogItem rehydrated = CatalogItemEntity.from(item).toDomain();

    assertThat(rehydrated.getId()).isEqualTo(item.getId());
    assertThat(rehydrated.getTenantId()).isEqualTo(item.getTenantId());
    assertThat(rehydrated.getName()).isEqualTo(item.getName());
    assertThat(rehydrated.getPrice()).isEqualByComparingTo(item.getPrice());
  }
}
