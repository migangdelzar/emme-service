package com.emme.services;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.services.adapter.in.web.response.ArtistResponse;
import com.emme.services.adapter.in.web.response.ServiceResponse;
import com.emme.services.api.result.ArtistDetails;
import com.emme.services.api.result.ServiceDetails;
import com.emme.services.api.type.ArtistStatus;
import com.emme.services.api.type.ServiceStatus;
import org.junit.jupiter.api.Test;

class ServiceStatusConventionTest {

  @Test
  void serviceAndArtistStatusesUseApiOwnedEnumsAcrossPublicBoundaries() {
    assertThat(ServiceDetails.class.getRecordComponents()[7].getType())
        .isEqualTo(ServiceStatus.class);
    assertThat(ServiceResponse.class.getRecordComponents()[7].getType())
        .isEqualTo(ServiceStatus.class);
    assertThat(ArtistDetails.class.getRecordComponents()[2].getType())
        .isEqualTo(ArtistStatus.class);
    assertThat(ArtistResponse.class.getRecordComponents()[2].getType())
        .isEqualTo(ArtistStatus.class);
  }
}
