package com.emme.studio.api.result;

import java.util.UUID;

/** Stable public artist-capability representation. */
public record ArtistCapabilityDetails(
    UUID id,
    UUID artistId,
    String artistName,
    UUID serviceId,
    String serviceName,
    boolean active) {}
