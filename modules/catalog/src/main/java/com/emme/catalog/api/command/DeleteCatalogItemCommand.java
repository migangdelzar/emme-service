package com.emme.catalog.api.command;

import java.util.UUID;

public record DeleteCatalogItemCommand(UUID tenantId, UUID itemId) {}
