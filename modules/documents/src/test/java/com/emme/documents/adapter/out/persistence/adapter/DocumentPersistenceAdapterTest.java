package com.emme.documents.adapter.out.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emme.documents.adapter.out.persistence.entity.DocumentEntity;
import com.emme.documents.adapter.out.persistence.repository.SpringDataDocumentChunkRepository;
import com.emme.documents.adapter.out.persistence.repository.SpringDataDocumentRepository;
import com.emme.documents.domain.model.Document;
import com.emme.documents.domain.model.DocumentChunk;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DocumentPersistenceAdapterTest {

  private final SpringDataDocumentRepository documents = org.mockito.Mockito.mock();
  private final SpringDataDocumentChunkRepository chunks = org.mockito.Mockito.mock();
  private final DocumentPersistenceAdapter adapter =
      new DocumentPersistenceAdapter(documents, chunks);

  @Test
  void savesDocumentThroughTheExistingJpaEntityMapping() {
    UUID tenantId = UUID.randomUUID();
    Document document = new Document(tenantId, "design.pdf", "UPLOAD");
    DocumentEntity entity = new DocumentEntity(tenantId, "old-name", "UPLOAD");
    entity.onCreate();

    when(documents.findById(document.id())).thenReturn(Optional.of(entity));
    when(documents.save(entity)).thenReturn(entity);

    Document saved = adapter.save(document);

    assertThat(saved.id()).isEqualTo(entity.getId());
    assertThat(saved.name()).isEqualTo(document.name());
    verify(documents).save(entity);
  }

  @Test
  void replacesChunksWithEntitiesCreatedByTheJpaMapping() {
    UUID tenantId = UUID.randomUUID();
    UUID documentId = UUID.randomUUID();
    List<DocumentChunk> newChunks =
        List.of(new DocumentChunk(tenantId, documentId, 0, "content", "fingerprint"));

    adapter.replaceChunks(tenantId, documentId, newChunks);

    verify(chunks).deleteByTenantIdAndDocumentId(tenantId, documentId);
    verify(chunks).flush();
    verify(chunks).saveAll(anyList());
  }
}
