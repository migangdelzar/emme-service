package com.emme.studio.adapter.in.web.response;

import com.emme.studio.api.result.CustomerDetails;
import java.util.UUID;

/** HTTP representation of a Studio customer. */
public record CustomerResponse(UUID id, String name, String phone, String email, String status) {

  public static CustomerResponse from(CustomerDetails customer) {
    return new CustomerResponse(
        customer.id(), customer.name(), customer.phone(), customer.email(), customer.status());
  }
}
