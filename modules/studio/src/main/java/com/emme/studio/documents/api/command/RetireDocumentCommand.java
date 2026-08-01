package com.emme.studio.documents.api.command;

import java.util.UUID;

/** Requests retirement of a document. */
public record RetireDocumentCommand(UUID documentId) {}
