package com.emme.studio.api.usecase;

import com.emme.studio.api.result.ServiceDetails;
import java.util.UUID;

/** Retires a service-catalog entry. */
public interface RetireServiceCatalogEntryUseCase {

  ServiceDetails retire(UUID id);
}
