package com.emme.studio.documents.api.usecase;

import com.emme.studio.documents.api.query.GetDocumentChunksQuery;
import com.emme.studio.documents.api.result.DocumentChunkDetails;
import java.util.List;

public interface GetDocumentChunksUseCase {
  List<DocumentChunkDetails> getChunks(GetDocumentChunksQuery query);
}
