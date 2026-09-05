package com.emme.catalog.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.catalog.adapter.out.persistence.entity.CatalogItemEntity;
import com.emme.catalog.adapter.out.persistence.repository.SpringDataCatalogItemRepository;
import com.emme.catalog.domain.model.CatalogItem;
import com.emme.catalog.domain.model.CatalogItemStatus;
import com.emme.testing.BaseRepositoryTest;
import com.emme.testing.TestSecurityConfig;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import(TestSecurityConfig.class)
class CatalogRepositoryTest extends BaseRepositoryTest {

  @Autowired private SpringDataCatalogItemRepository itemRepo;

  private static final UUID TENANT_ID = UUID.randomUUID();

  @Test
  void shouldSaveAndFindItem() {
    CatalogItem item =
        new CatalogItem(
            TENANT_ID,
            UUID.randomUUID(),
            "REPO-001",
            "Repository Item",
            "A test item",
            new BigDecimal("19.99"),
            "per service",
            30,
            "gel, acetone");

    CatalogItemEntity savedEntity = itemRepo.save(CatalogItemEntity.from(item));
    CatalogItem saved = savedEntity.toDomain();
    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getStatus()).isEqualTo(CatalogItemStatus.ACTIVE);

    CatalogItem found = itemRepo.findById(saved.getId()).orElseThrow().toDomain();
    assertThat(found.getName()).isEqualTo("Repository Item");
    assertThat(found.getPrice()).isEqualByComparingTo(new BigDecimal("19.99"));
  }

  @Test
  void shouldFindByStatus() {
    CatalogItem item =
        new CatalogItem(
            TENANT_ID,
            UUID.randomUUID(),
            "REPO-002",
            "Status Item",
            "Testing status query",
            new BigDecimal("9.99"),
            null,
            15,
            null);
    itemRepo.save(CatalogItemEntity.from(item));

    List<CatalogItem> items = itemRepo.findAll().stream().map(CatalogItemEntity::toDomain).toList();
    assertThat(items).isNotEmpty();
    assertThat(items.get(0).getStatus()).isEqualTo(CatalogItemStatus.ACTIVE);
  }
}
