package com.emme.studio.documents.api.command;

import java.util.UUID;

/** Records a document processing failure. */
public record FailDocumentCommand(UUID tenantId, UUID documentId, String error) {}
