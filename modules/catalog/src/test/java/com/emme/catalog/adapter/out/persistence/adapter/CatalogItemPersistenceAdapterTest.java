package com.emme.catalog.adapter.out.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emme.catalog.adapter.out.persistence.entity.CatalogItemEntity;
import com.emme.catalog.adapter.out.persistence.repository.SpringDataCatalogItemRepository;
import com.emme.catalog.domain.model.CatalogItem;
import com.emme.catalog.domain.model.CatalogItemStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CatalogItemPersistenceAdapterTest {

  @Test
  void listsItemsFromTheCurrentTenantSchema() {
    SpringDataCatalogItemRepository repository = mock();
    CatalogItemPersistenceAdapter adapter = new CatalogItemPersistenceAdapter(repository);
    when(repository.findAll()).thenReturn(List.of());

    assertThat(adapter.findAll()).isEmpty();

    verify(repository).findAll();
  }

  @Test
  void loadsMatchedItemsByIdFromTheCurrentTenantSchema() {
    SpringDataCatalogItemRepository repository = mock();
    CatalogItemPersistenceAdapter adapter = new CatalogItemPersistenceAdapter(repository);
    List<UUID> ids = List.of(UUID.randomUUID());
    when(repository.findAllById(ids)).thenReturn(List.of());

    assertThat(adapter.findAllById(ids)).isEmpty();

    verify(repository).findAllById(ids);
  }

  @Test
  void updatesTheManagedEntityWhenSavingAnExistingCatalogItem() {
    SpringDataCatalogItemRepository repository = mock();
    CatalogItemPersistenceAdapter adapter = new CatalogItemPersistenceAdapter(repository);
    UUID itemId = UUID.randomUUID();
    CatalogItem item =
        new CatalogItem(
            itemId,
            UUID.randomUUID(),
            UUID.randomUUID(),
            "code",
            "name",
            null,
            BigDecimal.TEN,
            null,
            30,
            null,
            CatalogItemStatus.ACTIVE);
    item.changeStatus(CatalogItemStatus.RETIRED);
    CatalogItemEntity managedEntity = CatalogItemEntity.from(item);
    when(repository.findById(itemId)).thenReturn(Optional.of(managedEntity));
    when(repository.save(managedEntity)).thenReturn(managedEntity);

    CatalogItem saved = adapter.save(item);

    verify(repository).findById(itemId);
    verify(repository).save(managedEntity);
    assertThat(saved.getStatus()).isEqualTo(CatalogItemStatus.RETIRED);
  }
}
