package com.emme.services.api.result;

import java.util.UUID;

/** Stable public artist representation returned by Studio use cases. */
public record ArtistDetails(UUID id, String name, String status) {}
