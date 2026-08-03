package com.emme.studio.api.usecase;

import com.emme.studio.domain.model.Service;
import java.util.List;
import java.util.UUID;

/** Lists active service-catalog entries for a tenant. */
public interface ListActiveServiceCatalogEntriesUseCase {

  List<Service> listActive(UUID tenantId);
}
