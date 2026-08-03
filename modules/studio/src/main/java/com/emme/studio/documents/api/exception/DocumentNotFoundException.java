package com.emme.studio.documents.api.exception;

import java.util.UUID;

/** Indicates that a requested document does not exist. */
public final class DocumentNotFoundException extends IllegalArgumentException {

  private static final long serialVersionUID = 1L;

  public DocumentNotFoundException(UUID documentId) {
    super("Document not found: " + documentId);
  }
}
