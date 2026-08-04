package com.emme.studio.documents.api.usecase;

import com.emme.studio.documents.api.query.SearchDocumentChunksQuery;
import com.emme.studio.documents.api.result.DocumentChunkDetails;
import java.util.List;

/** Searches document chunks through the public Documents capability. */
public interface SearchDocumentChunksUseCase {

  List<DocumentChunkDetails> search(SearchDocumentChunksQuery query);
}
