package com.emme.salon.adapter.in.web.response;

import com.emme.salon.api.result.BusinessProfileDetails;
import java.util.UUID;

/** HTTP representation of the tenant business profile. */
public record BusinessProfileResponse(UUID id, String displayName, String timeZone, String locale) {

  public static BusinessProfileResponse from(BusinessProfileDetails profile) {
    return new BusinessProfileResponse(
        profile.id(), profile.displayName(), profile.timeZone(), profile.locale());
  }
}
