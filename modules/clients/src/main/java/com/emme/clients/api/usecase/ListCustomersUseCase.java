package com.emme.clients.api.usecase;

import com.emme.clients.api.result.CustomerSummary;
import java.util.List;
import java.util.UUID;

/** Lists customers exposed by Studio to other modules. */
public interface ListCustomersUseCase {

  List<CustomerSummary> listCustomers(UUID tenantId);
}
