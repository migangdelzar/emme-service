package com.emme.clients.application.port.out;

import com.emme.clients.domain.model.Customer;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Persistence capability required by Customer use cases. */
public interface CustomerRepository {

  Customer save(Customer customer);

  Optional<Customer> findByTenantIdAndId(UUID tenantId, UUID id);

  List<Customer> findByTenantId(UUID tenantId);

  List<Customer> searchByName(UUID tenantId, String name);
}
