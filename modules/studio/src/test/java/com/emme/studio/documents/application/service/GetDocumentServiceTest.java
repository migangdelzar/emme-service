package com.emme.studio.documents.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.emme.studio.documents.api.exception.DocumentNotFoundException;
import com.emme.studio.documents.api.query.GetDocumentQuery;
import com.emme.studio.documents.application.port.out.DocumentRepository;
import com.emme.studio.documents.domain.model.Document;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GetDocumentServiceTest {

  @Test
  void rejectsADocumentFromAnotherTenant() {
    UUID owner = UUID.randomUUID();
    UUID caller = UUID.randomUUID();
    Document document = new Document(owner, "policy.pdf", "PDF");
    GetDocumentService service = new GetDocumentService(new Repository(document));

    assertThatThrownBy(() -> service.get(new GetDocumentQuery(caller, document.id())))
        .isInstanceOf(DocumentNotFoundException.class);
  }

  private static final class Repository implements DocumentRepository {
    private final Document document;

    private Repository(Document document) {
      this.document = document;
    }

    @Override
    public Optional<Document> findByTenantIdAndId(UUID tenantId, UUID documentId) {
      return document.tenantId().equals(tenantId) && document.id().equals(documentId)
          ? Optional.of(document)
          : Optional.empty();
    }

    @Override
    public List<Document> findByTenantId(UUID tenantId) {
      return List.of();
    }

    @Override
    public Document save(Document document) {
      return document;
    }

    @Override
    public List<com.emme.studio.documents.domain.model.DocumentChunk> findChunks(
        UUID tenantId, UUID documentId) {
      return List.of();
    }

    @Override
    public void replaceChunks(
        UUID tenantId,
        UUID documentId,
        List<com.emme.studio.documents.domain.model.DocumentChunk> chunks) {}
  }
}
