package com.emme.clients.application.port.out;

import com.emme.clients.domain.model.Customer;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Persistence capability required by Customer use cases. */
public interface CustomerRepository {

  Customer save(Customer customer);

  Optional<Customer> findById(UUID id);

  List<Customer> findAll();

  List<Customer> searchByName(String name);
}
