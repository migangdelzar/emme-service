package com.emme.assistant.ai.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.emme.ai.contracts.model.AiModelProvider;
import com.emme.assistant.ai.application.port.out.EmbeddingModelPort;
import com.emme.assistant.ai.application.semantic.EmbeddingVector;
import com.emme.assistant.ai.configuration.AiProperties;
import com.emme.documents.api.usecase.SearchDocumentChunksUseCase;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DocumentKnowledgeRetrievalAdapterTest {

  @Test
  void rejectsLegacyVectorsThatDoNotMatchTheConfiguredCatalogDimension() {
    AiModelProvider legacyModel = mock(AiModelProvider.class);
    when(legacyModel.embed("question")).thenReturn(Collections.nCopies(1024, 0.0f));
    DocumentKnowledgeRetrievalAdapter adapter =
        new DocumentKnowledgeRetrievalAdapter(
            legacyModel,
            mock(SearchDocumentChunksUseCase.class),
            Optional.<EmbeddingModelPort>empty(),
            new AiProperties(
                "mock",
                null,
                new AiProperties.EmbeddingConfig(
                    "embeddinggemma:300m", "http://localhost:11434", null, 768),
                true));

    assertThatThrownBy(
            () ->
                AiExecutionContextScope.call(
                    context(), () -> adapter.retrieve("question", 1)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Embedding dimensions must match document_chunk schema");
  }

  private static AiExecutionContext context() {
    UUID id = UUID.randomUUID();
    return new AiExecutionContext(
        UUID.randomUUID(), UUID.randomUUID(), Set.of("client"), id, id, "trace-" + id, "idem-" + id);
  }
}
