package com.emme.studio.documents.api.usecase;

import com.emme.studio.documents.api.query.GetDocumentChunksQuery;
import com.emme.studio.documents.api.result.DocumentChunkInfo;
import java.util.List;

public interface GetDocumentChunksUseCase {
  List<DocumentChunkInfo> getChunks(GetDocumentChunksQuery query);
}
