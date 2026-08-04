package com.emme.catalog.application.service;

import com.emme.catalog.api.query.ListCatalogItemsQuery;
import com.emme.catalog.api.result.CatalogItemDetails;
import com.emme.catalog.api.usecase.ListCatalogItemsUseCase;
import com.emme.catalog.application.mapper.CatalogApplicationMapper;
import com.emme.catalog.application.port.out.CatalogItemRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Executes the ListCatalogItems use case. */
@Service
@Transactional(readOnly = true)
public class ListCatalogItemsService implements ListCatalogItemsUseCase {

  private final CatalogItemRepository itemRepository;

  public ListCatalogItemsService(CatalogItemRepository itemRepository) {
    this.itemRepository = itemRepository;
  }

  @Override
  public List<CatalogItemDetails> list(ListCatalogItemsQuery query) {
    return itemRepository.findByTenantId(query.tenantId()).stream()
        .map(CatalogApplicationMapper::toResult)
        .toList();
  }
}
