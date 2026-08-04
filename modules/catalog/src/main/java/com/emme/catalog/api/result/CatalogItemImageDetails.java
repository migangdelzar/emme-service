package com.emme.catalog.api.result;

import java.util.UUID;

public record CatalogItemImageDetails(
    UUID id, UUID catalogItemId, String storageKey, String caption) {}
