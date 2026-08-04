package com.emme.studio.documents.adapter.in.web.mapper;

import com.emme.studio.documents.adapter.in.web.response.DocumentChunkResponse;
import com.emme.studio.documents.adapter.in.web.response.DocumentResponse;
import com.emme.studio.documents.api.result.DocumentChunkDetails;
import com.emme.studio.documents.api.result.DocumentDetails;
import org.springframework.stereotype.Component;

/** Maps document application models to HTTP response models. */
@Component
public class DocumentWebMapper {

  public DocumentResponse toResponse(DocumentDetails document) {
    return new DocumentResponse(
        document.id(),
        document.tenantId(),
        document.name(),
        document.sourceType(),
        document.status(),
        document.version());
  }

  public DocumentChunkResponse toResponse(DocumentChunkDetails chunk) {
    return new DocumentChunkResponse(
        chunk.id(),
        chunk.documentId(),
        chunk.chunkIndex(),
        chunk.content(),
        chunk.contentFingerprint());
  }
}
