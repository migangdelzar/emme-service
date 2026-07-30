package com.emme.catalog.adapter.in.web.request;

import jakarta.validation.constraints.NotBlank;

public record MatchCatalogItemsRequest(@NotBlank String query, String imageBase64) {}
