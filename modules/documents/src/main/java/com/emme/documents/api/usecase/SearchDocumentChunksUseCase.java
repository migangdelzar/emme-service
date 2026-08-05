package com.emme.documents.api.usecase;

import com.emme.documents.api.query.SearchDocumentChunksQuery;
import com.emme.documents.api.result.DocumentChunkDetails;
import java.util.List;

/** Searches document chunks through the public Documents capability. */
public interface SearchDocumentChunksUseCase {

  List<DocumentChunkDetails> search(SearchDocumentChunksQuery query);
}
