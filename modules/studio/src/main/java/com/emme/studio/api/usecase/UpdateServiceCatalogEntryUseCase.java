package com.emme.studio.api.usecase;

import com.emme.studio.api.result.ServiceDetails;
import java.math.BigDecimal;
import java.util.UUID;

/** Updates a service-catalog entry. */
public interface UpdateServiceCatalogEntryUseCase {

  ServiceDetails update(
      UUID id,
      String name,
      String category,
      String description,
      int durationMinutes,
      BigDecimal basePrice);
}
