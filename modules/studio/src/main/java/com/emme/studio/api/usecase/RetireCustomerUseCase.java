package com.emme.studio.api.usecase;

import com.emme.studio.domain.model.Customer;
import java.util.UUID;

/** Retires a customer from future operational use. */
public interface RetireCustomerUseCase {

  Customer retire(UUID id);
}
