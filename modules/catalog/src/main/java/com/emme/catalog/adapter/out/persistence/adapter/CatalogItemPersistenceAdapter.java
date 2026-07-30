package com.emme.catalog.adapter.out.persistence.adapter;

import com.emme.catalog.adapter.out.persistence.repository.SpringDataCatalogItemRepository;
import com.emme.catalog.application.port.out.CatalogItemRepository;
import com.emme.catalog.domain.model.CatalogItem;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class CatalogItemPersistenceAdapter implements CatalogItemRepository {

  private final SpringDataCatalogItemRepository repository;

  CatalogItemPersistenceAdapter(SpringDataCatalogItemRepository repository) {
    this.repository = repository;
  }

  @Override
  public Optional<CatalogItem> findById(UUID id) {
    return repository.findById(id);
  }

  @Override
  public List<CatalogItem> findByTenantId(UUID tenantId) {
    return repository.findByTenantId(tenantId);
  }

  @Override
  public CatalogItem save(CatalogItem item) {
    return repository.save(item);
  }

  @Override
  public void delete(CatalogItem item) {
    repository.delete(item);
  }
}
