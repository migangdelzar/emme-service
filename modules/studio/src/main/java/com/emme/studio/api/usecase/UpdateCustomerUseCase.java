package com.emme.studio.api.usecase;

import com.emme.studio.domain.model.Customer;
import java.util.UUID;

/** Updates a customer profile. */
public interface UpdateCustomerUseCase {

  Customer update(UUID id, String name, String phone, String email);
}
