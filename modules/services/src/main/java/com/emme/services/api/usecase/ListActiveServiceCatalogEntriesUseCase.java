package com.emme.services.api.usecase;

import com.emme.services.api.result.ServiceDetails;
import java.util.List;
import java.util.UUID;

/** Lists active service-catalog entries for a tenant. */
public interface ListActiveServiceCatalogEntriesUseCase {

  List<ServiceDetails> listActive(UUID tenantId);
}
