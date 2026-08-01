package com.emme.studio.documents.api.query;

import java.util.UUID;

/** Requests the ordered chunks of a document. */
public record GetDocumentChunksQuery(UUID documentId) {}
