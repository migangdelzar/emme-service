package com.emme.studio.documents.api.query;

import java.util.UUID;

/** Requests all documents owned by a tenant. */
public record ListDocumentsQuery(UUID tenantId) {}
