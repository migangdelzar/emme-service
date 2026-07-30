package com.emme.catalog.adapter.out.persistence.adapter;

import com.emme.catalog.adapter.out.persistence.repository.SpringDataCatalogItemImageRepository;
import com.emme.catalog.application.port.out.CatalogItemImageRepository;
import com.emme.catalog.domain.model.CatalogItemImage;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class CatalogItemImagePersistenceAdapter implements CatalogItemImageRepository {

  private final SpringDataCatalogItemImageRepository repository;

  CatalogItemImagePersistenceAdapter(SpringDataCatalogItemImageRepository repository) {
    this.repository = repository;
  }

  @Override
  public List<CatalogItemImage> findByCatalogItemId(UUID catalogItemId) {
    return repository.findByCatalogItemId(catalogItemId);
  }

  @Override
  public List<CatalogItemImage> findAllById(Iterable<UUID> ids) {
    return repository.findAllById(ids);
  }

  @Override
  public CatalogItemImage save(CatalogItemImage image) {
    return repository.save(image);
  }

  @Override
  public void deleteAll(List<CatalogItemImage> images) {
    repository.deleteAll(images);
  }
}
