package com.emme.services.adapter.in.web.response;

import com.emme.services.api.result.ArtistDetails;
import com.emme.services.domain.model.ArtistStatus;
import java.util.UUID;

/** HTTP representation of a Studio artist. */
public record ArtistResponse(UUID id, String name, ArtistStatus status) {

  public static ArtistResponse from(ArtistDetails artist) {
    return new ArtistResponse(artist.id(), artist.name(), artist.status());
  }
}
