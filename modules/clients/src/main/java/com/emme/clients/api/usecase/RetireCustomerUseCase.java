package com.emme.clients.api.usecase;

import com.emme.clients.api.result.CustomerDetails;
import java.util.UUID;

/** Retires a customer from future operational use. */
public interface RetireCustomerUseCase {

  CustomerDetails retire(UUID id);
}
