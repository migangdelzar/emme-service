package com.emme.catalog.api.query;

import java.util.UUID;

public record MatchCatalogItemsQuery(UUID tenantId, String query, String imageBase64) {}
