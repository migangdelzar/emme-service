package com.emme.studio.documents.api.command;

import java.util.List;
import java.util.UUID;

/** Requests replacement of the chunks belonging to a document. */
public record ChunkDocumentCommand(UUID documentId, List<String> chunks) {
  public ChunkDocumentCommand {
    chunks = List.copyOf(chunks);
  }
}
