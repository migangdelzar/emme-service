package com.emme.documents.api.command;

import java.util.UUID;

/** Requests processing of an uploaded document. */
public record ProcessDocumentCommand(UUID tenantId, UUID documentId) {}
