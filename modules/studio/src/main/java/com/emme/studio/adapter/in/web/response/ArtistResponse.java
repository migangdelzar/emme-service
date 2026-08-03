package com.emme.studio.adapter.in.web.response;

import com.emme.studio.api.result.ArtistDetails;
import java.util.UUID;

/** HTTP representation of a Studio artist. */
public record ArtistResponse(UUID id, String name, String status) {

  public static ArtistResponse from(ArtistDetails artist) {
    return new ArtistResponse(artist.id(), artist.name(), artist.status());
  }
}
