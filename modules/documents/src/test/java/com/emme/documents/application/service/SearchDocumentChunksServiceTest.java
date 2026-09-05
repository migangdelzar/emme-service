package com.emme.documents.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emme.documents.api.query.SearchDocumentChunksQuery;
import com.emme.documents.api.result.DocumentChunkDetails;
import com.emme.documents.application.port.out.DocumentRepository;
import com.emme.documents.application.port.out.DocumentSearchHit;
import com.emme.documents.application.port.out.DocumentSearchPort;
import com.emme.documents.domain.model.DocumentChunk;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SearchDocumentChunksServiceTest {

  @Test
  void returnsChunksInSearchRankOrderWithinTheRequestedTenant() {
    UUID tenantId = UUID.randomUUID();
    UUID firstChunkId = UUID.randomUUID();
    UUID secondChunkId = UUID.randomUUID();
    DocumentSearchPort search = mock(DocumentSearchPort.class);
    DocumentRepository repository = mock(DocumentRepository.class);
    SearchDocumentChunksService service = new SearchDocumentChunksService(search, repository);

    when(search.search(tenantId, List.of(0.25f), "pricing", 5))
        .thenReturn(
            List.of(
                new DocumentSearchHit(secondChunkId, 0.9),
                new DocumentSearchHit(firstChunkId, 0.7)));
    UUID documentId = UUID.randomUUID();
    when(repository.findChunksByIds(List.of(secondChunkId, firstChunkId)))
        .thenReturn(
            List.of(
                new DocumentChunk(firstChunkId, tenantId, documentId, 0, "first", "fingerprint-1"),
                new DocumentChunk(
                    secondChunkId, tenantId, documentId, 1, "second", "fingerprint-2")));

    List<DocumentChunkDetails> results =
        service.search(new SearchDocumentChunksQuery(tenantId, List.of(0.25f), "pricing", 5));

    assertThat(results)
        .extracting(DocumentChunkDetails::id)
        .containsExactly(secondChunkId, firstChunkId);
    verify(search).search(tenantId, List.of(0.25f), "pricing", 5);
    verify(repository).findChunksByIds(List.of(secondChunkId, firstChunkId));
  }
}
