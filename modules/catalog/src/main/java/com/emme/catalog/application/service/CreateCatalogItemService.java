package com.emme.catalog.application.service;

import com.emme.ai.contracts.semantic.SemanticCacheDependencyChanged;
import com.emme.ai.contracts.semantic.SemanticCacheDependencyPublisher;
import com.emme.catalog.api.command.CreateCatalogItemCommand;
import com.emme.catalog.api.result.CatalogItemDetails;
import com.emme.catalog.api.usecase.CreateCatalogItemUseCase;
import com.emme.catalog.application.mapper.CatalogApplicationMapper;
import com.emme.catalog.application.port.out.CatalogItemRepository;
import com.emme.catalog.domain.model.CatalogItem;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Executes the CreateCatalogItem use case. */
@Service
@Transactional
public class CreateCatalogItemService implements CreateCatalogItemUseCase {

  private final CatalogItemRepository itemRepository;
  private final Optional<SemanticCacheDependencyPublisher> cacheDependencies;

  public CreateCatalogItemService(CatalogItemRepository itemRepository) {
    this(itemRepository, Optional.empty());
  }

  public CreateCatalogItemService(
      CatalogItemRepository itemRepository, SemanticCacheDependencyPublisher cacheDependencies) {
    this(itemRepository, Optional.of(cacheDependencies));
  }

  @Autowired
  public CreateCatalogItemService(
      CatalogItemRepository itemRepository,
      Optional<SemanticCacheDependencyPublisher> cacheDependencies) {
    this.itemRepository = itemRepository;
    this.cacheDependencies = cacheDependencies;
  }

  @Override
  public CatalogItemDetails create(CreateCatalogItemCommand command) {
    CatalogItem item =
        new CatalogItem(
            command.tenantId(),
            command.serviceId(),
            command.code(),
            command.name(),
            command.description(),
            command.price(),
            command.priceNotes(),
            command.durationMinutes(),
            command.materials());
    CatalogItemDetails details = CatalogApplicationMapper.toResult(itemRepository.save(item));
    cacheDependencies.ifPresent(
        publisher ->
            publisher.publish(
                new SemanticCacheDependencyChanged(
                    UUID.randomUUID(),
                    command.tenantId(),
                    null,
                    SemanticCacheDependencyChanged.Dependency.PRICE,
                    details.id().toString(),
                    Instant.now())));
    return details;
  }
}
