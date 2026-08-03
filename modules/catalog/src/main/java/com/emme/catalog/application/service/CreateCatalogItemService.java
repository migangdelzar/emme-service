package com.emme.catalog.application.service;

import com.emme.catalog.api.command.CreateCatalogItemCommand;
import com.emme.catalog.api.result.CatalogItemInfo;
import com.emme.catalog.api.usecase.CreateCatalogItemUseCase;
import com.emme.catalog.application.mapper.CatalogApplicationMapper;
import com.emme.catalog.application.port.out.CatalogItemRepository;
import com.emme.catalog.domain.model.CatalogItem;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Executes the CreateCatalogItem use case. */
@Service
@Transactional
public class CreateCatalogItemService implements CreateCatalogItemUseCase {

  private final CatalogItemRepository itemRepository;

  public CreateCatalogItemService(CatalogItemRepository itemRepository) {
    this.itemRepository = itemRepository;
  }

  @Override
  public CatalogItemInfo create(CreateCatalogItemCommand command) {
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
    return CatalogApplicationMapper.toInfo(itemRepository.save(item));
  }
}
