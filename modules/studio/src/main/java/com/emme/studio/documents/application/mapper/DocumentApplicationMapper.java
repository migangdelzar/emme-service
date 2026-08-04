package com.emme.studio.documents.application.mapper;

import com.emme.studio.documents.api.result.DocumentChunkDetails;
import com.emme.studio.documents.api.result.DocumentDetails;
import com.emme.studio.documents.domain.model.Document;
import com.emme.studio.documents.domain.model.DocumentChunk;

/** Translates framework-free Documents models into public use-case results. */
public final class DocumentApplicationMapper {

  private DocumentApplicationMapper() {}

  public static DocumentDetails toResult(Document document) {
    return new DocumentDetails(
        document.id(),
        document.tenantId(),
        document.name(),
        document.sourceType(),
        document.status().name(),
        document.version());
  }

  public static DocumentChunkDetails toResult(DocumentChunk chunk) {
    return new DocumentChunkDetails(
        chunk.id(),
        chunk.documentId(),
        chunk.chunkIndex(),
        chunk.content(),
        chunk.contentFingerprint());
  }
}
