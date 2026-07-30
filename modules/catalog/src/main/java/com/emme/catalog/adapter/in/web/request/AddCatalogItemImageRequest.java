package com.emme.catalog.adapter.in.web.request;

import jakarta.validation.constraints.NotBlank;

public record AddCatalogItemImageRequest(@NotBlank String imageBase64) {}
