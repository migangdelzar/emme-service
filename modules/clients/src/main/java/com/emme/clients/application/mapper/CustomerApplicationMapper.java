package com.emme.clients.application.mapper;

import com.emme.clients.api.result.CustomerDetails;
import com.emme.clients.api.type.CustomerStatus;
import com.emme.clients.domain.model.Customer;

/** Maps Studio customer domain objects to stable public use-case results. */
public final class CustomerApplicationMapper {

  private CustomerApplicationMapper() {}

  public static CustomerDetails toDetails(Customer customer) {
    return new CustomerDetails(
        customer.getId(),
        customer.getName(),
        customer.getPhone(),
        customer.getEmail(),
        CustomerStatus.valueOf(customer.getStatus().name()));
  }
}
