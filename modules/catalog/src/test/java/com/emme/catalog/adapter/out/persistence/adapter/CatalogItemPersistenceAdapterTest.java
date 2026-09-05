package com.emme.catalog.adapter.out.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emme.catalog.adapter.out.persistence.repository.SpringDataCatalogItemRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CatalogItemPersistenceAdapterTest {

  @Test
  void listsItemsFromTheCurrentTenantSchema() {
    SpringDataCatalogItemRepository repository = org.mockito.Mockito.mock();
    CatalogItemPersistenceAdapter adapter = new CatalogItemPersistenceAdapter(repository);
    when(repository.findAll()).thenReturn(List.of());

    assertThat(adapter.findAll()).isEmpty();

    verify(repository).findAll();
  }

  @Test
  void loadsMatchedItemsByIdFromTheCurrentTenantSchema() {
    SpringDataCatalogItemRepository repository = org.mockito.Mockito.mock();
    CatalogItemPersistenceAdapter adapter = new CatalogItemPersistenceAdapter(repository);
    List<UUID> ids = List.of(UUID.randomUUID());
    when(repository.findAllById(ids)).thenReturn(List.of());

    assertThat(adapter.findAllById(ids)).isEmpty();

    verify(repository).findAllById(ids);
  }
}
