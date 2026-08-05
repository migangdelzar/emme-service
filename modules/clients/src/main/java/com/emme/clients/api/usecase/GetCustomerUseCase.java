package com.emme.clients.api.usecase;

import com.emme.clients.api.result.CustomerDetails;
import java.util.Optional;
import java.util.UUID;

/** Retrieves a customer by identifier. */
public interface GetCustomerUseCase {

  Optional<CustomerDetails> get(UUID id);
}
