package com.emme.documents.api.command;

import java.util.UUID;

/** Requests creation of a document owned by a tenant. */
public record UploadDocumentCommand(UUID tenantId, String name, String sourceType) {}
