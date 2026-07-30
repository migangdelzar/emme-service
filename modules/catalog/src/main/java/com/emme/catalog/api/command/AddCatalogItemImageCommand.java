package com.emme.catalog.api.command;

import java.util.UUID;

public record AddCatalogItemImageCommand(UUID tenantId, UUID itemId, String imageBase64) {}
