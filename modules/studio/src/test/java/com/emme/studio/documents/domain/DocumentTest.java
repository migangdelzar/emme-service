package com.emme.studio.documents.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import com.emme.studio.documents.domain.model.Document;
import com.emme.studio.documents.domain.model.DocumentStatus;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DocumentTest {

  @Test
  void transitionsFromUploadedToReady() {
    Document document = new Document(UUID.randomUUID(), "guide.pdf", "PDF");

    document.markProcessing();
    document.markReady();

    assertThat(document.status()).isEqualTo(DocumentStatus.READY);
  }

  @Test
  void rejectsProcessingWhenDocumentIsNotUploaded() {
    Document document = new Document(UUID.randomUUID(), "guide.pdf", "PDF");
    document.markProcessing();
    document.markReady();

    assertThatIllegalStateException().isThrownBy(document::markProcessing);
  }
}
