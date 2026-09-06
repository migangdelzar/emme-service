package com.emme.assistant.ai.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.emme.ai.contracts.guardrail.GuardrailAction;
import com.emme.ai.contracts.guardrail.GuardrailDecision;
import com.emme.ai.contracts.model.AiChatCompletion;
import com.emme.ai.contracts.model.ChatResponse;
import com.emme.ai.contracts.rag.KnowledgeQuery;
import com.emme.ai.contracts.rag.KnowledgeRetriever;
import com.emme.ai.contracts.rag.RetrievedDocument;
import com.emme.ai.platform.configuration.AiProviderProperties;
import com.emme.assistant.ai.application.guardrail.GroundingGuard;
import com.emme.assistant.ai.application.port.out.ChatProviderUnavailableException;
import com.emme.assistant.ai.application.port.out.RagAnswerPort;
import com.emme.assistant.ai.application.provider.RagAnswerPolicy;
import com.emme.assistant.ai.application.provider.RetrievalUnavailableException;
import com.emme.assistant.ai.application.rag.GroundedAnswer;
import com.emme.assistant.ai.application.rag.KnowledgeAnswerService;
import com.emme.assistant.ai.application.rag.KnowledgeRoute;
import com.emme.assistant.ai.application.rag.RetrievalQualityDecision;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

class RagQueryServiceTest {

  @Test
  void exposesOnlyOneSpringAutowiredConstructor() {
    assertThat(
            java.util.Arrays.stream(RagQueryService.class.getDeclaredConstructors())
                .filter(constructor -> constructor.isAnnotationPresent(Autowired.class)))
        .hasSize(1);
  }

  @Test
  void queriesThroughTheCanonicalKnowledgeRetrieverPort() {
    UUID tenantId = UUID.randomUUID();
    AiChatCompletion chat = mock(AiChatCompletion.class);
    KnowledgeRetriever retrieval = mock(KnowledgeRetriever.class);
    RagQueryService service = new RagQueryService(realProperties(), retrieval, chat);

    when(retrieval.search(any(), any()))
        .thenReturn(
            List.of(
                new RetrievedDocument(
                    "source-1", "The premium is monthly.", java.util.Map.of(), 0.91)));
    when(chat.complete(any()))
        .thenReturn(new ChatResponse("It is monthly.", "test", "test-v1", 0, 0));

    assertThat(inContext(tenantId, () -> service.query("What is the premium?")))
        .isEqualTo("It is monthly.");
  }

  @Test
  void embedsSearchesAndAnswersUsingRankedDocumentContext() {
    UUID tenantId = UUID.randomUUID();
    AiChatCompletion chat = mock(AiChatCompletion.class);
    KnowledgeRetriever retrieval = mock(KnowledgeRetriever.class);
    RagQueryService service = new RagQueryService(realProperties(), retrieval, chat);

    when(retrieval.search(any(), any()))
        .thenReturn(
            List.of(
                new RetrievedDocument(
                    "source-1", "The premium is monthly.", java.util.Map.of(), 0.91)));
    when(chat.complete(any()))
        .thenReturn(new ChatResponse("It is monthly.", "test", "test-v1", 0, 0));

    String answer = inContext(tenantId, () -> service.query("What is the premium?"));

    assertThat(answer).isEqualTo("It is monthly.");
    verify(retrieval).search(any(), any());
    verify(chat).complete(any());
  }

  @Test
  void usesKeywordOnlySearchWhenEmbeddingIsUnavailable() {
    UUID tenantId = UUID.randomUUID();
    AiChatCompletion chat = mock(AiChatCompletion.class);
    KnowledgeRetriever retrieval = mock(KnowledgeRetriever.class);
    RagQueryService service = new RagQueryService(realProperties(), retrieval, chat);

    when(retrieval.search(any(), any())).thenReturn(List.of());

    String answer = inContext(tenantId, () -> service.query("Which cancellation rules apply?"));

    assertThat(answer).isEqualTo("No relevant documents were found.");
    verify(retrieval).search(any(), any());
    verify(chat, never()).complete(any());
  }

  @Test
  void keepsTheCannedResponseInMockMode() {
    AiChatCompletion chat = mock(AiChatCompletion.class);
    KnowledgeRetriever retrieval = mock(KnowledgeRetriever.class);
    RagQueryService service =
        new RagQueryService(new AiProviderProperties("mock", null, null, true), retrieval, chat);

    String answer = inContext(UUID.randomUUID(), () -> service.query("hello"));

    assertThat(answer).contains("MOCK RAG").contains("hello");
    verifyNoInteractions(chat);
    verify(retrieval, never()).search(any(), any());
  }

