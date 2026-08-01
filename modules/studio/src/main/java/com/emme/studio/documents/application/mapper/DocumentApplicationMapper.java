package com.emme.studio.documents.application.mapper;

import com.emme.studio.documents.api.result.DocumentChunkInfo;
import com.emme.studio.documents.api.result.DocumentInfo;
import com.emme.studio.documents.domain.model.Document;
import com.emme.studio.documents.domain.model.DocumentChunk;

/** Translates framework-free Documents models into public use-case results. */
public final class DocumentApplicationMapper {

  private DocumentApplicationMapper() {}

  public static DocumentInfo toInfo(Document document) {
    return new DocumentInfo(
        document.id(),
        document.tenantId(),
        document.name(),
        document.sourceType(),
        document.status().name(),
        document.version());
  }

  public static DocumentChunkInfo toInfo(DocumentChunk chunk) {
    return new DocumentChunkInfo(
        chunk.id(),
        chunk.documentId(),
        chunk.chunkIndex(),
        chunk.content(),
        chunk.contentFingerprint());
  }
}
