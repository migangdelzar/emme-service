package com.emme.documents.api.usecase;

import com.emme.documents.api.query.GetDocumentChunksQuery;
import com.emme.documents.api.result.DocumentChunkDetails;
import java.util.List;

public interface GetDocumentChunksUseCase {
  List<DocumentChunkDetails> getChunks(GetDocumentChunksQuery query);
}
