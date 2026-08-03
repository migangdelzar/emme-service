package com.emme.studio.api.usecase;

import com.emme.studio.domain.model.Customer;
import java.util.UUID;

/** Creates a customer in a tenant. */
public interface CreateCustomerUseCase {

  Customer create(UUID tenantId, String name, String phone, String email);
}
