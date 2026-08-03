package com.emme.studio.application.port.out;

import com.emme.studio.domain.model.Customer;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Persistence capability required by Customer use cases. */
public interface CustomerRepository {

  Customer save(Customer customer);

  Optional<Customer> findById(UUID id);

  List<Customer> findByTenantId(UUID tenantId);

  List<Customer> searchByName(UUID tenantId, String name);
}
