package com.emme.studio.application.mapper;

import com.emme.studio.api.result.CustomerDetails;
import com.emme.studio.domain.model.Customer;

/** Maps Studio customer domain objects to stable public use-case results. */
public final class CustomerApplicationMapper {

  private CustomerApplicationMapper() {}

  public static CustomerDetails toDetails(Customer customer) {
    return new CustomerDetails(
        customer.getId(),
        customer.getName(),
        customer.getPhone(),
        customer.getEmail(),
        customer.getStatus().name());
  }
}
