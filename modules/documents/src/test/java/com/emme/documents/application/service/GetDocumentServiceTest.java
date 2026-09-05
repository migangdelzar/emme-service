package com.emme.documents.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.documents.api.query.GetDocumentQuery;
import com.emme.documents.application.port.out.DocumentRepository;
import com.emme.documents.domain.model.Document;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GetDocumentServiceTest {

  @Test
  void loadsADocumentByIdFromTheTenantScopedConnection() {
    UUID owner = UUID.randomUUID();
    Document document = new Document(owner, "policy.pdf", "PDF");
    GetDocumentService service = new GetDocumentService(new Repository(document));

    assertThat(service.get(new GetDocumentQuery(owner, document.id())).id())
        .isEqualTo(document.id());
  }

  private static final class Repository implements DocumentRepository {
    private final Document document;

    private Repository(Document document) {
      this.document = document;
    }

    @Override
    public Optional<Document> findById(UUID documentId) {
      return document.id().equals(documentId) ? Optional.of(document) : Optional.empty();
    }

    @Override
    public List<Document> findAll() {
      return List.of();
    }

    @Override
    public Document save(Document document) {
      return document;
    }

    @Override
    public List<com.emme.documents.domain.model.DocumentChunk> findChunks(UUID documentId) {
      return List.of();
    }

    @Override
    public List<com.emme.documents.domain.model.DocumentChunk> findChunksByIds(
        List<UUID> chunkIds) {
      return List.of();
    }

    @Override
    public void replaceChunks(
        UUID documentId, List<com.emme.documents.domain.model.DocumentChunk> chunks) {}
  }
}
