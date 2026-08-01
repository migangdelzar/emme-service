package com.emme.studio.api.usecase;

import com.emme.studio.domain.model.Customer;
import java.util.List;
import java.util.UUID;

/** Lists customers belonging to a tenant. */
public interface ListTenantCustomersUseCase {

  List<Customer> list(UUID tenantId);
}
