package com.emme.catalog.application.service;

import com.emme.assistant.ai.application.ModelProvider;
import com.emme.catalog.api.command.AddCatalogItemImageCommand;
import com.emme.catalog.api.command.CreateCatalogItemCommand;
import com.emme.catalog.api.command.DeleteCatalogItemCommand;
import com.emme.catalog.api.exception.CatalogItemNotFoundException;
import com.emme.catalog.api.query.ListCatalogItemsQuery;
import com.emme.catalog.api.result.CatalogItemImageInfo;
import com.emme.catalog.api.result.CatalogItemInfo;
import com.emme.catalog.api.usecase.AddCatalogItemImageUseCase;
import com.emme.catalog.api.usecase.CreateCatalogItemUseCase;
import com.emme.catalog.api.usecase.DeleteCatalogItemUseCase;
import com.emme.catalog.api.usecase.ListCatalogItemsUseCase;
import com.emme.catalog.application.mapper.CatalogApplicationMapper;
import com.emme.catalog.application.port.out.CatalogItemImageRepository;
import com.emme.catalog.application.port.out.CatalogItemRepository;
import com.emme.catalog.application.port.out.ImageStorage;
import com.emme.catalog.domain.model.CatalogItem;
import com.emme.catalog.domain.model.CatalogItemImage;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CatalogItemService
    implements CreateCatalogItemUseCase,
        DeleteCatalogItemUseCase,
        ListCatalogItemsUseCase,
        AddCatalogItemImageUseCase {

  private final CatalogItemRepository itemRepository;
  private final CatalogItemImageRepository imageRepository;
  private final ImageStorage imageStorage;
  private final ModelProvider modelProvider;

  public CatalogItemService(
      CatalogItemRepository itemRepository,
      CatalogItemImageRepository imageRepository,
      ImageStorage imageStorage,
      ModelProvider modelProvider) {
    this.itemRepository = itemRepository;
    this.imageRepository = imageRepository;
    this.imageStorage = imageStorage;
    this.modelProvider = modelProvider;
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

  @Override
  public void delete(DeleteCatalogItemCommand command) {
    CatalogItem item = findOwned(command.tenantId(), command.itemId());
    imageRepository.deleteAll(imageRepository.findByCatalogItemId(item.getId()));
    itemRepository.delete(item);
  }

  @Override
  @Transactional(readOnly = true)
  public List<CatalogItemInfo> list(ListCatalogItemsQuery query) {
    return itemRepository.findByTenantId(query.tenantId()).stream()
        .map(CatalogApplicationMapper::toInfo)
        .toList();
  }

  @Override
  public CatalogItemImageInfo addImage(AddCatalogItemImageCommand command) {
    CatalogItem item = findOwned(command.tenantId(), command.itemId());
    byte[] bytes = Base64.getDecoder().decode(command.imageBase64());
    String storageKey = imageStorage.store(command.tenantId(), bytes);
    String caption = modelProvider.caption(command.imageBase64());
    CatalogItemImage image =
        new CatalogItemImage(
            command.tenantId(), item.getId(), storageKey, caption.isBlank() ? null : caption);
    return CatalogApplicationMapper.toInfo(imageRepository.save(image));
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
