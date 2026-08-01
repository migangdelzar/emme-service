package com.emme.studio.api.usecase;

import com.emme.studio.domain.model.Customer;
import java.util.Optional;
import java.util.UUID;

/** Retrieves a customer by identifier. */
public interface GetCustomerUseCase {

  Optional<Customer> get(UUID id);
}
