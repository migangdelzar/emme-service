package com.emme.services.api.usecase;

import com.emme.services.api.result.ServiceDetails;
import java.util.Optional;
import java.util.UUID;

/** Retrieves a service-catalog entry. */
public interface GetServiceCatalogEntryUseCase {

  Optional<ServiceDetails> get(UUID id);
}
