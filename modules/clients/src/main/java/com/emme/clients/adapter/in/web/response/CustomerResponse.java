package com.emme.clients.adapter.in.web.response;

import com.emme.clients.api.result.CustomerDetails;
import com.emme.clients.domain.model.CustomerStatus;
import java.util.UUID;

/** HTTP representation of a Studio customer. */
public record CustomerResponse(
    UUID id, String name, String phone, String email, CustomerStatus status) {

  public static CustomerResponse from(CustomerDetails customer) {
    return new CustomerResponse(
        customer.id(), customer.name(), customer.phone(), customer.email(), customer.status());
  }
}
