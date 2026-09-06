package com.emme.services.application.mapper;

import com.emme.services.api.result.ArtistCapabilityDetails;
import com.emme.services.api.result.ArtistDetails;
import com.emme.services.api.type.ArtistStatus;
import com.emme.services.domain.model.Artist;
import com.emme.services.domain.model.ArtistCapability;

/** Maps artist domain objects to stable public use-case results. */
public final class ArtistApplicationMapper {

  private ArtistApplicationMapper() {}

  public static ArtistDetails toDetails(Artist artist) {
    return new ArtistDetails(
        artist.getId(), artist.getName(), ArtistStatus.valueOf(artist.getStatus().name()));
  }

  public static ArtistCapabilityDetails toDetails(ArtistCapability capability) {
    return new ArtistCapabilityDetails(
        capability.getId(),
        capability.getArtist().getId(),
        capability.getArtist().getName(),
        capability.getService().getId(),
        capability.getService().getName(),
        capability.isActive());
  }
}
