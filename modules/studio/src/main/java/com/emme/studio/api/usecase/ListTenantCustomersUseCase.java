package com.emme.studio.api.usecase;

import com.emme.studio.api.result.CustomerDetails;
import java.util.List;
import java.util.UUID;

/** Lists customers belonging to a tenant. */
public interface ListTenantCustomersUseCase {

  List<CustomerDetails> list(UUID tenantId);
}
