package com.emme.studio.api.usecase;

import com.emme.studio.domain.model.Customer;
import java.util.List;
import java.util.UUID;

/** Searches tenant customers by name. */
public interface SearchCustomersUseCase {

  List<Customer> search(UUID tenantId, String query);
}
