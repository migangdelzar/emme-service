package com.emme.services.api.usecase;

import com.emme.services.api.result.ServiceDetails;
import java.util.UUID;

/** Retires a service-catalog entry. */
public interface RetireServiceCatalogEntryUseCase {

  ServiceDetails retire(UUID id);
}
