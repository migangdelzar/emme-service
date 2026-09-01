package com.emme.assistant.ai.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.emme.ai.contracts.model.AiModelProvider;
import com.emme.assistant.ai.application.port.out.ChatCompletionPort;
import com.emme.assistant.ai.application.port.out.KnowledgeDocument;
import com.emme.assistant.ai.application.port.out.KnowledgeRetrievalPort;
import com.emme.assistant.ai.application.port.out.RagAnswerPort;
import com.emme.assistant.ai.application.provider.RetrievalUnavailableException;
import com.emme.assistant.ai.configuration.AiProperties;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RagQueryServiceTest {

  @Test
  void queriesThroughTheFrameworkNeutralRetrievalPort() {
    UUID tenantId = UUID.randomUUID();
    AiModelProvider model = mock(AiModelProvider.class);
    KnowledgeRetrievalPort retrieval = mock(KnowledgeRetrievalPort.class);
    RagQueryService service = new RagQueryService(realProperties(), model, retrieval);

    when(retrieval.retrieve("What is the premium?", 5))
        .thenReturn(List.of(new KnowledgeDocument("source-1", "The premium is monthly.", 0.91)));
    when(model.chat("The premium is monthly.", "What is the premium?"))
        .thenReturn("It is monthly.");

    assertThat(inContext(tenantId, () -> service.query("What is the premium?")))
        .isEqualTo("It is monthly.");
  }

  @Test
  void embedsSearchesAndAnswersUsingRankedDocumentContext() {
    UUID tenantId = UUID.randomUUID();
    AiModelProvider model = mock(AiModelProvider.class);
    KnowledgeRetrievalPort retrieval = mock(KnowledgeRetrievalPort.class);
    RagQueryService service = new RagQueryService(realProperties(), model, retrieval);

    when(retrieval.retrieve("What is the premium?", 5))
        .thenReturn(List.of(new KnowledgeDocument("source-1", "The premium is monthly.", 0.91)));
    when(model.chat("The premium is monthly.", "What is the premium?"))
        .thenReturn("It is monthly.");

    String answer = inContext(tenantId, () -> service.query("What is the premium?"));

    assertThat(answer).isEqualTo("It is monthly.");
    verify(retrieval).retrieve("What is the premium?", 5);
    verify(model).chat("The premium is monthly.", "What is the premium?");
  }

  @Test
  void usesKeywordOnlySearchWhenEmbeddingIsUnavailable() {
    UUID tenantId = UUID.randomUUID();
    AiModelProvider model = mock(AiModelProvider.class);
    KnowledgeRetrievalPort retrieval = mock(KnowledgeRetrievalPort.class);
    RagQueryService service = new RagQueryService(realProperties(), model, retrieval);

    when(retrieval.retrieve("Which cancellation rules apply?", 5)).thenReturn(List.of());

    String answer = inContext(tenantId, () -> service.query("Which cancellation rules apply?"));

    assertThat(answer).isEqualTo("No relevant documents were found.");
    verify(retrieval).retrieve("Which cancellation rules apply?", 5);
    verify(model, never()).chat(any(), any());
  }

  @Test
  void keepsTheCannedResponseInMockMode() {
    AiModelProvider model = mock(AiModelProvider.class);
    KnowledgeRetrievalPort retrieval = mock(KnowledgeRetrievalPort.class);
    RagQueryService service =
        new RagQueryService(new AiProperties("mock", null, null, true), model, retrieval);

    String answer = inContext(UUID.randomUUID(), () -> service.query("hello"));

    assertThat(answer).contains("MOCK RAG").contains("hello");
    verify(model, never()).embed(any());
    verify(retrieval, never()).retrieve(any(), any(Integer.class));
  }

  @Test
  void rejectsAQueryWhenTheBackendAiContextIsMissing() {
    AiModelProvider model = mock(AiModelProvider.class);
    KnowledgeRetrievalPort retrieval = mock(KnowledgeRetrievalPort.class);
    RagQueryService service = new RagQueryService(realProperties(), model, retrieval);

    org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.query("hello"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("No AI execution context");
  }

  @Test
  void usesTheTenantFromTheBackendContextWhenSearchingDocuments() {
    UUID tenantId = UUID.randomUUID();
    AiModelProvider model = mock(AiModelProvider.class);
    KnowledgeRetrievalPort retrieval = mock(KnowledgeRetrievalPort.class);
    RagQueryService service = new RagQueryService(realProperties(), model, retrieval);

    when(retrieval.retrieve("hello", 5)).thenReturn(List.of());

    inContext(tenantId, () -> service.query("hello"));

    verify(retrieval).retrieve("hello", 5);
  }

  @Test
  void prefersProviderNeutralEmbeddingAndChatPortsWhenConfigured() {
    UUID tenantId = UUID.randomUUID();
    AiModelProvider legacyModel = mock(AiModelProvider.class);
    ChatCompletionPort chat = mock(ChatCompletionPort.class);
    KnowledgeRetrievalPort retrieval = mock(KnowledgeRetrievalPort.class);
    RagQueryService service =
        new RagQueryService(realProperties(), legacyModel, retrieval, java.util.Optional.of(chat));

    when(retrieval.retrieve("What is the premium?", 5))
        .thenReturn(List.of(new KnowledgeDocument("source-1", "The premium is monthly.", 0.91)));
    when(chat.complete("The premium is monthly.", "What is the premium?"))
        .thenReturn("It is monthly.");

    assertThat(inContext(tenantId, () -> service.query("What is the premium?")))
        .isEqualTo("It is monthly.");
    verify(retrieval).retrieve("What is the premium?", 5);
    verify(chat).complete("The premium is monthly.", "What is the premium?");
    verify(legacyModel, never()).embed(any());
    verify(legacyModel, never()).chat(any(), any());
  }

  @Test
  void returnsExplicitRetrievalUnavailableWhenVectorEmbeddingFails() {
    UUID tenantId = UUID.randomUUID();
    AiModelProvider model = mock(AiModelProvider.class);
    KnowledgeRetrievalPort retrieval = mock(KnowledgeRetrievalPort.class);
    ChatCompletionPort chat = mock(ChatCompletionPort.class);
    RagQueryService service =
        new RagQueryService(realProperties(), model, retrieval, java.util.Optional.of(chat));

    when(retrieval.retrieve("hello", 5)).thenThrow(new IllegalStateException("vector unavailable"));
    assertThat(inContext(tenantId, () -> service.query("hello")))
        .isEqualTo("Retrieval unavailable.");
    verifyNoInteractions(chat);
    verifyNoInteractions(model);
    verify(retrieval).retrieve("hello", 5);
  }

  @Test
  void returnsExplicitRetrievalUnavailableWhenVectorSearchFails() {
    UUID tenantId = UUID.randomUUID();
    AiModelProvider model = mock(AiModelProvider.class);
    KnowledgeRetrievalPort retrieval = mock(KnowledgeRetrievalPort.class);
    ChatCompletionPort chat = mock(ChatCompletionPort.class);
    RagQueryService service =
        new RagQueryService(realProperties(), model, retrieval, java.util.Optional.of(chat));

    when(retrieval.retrieve("hello", 5))
        .thenThrow(new IllegalStateException("pgvector unavailable"));
    assertThat(inContext(tenantId, () -> service.query("hello")))
        .isEqualTo("Retrieval unavailable.");
    verifyNoInteractions(chat);
    verifyNoInteractions(model);
    verify(retrieval).retrieve("hello", 5);
  }

  @Test
  void doesNotHideSecurityFailuresFromVectorSearch() {
    UUID tenantId = UUID.randomUUID();
    AiModelProvider model = mock(AiModelProvider.class);
    KnowledgeRetrievalPort retrieval = mock(KnowledgeRetrievalPort.class);
    ChatCompletionPort chat = mock(ChatCompletionPort.class);
    RagQueryService service =
        new RagQueryService(realProperties(), model, retrieval, java.util.Optional.of(chat));

    when(retrieval.retrieve("hello", 5)).thenThrow(new SecurityException("tenant denied"));

    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> inContext(tenantId, () -> service.query("hello")))
        .isInstanceOf(SecurityException.class)
        .hasMessage("tenant denied");
    verifyNoInteractions(chat, model);
    verify(retrieval).retrieve("hello", 5);
  }

  @Test
  void prefersTheConfiguredSpringRagAnswerPortWhenAvailable() {
    UUID tenantId = UUID.randomUUID();
    AiModelProvider legacyModel = mock(AiModelProvider.class);
    ChatCompletionPort chat = mock(ChatCompletionPort.class);
    RagAnswerPort ragAnswer = mock(RagAnswerPort.class);
    KnowledgeRetrievalPort retrieval = mock(KnowledgeRetrievalPort.class);
    RagQueryService service =
        new RagQueryService(
            realProperties(),
            legacyModel,
            retrieval,
            java.util.Optional.of(chat),
            java.util.Optional.of(ragAnswer));
    when(ragAnswer.answer("Which cancellation rules apply?"))
        .thenReturn("The salon requires 24 hours.");

    assertThat(inContext(tenantId, () -> service.query("Which cancellation rules apply?")))
        .isEqualTo("The salon requires 24 hours.");
    verify(ragAnswer).answer("Which cancellation rules apply?");
    verifyNoInteractions(chat, retrieval, legacyModel);
  }

  @Test
  void returnsExplicitRetrievalUnavailableWhenTheConfiguredRagAnswerPortCannotRetrieve() {
    UUID tenantId = UUID.randomUUID();
    AiModelProvider legacyModel = mock(AiModelProvider.class);
    RagAnswerPort ragAnswer = mock(RagAnswerPort.class);
    KnowledgeRetrievalPort retrieval = mock(KnowledgeRetrievalPort.class);
    RagQueryService service =
        new RagQueryService(
            realProperties(),
            legacyModel,
            retrieval,
            java.util.Optional.empty(),
            java.util.Optional.of(ragAnswer));
    when(ragAnswer.answer("hello")).thenThrow(new RetrievalUnavailableException());

    assertThat(inContext(tenantId, () -> service.query("hello")))
        .isEqualTo("Retrieval unavailable.");
    verify(ragAnswer).answer("hello");
    verifyNoInteractions(retrieval, legacyModel);
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
