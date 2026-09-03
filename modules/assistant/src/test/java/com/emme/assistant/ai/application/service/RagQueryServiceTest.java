package com.emme.assistant.ai.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.emme.ai.contracts.rag.KnowledgeQuery;
import com.emme.ai.contracts.rag.KnowledgeSearch;
import com.emme.ai.contracts.rag.RetrievedDocument;
import com.emme.ai.platform.configuration.AiProviderProperties;
import com.emme.assistant.ai.application.port.out.ChatCompletionPort;
import com.emme.assistant.ai.application.port.out.ChatProviderUnavailableException;
import com.emme.assistant.ai.application.port.out.RagAnswerPort;
import com.emme.assistant.ai.application.provider.RetrievalUnavailableException;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class RagQueryServiceTest {

  @Test
  void queriesThroughTheCanonicalKnowledgeSearchPort() {
    UUID tenantId = UUID.randomUUID();
    ChatCompletionPort chat = mock(ChatCompletionPort.class);
    KnowledgeSearch retrieval = mock(KnowledgeSearch.class);
    RagQueryService service = new RagQueryService(realProperties(), retrieval, chat);

    when(retrieval.search(any(), any()))
        .thenReturn(
            List.of(
                new RetrievedDocument(
                    "source-1", "The premium is monthly.", java.util.Map.of(), 0.91)));
    when(chat.complete("The premium is monthly.", "What is the premium?"))
        .thenReturn("It is monthly.");

    assertThat(inContext(tenantId, () -> service.query("What is the premium?")))
        .isEqualTo("It is monthly.");
  }

  @Test
  void embedsSearchesAndAnswersUsingRankedDocumentContext() {
    UUID tenantId = UUID.randomUUID();
    ChatCompletionPort chat = mock(ChatCompletionPort.class);
    KnowledgeSearch retrieval = mock(KnowledgeSearch.class);
    RagQueryService service = new RagQueryService(realProperties(), retrieval, chat);

    when(retrieval.search(any(), any()))
        .thenReturn(
            List.of(
                new RetrievedDocument(
                    "source-1", "The premium is monthly.", java.util.Map.of(), 0.91)));
    when(chat.complete("The premium is monthly.", "What is the premium?"))
        .thenReturn("It is monthly.");

    String answer = inContext(tenantId, () -> service.query("What is the premium?"));

    assertThat(answer).isEqualTo("It is monthly.");
    verify(retrieval).search(any(), any());
    verify(chat).complete("The premium is monthly.", "What is the premium?");
  }

  @Test
  void usesKeywordOnlySearchWhenEmbeddingIsUnavailable() {
    UUID tenantId = UUID.randomUUID();
    ChatCompletionPort chat = mock(ChatCompletionPort.class);
    KnowledgeSearch retrieval = mock(KnowledgeSearch.class);
    RagQueryService service = new RagQueryService(realProperties(), retrieval, chat);

    when(retrieval.search(any(), any())).thenReturn(List.of());

    String answer = inContext(tenantId, () -> service.query("Which cancellation rules apply?"));

    assertThat(answer).isEqualTo("No relevant documents were found.");
    verify(retrieval).search(any(), any());
    verify(chat, never()).complete(any(), any());
  }

  @Test
  void keepsTheCannedResponseInMockMode() {
    ChatCompletionPort chat = mock(ChatCompletionPort.class);
    KnowledgeSearch retrieval = mock(KnowledgeSearch.class);
    RagQueryService service =
        new RagQueryService(new AiProviderProperties("mock", null, null, true), retrieval, chat);

    String answer = inContext(UUID.randomUUID(), () -> service.query("hello"));

    assertThat(answer).contains("MOCK RAG").contains("hello");
    verifyNoInteractions(chat);
    verify(retrieval, never()).search(any(), any());
  }

  @Test
  void rejectsAQueryWhenTheBackendAiContextIsMissing() {
    ChatCompletionPort chat = mock(ChatCompletionPort.class);
    KnowledgeSearch retrieval = mock(KnowledgeSearch.class);
    RagQueryService service = new RagQueryService(realProperties(), retrieval, chat);

    org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.query("hello"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("No AI execution context");
  }

  @Test
  void rejectsBlankQuestionsBeforeSearchingOrCompleting() {
    ChatCompletionPort chat = mock(ChatCompletionPort.class);
    KnowledgeSearch retrieval = mock(KnowledgeSearch.class);
    RagQueryService service = new RagQueryService(realProperties(), retrieval, chat);

    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> inContext(UUID.randomUUID(), () -> service.query("  ")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("question must not be blank");
    verifyNoInteractions(chat, retrieval);
  }

  @Test
  void usesTheTenantFromTheBackendContextWhenSearchingDocuments() {
    UUID tenantId = UUID.randomUUID();
    ChatCompletionPort chat = mock(ChatCompletionPort.class);
    KnowledgeSearch retrieval = mock(KnowledgeSearch.class);
    RagQueryService service = new RagQueryService(realProperties(), retrieval, chat);

    when(retrieval.search(any(), any())).thenReturn(List.of());

    inContext(tenantId, () -> service.query("hello"));

    ArgumentCaptor<KnowledgeQuery> query = ArgumentCaptor.forClass(KnowledgeQuery.class);
    ArgumentCaptor<AiExecutionContext> context = ArgumentCaptor.forClass(AiExecutionContext.class);
    verify(retrieval).search(query.capture(), context.capture());
    assertThat(query.getValue()).isEqualTo(new KnowledgeQuery("hello", "es-MX", 5));
    assertThat(context.getValue().tenantId()).isEqualTo(tenantId);
  }

  @Test
  void usesTheCanonicalChatPort() {
    UUID tenantId = UUID.randomUUID();
    ChatCompletionPort chat = mock(ChatCompletionPort.class);
    KnowledgeSearch retrieval = mock(KnowledgeSearch.class);
    RagQueryService service = new RagQueryService(realProperties(), retrieval, chat);

    when(retrieval.search(any(), any()))
        .thenReturn(
            List.of(
                new RetrievedDocument(
                    "source-1", "The premium is monthly.", java.util.Map.of(), 0.91)));
    when(chat.complete("The premium is monthly.", "What is the premium?"))
        .thenReturn("It is monthly.");

    assertThat(inContext(tenantId, () -> service.query("What is the premium?")))
        .isEqualTo("It is monthly.");
    verify(retrieval).search(any(), any());
    verify(chat).complete("The premium is monthly.", "What is the premium?");
  }

  @Test
  void returnsBoundedUnavailableWithoutBypassingTheCanonicalChatBoundary() {
    UUID tenantId = UUID.randomUUID();
    ChatCompletionPort chat = mock(ChatCompletionPort.class);
    KnowledgeSearch retrieval = mock(KnowledgeSearch.class);
    RagQueryService service = new RagQueryService(realProperties(), retrieval, chat);

    when(retrieval.search(any(), any()))
        .thenReturn(
            List.of(
                new RetrievedDocument(
                    "source-1", "The premium is monthly.", java.util.Map.of(), 0.91)));
    when(chat.complete("The premium is monthly.", "What is the premium?"))
        .thenThrow(new ChatProviderUnavailableException("all configured providers unavailable"));

    assertThat(inContext(tenantId, () -> service.query("What is the premium?")))
        .isEqualTo("Retrieval unavailable.");
    verify(chat).complete("The premium is monthly.", "What is the premium?");
  }

  @Test
  void returnsExplicitRetrievalUnavailableWhenVectorEmbeddingFails() {
    UUID tenantId = UUID.randomUUID();
    KnowledgeSearch retrieval = mock(KnowledgeSearch.class);
    ChatCompletionPort chat = mock(ChatCompletionPort.class);
    RagQueryService service = new RagQueryService(realProperties(), retrieval, chat);

    when(retrieval.search(any(), any())).thenThrow(new IllegalStateException("vector unavailable"));
    assertThat(inContext(tenantId, () -> service.query("hello")))
        .isEqualTo("Retrieval unavailable.");
    verifyNoInteractions(chat);
    verify(retrieval).search(any(), any());
  }

  @Test
  void returnsExplicitRetrievalUnavailableWhenVectorSearchFails() {
    UUID tenantId = UUID.randomUUID();
    KnowledgeSearch retrieval = mock(KnowledgeSearch.class);
    ChatCompletionPort chat = mock(ChatCompletionPort.class);
    RagQueryService service = new RagQueryService(realProperties(), retrieval, chat);

    when(retrieval.search(any(), any()))
        .thenThrow(new IllegalStateException("pgvector unavailable"));
    assertThat(inContext(tenantId, () -> service.query("hello")))
        .isEqualTo("Retrieval unavailable.");
    verifyNoInteractions(chat);
    verify(retrieval).search(any(), any());
  }

  @Test
  void doesNotHideSecurityFailuresFromVectorSearch() {
    UUID tenantId = UUID.randomUUID();
    KnowledgeSearch retrieval = mock(KnowledgeSearch.class);
    ChatCompletionPort chat = mock(ChatCompletionPort.class);
    RagQueryService service = new RagQueryService(realProperties(), retrieval, chat);

    when(retrieval.search(any(), any())).thenThrow(new SecurityException("tenant denied"));

    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> inContext(tenantId, () -> service.query("hello")))
        .isInstanceOf(SecurityException.class)
        .hasMessage("tenant denied");
    verifyNoInteractions(chat);
    verify(retrieval).search(any(), any());
  }

  @Test
  void prefersTheConfiguredSpringRagAnswerPortWhenAvailable() {
    UUID tenantId = UUID.randomUUID();
    ChatCompletionPort chat = mock(ChatCompletionPort.class);
    RagAnswerPort ragAnswer = mock(RagAnswerPort.class);
    KnowledgeSearch retrieval = mock(KnowledgeSearch.class);
    RagQueryService service =
        new RagQueryService(
            realProperties(), retrieval, chat, java.util.Optional.of(ragAnswer));
    when(ragAnswer.answer("Which cancellation rules apply?"))
        .thenReturn("The salon requires 24 hours.");

    assertThat(inContext(tenantId, () -> service.query("Which cancellation rules apply?")))
        .isEqualTo("The salon requires 24 hours.");
    verify(ragAnswer).answer("Which cancellation rules apply?");
    verifyNoInteractions(chat, retrieval);
  }

  @Test
  void returnsExplicitRetrievalUnavailableWhenTheConfiguredRagAnswerPortCannotRetrieve() {
    UUID tenantId = UUID.randomUUID();
    ChatCompletionPort chat = mock(ChatCompletionPort.class);
    RagAnswerPort ragAnswer = mock(RagAnswerPort.class);
    KnowledgeSearch retrieval = mock(KnowledgeSearch.class);
    RagQueryService service =
        new RagQueryService(
            realProperties(), retrieval, chat, java.util.Optional.of(ragAnswer));
    when(ragAnswer.answer("hello")).thenThrow(new RetrievalUnavailableException());

    assertThat(inContext(tenantId, () -> service.query("hello")))
        .isEqualTo("Retrieval unavailable.");
    verify(ragAnswer).answer("hello");
    verifyNoInteractions(chat, retrieval);
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

  private static AiProviderProperties realProperties() {
    return new AiProviderProperties("ollama", null, null, false);
  }
}
