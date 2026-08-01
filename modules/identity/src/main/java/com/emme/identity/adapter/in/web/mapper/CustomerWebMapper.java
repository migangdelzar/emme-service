package com.emme.identity.adapter.in.web.mapper;

import com.emme.identity.adapter.in.web.response.CustomerLoginResponse;
import com.emme.identity.adapter.in.web.response.CustomerProfileResponse;
import com.emme.identity.api.result.CustomerDetails;
import com.emme.identity.api.result.CustomerLoginResult;

/** Maps customer application results into stable HTTP response models. */
public final class CustomerWebMapper {

  private CustomerWebMapper() {}

  public static CustomerLoginResponse toLoginResponse(CustomerLoginResult result) {
    return new CustomerLoginResponse(result.needsPhone(), toProfileResponse(result.customer()));
  }

  public static CustomerProfileResponse toProfileResponse(CustomerDetails customer) {
    return new CustomerProfileResponse(
        customer.id().toString(),
        valueOrEmpty(customer.email()),
        valueOrEmpty(customer.name()),
        valueOrEmpty(customer.phone()),
        valueOrDefault(customer.provider(), "UNKNOWN"));
  }

  private static String valueOrEmpty(String value) {
    return value == null ? "" : value;
  }

  private static String valueOrDefault(String value, String fallback) {
    return value == null ? fallback : value;
  }
}
