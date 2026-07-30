package com.emme.catalog.adapter.in.web.response;

import java.util.UUID;

public record CatalogItemImageResponse(UUID id, String storageKey, String caption) {}
