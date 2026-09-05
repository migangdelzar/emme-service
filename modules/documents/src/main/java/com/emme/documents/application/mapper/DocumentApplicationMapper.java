package com.emme.documents.application.mapper;

import com.emme.documents.api.result.DocumentChunkDetails;
import com.emme.documents.api.result.DocumentDetails;
import com.emme.documents.domain.model.Document;
import com.emme.documents.domain.model.DocumentChunk;

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
    return toResult(chunk, 0.0);
  }

  public static DocumentChunkDetails toResult(DocumentChunk chunk, double score) {
    return new DocumentChunkDetails(
        chunk.id(),
        chunk.documentId(),
        chunk.chunkIndex(),
        chunk.content(),
        chunk.contentFingerprint(),
        score);
  }
}
