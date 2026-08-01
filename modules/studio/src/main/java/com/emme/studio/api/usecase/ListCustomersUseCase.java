package com.emme.studio.api.usecase;

import com.emme.studio.api.result.CustomerInfo;
import java.util.List;
import java.util.UUID;

/** Lists customers exposed by Studio to other modules. */
public interface ListCustomersUseCase {

  List<CustomerInfo> listCustomers(UUID tenantId);
}
