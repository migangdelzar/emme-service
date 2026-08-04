package com.emme.assistant.ai.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emme.assistant.ai.application.port.out.ModelProvider;
import com.emme.assistant.ai.configuration.AiProperties;
import com.emme.studio.documents.api.result.DocumentChunkDetails;
import com.emme.studio.documents.api.usecase.SearchDocumentChunksUseCase;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RagQueryServiceTest {

  @Test
  void embedsSearchesAndAnswersUsingRankedDocumentContext() {
    UUID tenantId = UUID.randomUUID();
    ModelProvider model = mock(ModelProvider.class);
    SearchDocumentChunksUseCase search = mock(SearchDocumentChunksUseCase.class);
    RagQueryService service = new RagQueryService(realProperties(), model, search);
    DocumentChunkDetails chunk =
        new DocumentChunkDetails(
            UUID.randomUUID(), UUID.randomUUID(), 0, "The premium is monthly.", "fingerprint");

    when(model.embed("What is the premium?")).thenReturn(List.of(0.1f, 0.2f));
    when(search.search(any())).thenReturn(List.of(chunk));
    when(model.chat("The premium is monthly.", "What is the premium?"))
        .thenReturn("It is monthly.");

    String answer = service.query(tenantId, "What is the premium?");

    assertThat(answer).isEqualTo("It is monthly.");
    verify(model).embed("What is the premium?");
    verify(search).search(any());
    verify(model).chat("The premium is monthly.", "What is the premium?");
  }

  @Test
  void usesKeywordOnlySearchWhenEmbeddingIsUnavailable() {
    UUID tenantId = UUID.randomUUID();
    ModelProvider model = mock(ModelProvider.class);
    SearchDocumentChunksUseCase search = mock(SearchDocumentChunksUseCase.class);
    RagQueryService service = new RagQueryService(realProperties(), model, search);

    when(model.embed("Which cancellation rules apply?")).thenReturn(List.of());
    when(search.search(any())).thenReturn(List.of());

    String answer = service.query(tenantId, "Which cancellation rules apply?");

    assertThat(answer).isEqualTo("No relevant documents were found.");
    verify(search).search(any());
    verify(model, never()).chat(any(), any());
  }

  @Test
  void keepsTheCannedResponseInMockMode() {
    ModelProvider model = mock(ModelProvider.class);
    SearchDocumentChunksUseCase search = mock(SearchDocumentChunksUseCase.class);
    RagQueryService service =
        new RagQueryService(new AiProperties("mock", null, null, true), model, search);

    String answer = service.query(UUID.randomUUID(), "hello");

    assertThat(answer).contains("MOCK RAG").contains("hello");
    verify(model, never()).embed(any());
    verify(search, never()).search(any());
  }

  private static AiProperties realProperties() {
    return new AiProperties("ollama", null, null, false);
  }
}
