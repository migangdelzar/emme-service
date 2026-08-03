package com.emme.studio.api.usecase;

import com.emme.studio.api.result.CustomerDetails;
import java.util.UUID;

/** Retires a customer from future operational use. */
public interface RetireCustomerUseCase {

  CustomerDetails retire(UUID id);
}
