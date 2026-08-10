package com.emme.services.api.usecase;

import com.emme.services.api.result.ServiceDetails;
import java.math.BigDecimal;
import java.util.UUID;

/** Creates a service-catalog entry. */
public interface CreateServiceCatalogEntryUseCase {

  ServiceDetails create(
      UUID tenantId,
      String code,
      String name,
      String category,
      String description,
      int durationMinutes,
      BigDecimal basePrice);
}
