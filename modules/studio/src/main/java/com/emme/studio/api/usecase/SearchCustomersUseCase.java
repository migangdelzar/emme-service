package com.emme.studio.api.usecase;

import com.emme.studio.api.result.CustomerDetails;
import java.util.List;
import java.util.UUID;

/** Searches tenant customers by name. */
public interface SearchCustomersUseCase {

  List<CustomerDetails> search(UUID tenantId, String query);
}
