package com.emme.services.api.result;

import com.emme.services.domain.model.ArtistStatus;
import java.util.UUID;

/** Stable public artist representation returned by Studio use cases. */
public record ArtistDetails(UUID id, String name, ArtistStatus status) {}
