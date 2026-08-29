package com.emme.assistant.ai.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emme.ai.contracts.model.AiModelProvider;
import com.emme.assistant.ai.configuration.AiProperties;
import com.emme.documents.api.result.DocumentChunkDetails;
import com.emme.documents.api.usecase.SearchDocumentChunksUseCase;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RagQueryServiceTest {

  @Test
  void embedsSearchesAndAnswersUsingRankedDocumentContext() {
    UUID tenantId = UUID.randomUUID();
    AiModelProvider model = mock(AiModelProvider.class);
    SearchDocumentChunksUseCase search = mock(SearchDocumentChunksUseCase.class);
    RagQueryService service = new RagQueryService(realProperties(), model, search);
    DocumentChunkDetails chunk =
        new DocumentChunkDetails(
            UUID.randomUUID(), UUID.randomUUID(), 0, "The premium is monthly.", "fingerprint");

    when(model.embed("What is the premium?")).thenReturn(List.of(0.1f, 0.2f));
    when(search.search(any())).thenReturn(List.of(chunk));
    when(model.chat("The premium is monthly.", "What is the premium?"))
        .thenReturn("It is monthly.");

    String answer = inContext(tenantId, () -> service.query("What is the premium?"));

    assertThat(answer).isEqualTo("It is monthly.");
    verify(model).embed("What is the premium?");
    verify(search).search(any());
    verify(model).chat("The premium is monthly.", "What is the premium?");
  }

  @Test
  void usesKeywordOnlySearchWhenEmbeddingIsUnavailable() {
    UUID tenantId = UUID.randomUUID();
    AiModelProvider model = mock(AiModelProvider.class);
    SearchDocumentChunksUseCase search = mock(SearchDocumentChunksUseCase.class);
    RagQueryService service = new RagQueryService(realProperties(), model, search);

    when(model.embed("Which cancellation rules apply?")).thenReturn(List.of());
    when(search.search(any())).thenReturn(List.of());

    String answer = inContext(tenantId, () -> service.query("Which cancellation rules apply?"));

    assertThat(answer).isEqualTo("No relevant documents were found.");
    verify(search).search(any());
    verify(model, never()).chat(any(), any());
  }

  @Test
  void keepsTheCannedResponseInMockMode() {
    AiModelProvider model = mock(AiModelProvider.class);
    SearchDocumentChunksUseCase search = mock(SearchDocumentChunksUseCase.class);
    RagQueryService service =
        new RagQueryService(new AiProperties("mock", null, null, true), model, search);

    String answer = inContext(UUID.randomUUID(), () -> service.query("hello"));

    assertThat(answer).contains("MOCK RAG").contains("hello");
    verify(model, never()).embed(any());
    verify(search, never()).search(any());
  }

  @Test
  void rejectsAQueryWhenTheBackendAiContextIsMissing() {
    AiModelProvider model = mock(AiModelProvider.class);
    SearchDocumentChunksUseCase search = mock(SearchDocumentChunksUseCase.class);
    RagQueryService service = new RagQueryService(realProperties(), model, search);

    org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.query("hello"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("No AI execution context");
  }

  @Test
  void usesTheTenantFromTheBackendContextWhenSearchingDocuments() {
    UUID tenantId = UUID.randomUUID();
    AiModelProvider model = mock(AiModelProvider.class);
    SearchDocumentChunksUseCase search = mock(SearchDocumentChunksUseCase.class);
    RagQueryService service = new RagQueryService(realProperties(), model, search);

    when(model.embed("hello")).thenReturn(List.of(0.1f, 0.2f));
    when(search.search(any())).thenReturn(List.of());

    inContext(tenantId, () -> service.query("hello"));

    org.mockito.ArgumentCaptor<com.emme.documents.api.query.SearchDocumentChunksQuery> query =
        org.mockito.ArgumentCaptor.forClass(
            com.emme.documents.api.query.SearchDocumentChunksQuery.class);
    verify(search).search(query.capture());
    assertThat(query.getValue().tenantId()).isEqualTo(tenantId);
  }

  private static <T> T inContext(UUID tenantId, java.util.function.Supplier<T> action) {
    UUID resourceId = UUID.randomUUID();
    return AiExecutionContextScope.call(
        new AiExecutionContext(
            tenantId,
            UUID.randomUUID(),
            Set.of("ROLE_tenant_client"),
            resourceId,
            resourceId,
            "trace-rag",
            "idempotency-rag"),
        action::get);
  }

  private static AiProperties realProperties() {
    return new AiProperties("ollama", null, null, false);
  }
}
