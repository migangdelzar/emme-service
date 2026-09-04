package com.emme.catalog.application.service;

import com.emme.ai.contracts.semantic.SemanticCacheDependencyChanged;
import com.emme.ai.contracts.semantic.SemanticCacheDependencyPublisher;
import com.emme.catalog.api.command.DeleteCatalogItemCommand;
import com.emme.catalog.api.exception.CatalogItemNotFoundException;
import com.emme.catalog.api.usecase.DeleteCatalogItemUseCase;
import com.emme.catalog.application.port.out.CatalogItemImageRepository;
import com.emme.catalog.application.port.out.CatalogItemRepository;
import com.emme.catalog.domain.model.CatalogItem;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Executes the DeleteCatalogItem use case. */
@Service
@Transactional
public class DeleteCatalogItemService implements DeleteCatalogItemUseCase {

  private final CatalogItemRepository itemRepository;
  private final CatalogItemImageRepository imageRepository;
  private final Optional<SemanticCacheDependencyPublisher> cacheDependencies;

  public DeleteCatalogItemService(
      CatalogItemRepository itemRepository, CatalogItemImageRepository imageRepository) {
    this(itemRepository, imageRepository, Optional.empty());
  }

  public DeleteCatalogItemService(
      CatalogItemRepository itemRepository,
      CatalogItemImageRepository imageRepository,
      SemanticCacheDependencyPublisher cacheDependencies) {
    this(itemRepository, imageRepository, Optional.of(cacheDependencies));
  }

  @Autowired
  public DeleteCatalogItemService(
      CatalogItemRepository itemRepository,
      CatalogItemImageRepository imageRepository,
      Optional<SemanticCacheDependencyPublisher> cacheDependencies) {
    this.itemRepository = itemRepository;
    this.imageRepository = imageRepository;
    this.cacheDependencies = cacheDependencies;
  }

  @Override
  public void delete(DeleteCatalogItemCommand command) {
    CatalogItem item = findOwned(command.tenantId(), command.itemId());
    imageRepository.deleteAll(imageRepository.findByCatalogItemId(item.getId()));
    itemRepository.delete(item);
    cacheDependencies.ifPresent(
        publisher ->
            publisher.publish(
                new SemanticCacheDependencyChanged(
                    UUID.randomUUID(),
                    item.getTenantId(),
                    null,
                    SemanticCacheDependencyChanged.Dependency.PRICE,
                    item.getId().toString(),
                    Instant.now())));
  }

  private CatalogItem findOwned(UUID tenantId, UUID itemId) {
    CatalogItem item =
        itemRepository.findById(itemId).orElseThrow(() -> new CatalogItemNotFoundException(itemId));
    if (!item.getTenantId().equals(tenantId)) {
      throw new CatalogItemNotFoundException(itemId);
    }
    return item;
  }
}
