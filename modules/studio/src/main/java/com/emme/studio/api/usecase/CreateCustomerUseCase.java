package com.emme.studio.api.usecase;

import com.emme.studio.api.result.CustomerDetails;
import java.util.UUID;

/** Creates a customer in a tenant. */
public interface CreateCustomerUseCase {

  CustomerDetails create(UUID tenantId, String name, String phone, String email);
}
