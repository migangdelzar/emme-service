package com.emme.catalog.application.service;

import com.emme.ai.contracts.image.CaptionImageUseCase;
import com.emme.catalog.api.command.AddCatalogItemImageCommand;
import com.emme.catalog.api.exception.CatalogItemNotFoundException;
import com.emme.catalog.api.result.CatalogItemImageDetails;
import com.emme.catalog.api.usecase.AddCatalogItemImageUseCase;
import com.emme.catalog.application.mapper.CatalogApplicationMapper;
import com.emme.catalog.application.port.out.CatalogItemImageRepository;
import com.emme.catalog.application.port.out.CatalogItemRepository;
import com.emme.catalog.application.port.out.ImageStorage;
import com.emme.catalog.domain.model.CatalogItem;
import com.emme.catalog.domain.model.CatalogItemImage;
import java.util.Base64;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Executes the AddCatalogItemImage use case. */
@Service
@Transactional
public class AddCatalogItemImageService implements AddCatalogItemImageUseCase {

  private final CatalogItemRepository itemRepository;
  private final CatalogItemImageRepository imageRepository;
  private final ImageStorage imageStorage;
  private final CaptionImageUseCase captionImageUseCase;

  public AddCatalogItemImageService(
      CatalogItemRepository itemRepository,
      CatalogItemImageRepository imageRepository,
      ImageStorage imageStorage,
      CaptionImageUseCase captionImageUseCase) {
    this.itemRepository = itemRepository;
    this.imageRepository = imageRepository;
    this.imageStorage = imageStorage;
    this.captionImageUseCase = captionImageUseCase;
  }

  @Override
  public CatalogItemImageDetails addImage(AddCatalogItemImageCommand command) {
    CatalogItem item = findOwned(command.tenantId(), command.itemId());
    byte[] bytes = Base64.getDecoder().decode(command.imageBase64());
    String storageKey = imageStorage.store(command.tenantId(), bytes);
    String caption = captionImageUseCase.caption(command.imageBase64());
    CatalogItemImage image =
        new CatalogItemImage(
            command.tenantId(), item.getId(), storageKey, caption.isBlank() ? null : caption);
    return CatalogApplicationMapper.toResult(imageRepository.save(image));
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