  @Test
  void rejectsAQueryWhenTheBackendAiContextIsMissing() {
    AiChatCompletion chat = mock(AiChatCompletion.class);
    KnowledgeRetriever retrieval = mock(KnowledgeRetriever.class);
    RagQueryService service = new RagQueryService(realProperties(), retrieval, chat);

    org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.query("hello"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("No AI execution context");
  }

  @Test
  void rejectsBlankQuestionsBeforeSearchingOrCompleting() {
    AiChatCompletion chat = mock(AiChatCompletion.class);
    KnowledgeRetriever retrieval = mock(KnowledgeRetriever.class);
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
    AiChatCompletion chat = mock(AiChatCompletion.class);
    KnowledgeRetriever retrieval = mock(KnowledgeRetriever.class);
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
    AiChatCompletion chat = mock(AiChatCompletion.class);
    KnowledgeRetriever retrieval = mock(KnowledgeRetriever.class);
    RagQueryService service = new RagQueryService(realProperties(), retrieval, chat);

    when(retrieval.search(any(), any()))
        .thenReturn(
            List.of(
                new RetrievedDocument(
                    "source-1", "The premium is monthly.", java.util.Map.of(), 0.91)));
    when(chat.complete(any()))
        .thenReturn(new ChatResponse("It is monthly.", "test", "test-v1", 0, 0));

    assertThat(inContext(tenantId, () -> service.query("What is the premium?")))
        .isEqualTo("It is monthly.");
    verify(retrieval).search(any(), any());
    verify(chat).complete(any());
  }

  @Test
  void returnsBoundedUnavailableWithoutBypassingTheCanonicalChatBoundary() {
    UUID tenantId = UUID.randomUUID();
    AiChatCompletion chat = mock(AiChatCompletion.class);
    KnowledgeRetriever retrieval = mock(KnowledgeRetriever.class);
    RagQueryService service = new RagQueryService(realProperties(), retrieval, chat);

    when(retrieval.search(any(), any()))
        .thenReturn(
            List.of(
                new RetrievedDocument(
                    "source-1", "The premium is monthly.", java.util.Map.of(), 0.91)));
    when(chat.complete(any()))
        .thenThrow(new ChatProviderUnavailableException("all configured providers unavailable"));

    assertThat(inContext(tenantId, () -> service.query("What is the premium?")))
        .isEqualTo("Retrieval unavailable.");
    verify(chat).complete(any());
  }

  @Test
  void doesNotReenterRetrievalWhenTheConfiguredRagProviderIsUnavailable() {
    UUID tenantId = UUID.randomUUID();
    AiChatCompletion chat = mock(AiChatCompletion.class);
    RagAnswerPort ragAnswer = mock(RagAnswerPort.class);
    KnowledgeRetriever retrieval = mock(KnowledgeRetriever.class);
    RagQueryService service =
        new RagQueryService(realProperties(), retrieval, chat, java.util.Optional.of(ragAnswer));
    when(ragAnswer.answer("hello"))
        .thenThrow(new ChatProviderUnavailableException("all providers unavailable"));

    assertThat(inContext(tenantId, () -> service.query("hello")))
        .isEqualTo("Retrieval unavailable.");
    verify(ragAnswer).answer("hello");
    verifyNoInteractions(retrieval, chat);
  }

  @Test
  void returnsExplicitRetrievalUnavailableWhenVectorEmbeddingFails() {
    UUID tenantId = UUID.randomUUID();
    KnowledgeRetriever retrieval = mock(KnowledgeRetriever.class);
    AiChatCompletion chat = mock(AiChatCompletion.class);
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
    KnowledgeRetriever retrieval = mock(KnowledgeRetriever.class);
    AiChatCompletion chat = mock(AiChatCompletion.class);
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
    KnowledgeRetriever retrieval = mock(KnowledgeRetriever.class);
    AiChatCompletion chat = mock(AiChatCompletion.class);
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
    AiChatCompletion chat = mock(AiChatCompletion.class);
    RagAnswerPort ragAnswer = mock(RagAnswerPort.class);
    KnowledgeRetriever retrieval = mock(KnowledgeRetriever.class);
    RagQueryService service =
        new RagQueryService(realProperties(), retrieval, chat, java.util.Optional.of(ragAnswer));
    when(ragAnswer.answer("Which cancellation rules apply?"))
        .thenReturn("The salon requires 24 hours.");

    assertThat(inContext(tenantId, () -> service.query("Which cancellation rules apply?")))
        .isEqualTo("The salon requires 24 hours.");
    verify(ragAnswer).answer("Which cancellation rules apply?");
    verifyNoInteractions(chat, retrieval);
  }

  @Test
  void executesTheConfiguredRagCompositionThroughTheSpringAiAdvisor() {
    UUID tenantId = UUID.randomUUID();
    AiChatCompletion chat = mock(AiChatCompletion.class);
    KnowledgeRetriever retrieval = mock(KnowledgeRetriever.class);
    RagAnswerPort ragAnswer =
        new RagAnswerPolicy(chat, new AiChatCompletion.ProviderPolicy(List.of("test"), true));
    RagQueryService service =
        new RagQueryService(realProperties(), retrieval, chat, java.util.Optional.of(ragAnswer));
    when(chat.complete(any()))
        .thenReturn(new ChatResponse("It is monthly.", "test", "test-v1", 0, 0));

    assertThat(inContext(tenantId, () -> service.query("What is the premium?")))
        .isEqualTo("It is monthly.");
    verifyNoInteractions(retrieval);
    verify(chat).complete(any());
  }

  @Test
  void returnsExplicitRetrievalUnavailableWhenTheConfiguredRagAnswerPortCannotRetrieve() {
    UUID tenantId = UUID.randomUUID();
    AiChatCompletion chat = mock(AiChatCompletion.class);
    RagAnswerPort ragAnswer = mock(RagAnswerPort.class);
    KnowledgeRetriever retrieval = mock(KnowledgeRetriever.class);
    RagQueryService service =
        new RagQueryService(realProperties(), retrieval, chat, java.util.Optional.of(ragAnswer));
    when(ragAnswer.answer("hello")).thenThrow(new RetrievalUnavailableException());

    assertThat(inContext(tenantId, () -> service.query("hello")))
        .isEqualTo("Retrieval unavailable.");
    verify(ragAnswer).answer("hello");
    verifyNoInteractions(chat, retrieval);
  }

  @Test
  void prefersTheBoundedKnowledgeAnswerServiceWhenConfigured() {
    UUID tenantId = UUID.randomUUID();
    AiChatCompletion chat = mock(AiChatCompletion.class);
    KnowledgeRetriever retrieval = mock(KnowledgeRetriever.class);
    KnowledgeAnswerService knowledgeAnswer = mock(KnowledgeAnswerService.class);
    AiExecutionContext context = context(tenantId);
    KnowledgeQuery query = new KnowledgeQuery("Which cancellation rules apply?", "es-MX", 5);
    GroundedAnswer grounded =
        new GroundedAnswer(
            "The salon requires 24 hours.",
            KnowledgeRoute.GENERAL,
            new RetrievalQualityDecision(true, 0.92, 0.80, 0.12, 2, 2, true, "ACCEPTED"),
            true);
    when(knowledgeAnswer.answer(query, KnowledgeRoute.GENERAL, context)).thenReturn(grounded);
    RagQueryService service =
        new RagQueryService(
            realProperties(),
            retrieval,
            chat,
            java.util.Optional.empty(),
            java.util.Optional.of(knowledgeAnswer));

    assertThat(AiExecutionContextScope.call(context, () -> service.query(query.text())))
        .isEqualTo(grounded.text());
    verify(knowledgeAnswer).answer(query, KnowledgeRoute.GENERAL, context);
    verifyNoInteractions(chat, retrieval);
  }

  @Test
  void returnsNoAnswerWhenGroundingGuardRejectsTheKnowledgeAnswer() {
    UUID tenantId = UUID.randomUUID();
    AiChatCompletion chat = mock(AiChatCompletion.class);
    KnowledgeRetriever retrieval = mock(KnowledgeRetriever.class);
    KnowledgeAnswerService knowledgeAnswer = mock(KnowledgeAnswerService.class);
    GroundingGuard grounding = mock(GroundingGuard.class);
    AiExecutionContext context = context(tenantId);
    KnowledgeQuery query = new KnowledgeQuery("Which cancellation rules apply?", "es-MX", 5);
    GroundedAnswer ungrounded =
        new GroundedAnswer(
            "The salon requires 24 hours.",
            KnowledgeRoute.GENERAL,
            new RetrievalQualityDecision(false, 0.20, 0.01, 0.19, 1, 0, false, "INSUFFICIENT"),
            false);
    when(knowledgeAnswer.answer(query, KnowledgeRoute.GENERAL, context)).thenReturn(ungrounded);
    when(grounding.check(any(), any()))
        .thenReturn(
            new GuardrailDecision(
                GuardrailAction.NO_ANSWER, "grounding.rejected", java.util.Map.of()));
    RagQueryService service =
        new RagQueryService(
            realProperties(),
            retrieval,
            chat,
            java.util.Optional.empty(),
            java.util.Optional.of(knowledgeAnswer),
            java.util.Optional.of(grounding));

    assertThat(AiExecutionContextScope.call(context, () -> service.query(query.text())))
        .isEqualTo("No relevant documents were found.");
    verify(grounding).check(any(), any());
    verifyNoInteractions(chat, retrieval);
  }

  private static <T> T inContext(UUID tenantId, java.util.function.Supplier<T> action) {
    return AiExecutionContextScope.call(context(tenantId), action::get);
  }

  private static AiExecutionContext context(UUID tenantId) {
    UUID resourceId = UUID.randomUUID();
    return new AiExecutionContext(
        tenantId,
        UUID.randomUUID(),
        Set.of("ROLE_tenant_client"),
        resourceId,
        resourceId,
        "trace-rag",
        "idempotency-rag");
  }

  private static AiProviderProperties realProperties() {
    return new AiProviderProperties("ollama", null, null, false);
  }
}
