package com.emme.studio.adapter.in.web.response;

import com.emme.studio.api.result.ArtistCapabilityDetails;
import java.util.UUID;

/** HTTP representation of an artist service capability. */
public record ArtistCapabilityResponse(
    UUID id, UUID artistId, String artistName, UUID serviceId, String serviceName, boolean active) {

  public static ArtistCapabilityResponse from(ArtistCapabilityDetails capability) {
    return new ArtistCapabilityResponse(
        capability.id(),
        capability.artistId(),
        capability.artistName(),
        capability.serviceId(),
        capability.serviceName(),
        capability.active());
  }
}
