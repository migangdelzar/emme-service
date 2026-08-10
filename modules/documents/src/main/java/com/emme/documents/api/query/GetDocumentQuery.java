package com.emme.documents.api.query;

import java.util.UUID;

/** Requests one document by identifier. */
public record GetDocumentQuery(UUID tenantId, UUID documentId) {}
