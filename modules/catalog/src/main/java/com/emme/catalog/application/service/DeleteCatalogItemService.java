package com.emme.catalog.application.service;

import com.emme.catalog.api.command.DeleteCatalogItemCommand;
import com.emme.catalog.api.exception.CatalogItemNotFoundException;
import com.emme.catalog.api.usecase.DeleteCatalogItemUseCase;
import com.emme.catalog.application.port.out.CatalogItemImageRepository;
import com.emme.catalog.application.port.out.CatalogItemRepository;
import com.emme.catalog.domain.model.CatalogItem;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Executes the DeleteCatalogItem use case. */
@Service
@Transactional
public class DeleteCatalogItemService implements DeleteCatalogItemUseCase {

  private final CatalogItemRepository itemRepository;
  private final CatalogItemImageRepository imageRepository;

  public DeleteCatalogItemService(
      CatalogItemRepository itemRepository, CatalogItemImageRepository imageRepository) {
    this.itemRepository = itemRepository;
    this.imageRepository = imageRepository;
  }

  @Override
  public void delete(DeleteCatalogItemCommand command) {
    CatalogItem item = findOwned(command.tenantId(), command.itemId());
    imageRepository.deleteAll(imageRepository.findByCatalogItemId(item.getId()));
    itemRepository.delete(item);
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
