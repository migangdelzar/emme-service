package com.emme.studio.api.usecase;

import com.emme.studio.api.result.ServiceDetails;
import java.util.Optional;
import java.util.UUID;

/** Retrieves a service-catalog entry. */
public interface GetServiceCatalogEntryUseCase {

  Optional<ServiceDetails> get(UUID id);
}
