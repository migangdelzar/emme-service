package com.emme.studio.documents.application.service;

import com.emme.studio.documents.api.query.SearchDocumentChunksQuery;
import com.emme.studio.documents.api.result.DocumentChunkInfo;
import com.emme.studio.documents.api.usecase.SearchDocumentChunksUseCase;
import com.emme.studio.documents.application.mapper.DocumentApplicationMapper;
import com.emme.studio.documents.application.port.out.DocumentRepository;
import com.emme.studio.documents.application.port.out.DocumentSearchHit;
import com.emme.studio.documents.application.port.out.DocumentSearchPort;
import com.emme.studio.documents.domain.model.DocumentChunk;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Executes tenant-scoped document retrieval and restores search rank ordering. */
@Service
@Transactional(readOnly = true)
public class SearchDocumentChunksService implements SearchDocumentChunksUseCase {

  private final DocumentSearchPort search;
  private final DocumentRepository repository;

  public SearchDocumentChunksService(DocumentSearchPort search, DocumentRepository repository) {
    this.search = search;
    this.repository = repository;
  }

  @Override
  public List<DocumentChunkInfo> search(SearchDocumentChunksQuery query) {
    List<DocumentSearchHit> hits =
        search.search(query.tenantId(), query.queryVector(), query.queryText(), query.limit());
    List<java.util.UUID> chunkIds =
        hits.stream().map(DocumentSearchHit::chunkId).distinct().toList();
    if (chunkIds.isEmpty()) {
      return List.of();
    }

    Map<java.util.UUID, DocumentChunk> chunksById =
        repository.findChunksByTenantIdAndIds(query.tenantId(), chunkIds).stream()
            .collect(Collectors.toMap(DocumentChunk::id, Function.identity()));
    return chunkIds.stream()
        .map(chunksById::get)
        .filter(java.util.Objects::nonNull)
        .map(DocumentApplicationMapper::toInfo)
        .toList();
  }
}
