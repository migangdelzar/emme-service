package com.emme.catalog.application.port.out;

import java.util.UUID;

/** Technology-neutral search hit returned by the Catalog search capability. */
public record CatalogSearchHit(UUID id, double score) {}
