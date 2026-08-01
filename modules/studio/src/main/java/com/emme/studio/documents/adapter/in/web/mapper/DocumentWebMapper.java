package com.emme.studio.documents.adapter.in.web.mapper;

import com.emme.studio.documents.adapter.in.web.response.DocumentChunkResponse;
import com.emme.studio.documents.adapter.in.web.response.DocumentResponse;
import com.emme.studio.documents.domain.model.Document;
import com.emme.studio.documents.domain.model.DocumentChunk;
import org.springframework.stereotype.Component;

/** Maps document application models to HTTP response models. */
@Component
public class DocumentWebMapper {

  public DocumentResponse toResponse(Document document) {
    return new DocumentResponse(
        document.id(),
        document.tenantId(),
        document.name(),
        document.sourceType(),
        document.status().name(),
        document.version());
  }

  public DocumentChunkResponse toResponse(DocumentChunk chunk) {
    return new DocumentChunkResponse(
        chunk.id(),
        chunk.documentId(),
        chunk.chunkIndex(),
        chunk.content(),
        chunk.contentFingerprint());
  }
}
