package com.emme.assistant.ai.adapter.out.provider.springai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.emme.ai.contracts.rag.KnowledgeRetriever;
import com.emme.ai.contracts.rag.RetrievedDocument;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.ai.rag.Query;

class TenantScopedDocumentRetrieverTest {

  @Test
  void adaptsFrameworkNeutralResultsToSpringDocuments() {
    KnowledgeRetriever retrieval = mock(KnowledgeRetriever.class);
    when(retrieval.search(any(), any()))
        .thenReturn(
            List.of(
                new RetrievedDocument(
                    "source-1",
                    "Cancellation requires 24 hours.",
                    java.util.Map.of(
                        "documentType", "policy",
                        "locale", "es-MX",
                        "sourceId", "untrusted-source",
                        "score", "untrusted-score"),
                    0.92)));
    TenantScopedDocumentRetriever retriever = new TenantScopedDocumentRetriever(retrieval, 5);

    List<org.springframework.ai.document.Document> documents =
        AiExecutionContextScope.call(
            context(), () -> retriever.retrieve(new Query("What is the cancellation policy?")));

    assertThat(documents).hasSize(1);
    assertThat(documents.getFirst().getText()).isEqualTo("Cancellation requires 24 hours.");
    assertThat(documents.getFirst().getMetadata())
        .containsEntry("sourceId", "source-1")
        .containsEntry("documentType", "policy")
        .containsEntry("locale", "es-MX")
        .containsEntry("score", 0.92);
    verify(retrieval).search(any(), any());
  }

  @Test
  void failsClosedWhenTheQueryIsBlank() {
    KnowledgeRetriever retrieval = mock(KnowledgeRetriever.class);
    TenantScopedDocumentRetriever retriever = new TenantScopedDocumentRetriever(retrieval, 5);

    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> AiExecutionContextScope.call(context(), () -> retriever.retrieve(new Query(" "))))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("text cannot be null or empty");
    verifyNoInteractions(retrieval);
  }

  private static AiExecutionContext context() {
    UUID id = UUID.randomUUID();
    return new AiExecutionContext(
        UUID.randomUUID(), UUID.randomUUID(), Set.of("ROLE_tenant_client"), id, id, "trace", "id");
  }
}
