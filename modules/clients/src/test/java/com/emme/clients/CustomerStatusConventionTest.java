package com.emme.clients;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.clients.adapter.in.web.response.CustomerResponse;
import com.emme.clients.api.result.CustomerDetails;
import com.emme.clients.domain.model.CustomerStatus;
import org.junit.jupiter.api.Test;

class CustomerStatusConventionTest {

  @Test
  void customerStatusUsesTheDomainEnumAcrossPublicBoundaries() {
    assertThat(CustomerDetails.class.getRecordComponents()[4].getType())
        .isEqualTo(CustomerStatus.class);
    assertThat(CustomerResponse.class.getRecordComponents()[4].getType())
        .isEqualTo(CustomerStatus.class);
  }
}
