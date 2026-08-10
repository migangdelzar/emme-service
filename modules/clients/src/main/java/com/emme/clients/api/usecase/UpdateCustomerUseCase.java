package com.emme.clients.api.usecase;

import com.emme.clients.api.result.CustomerDetails;
import java.util.UUID;

/** Updates a customer profile. */
public interface UpdateCustomerUseCase {

  CustomerDetails update(UUID id, String name, String phone, String email);
}
