package com.emme.assistant.ai.application.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.emme.ai.contracts.model.AiChatCompletion;
import com.emme.ai.contracts.model.ChatResponse;
import com.emme.ai.contracts.rag.KnowledgeQuery;
import com.emme.ai.contracts.rag.RetrievedDocument;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RagAnswerPolicyTest {

  @Test
  void delegatesGroundingAndProviderSelectionToTheSpringAiCompletionPipeline() {
    AiChatCompletion completions = mock(AiChatCompletion.class);
    RagAnswerPolicy answers = new RagAnswerPolicy(completions, policy());
    org.mockito.Mockito.when(completions.complete(org.mockito.ArgumentMatchers.any()))
        .thenReturn(new ChatResponse("The salon requires 24 hours.", "test", "test-v1", 0, 0));
    AiExecutionContext expected = context();

    String answer =
        AiExecutionContextScope.call(
            expected, () -> answers.answer("What is the cancellation policy?"));

    assertThat(answer).isEqualTo("The salon requires 24 hours.");
    verify(completions)
        .complete(
            org.mockito.ArgumentMatchers.argThat(
                request ->
                    request.conversationContext().isEmpty()
                        && request.userMessage().equals("What is the cancellation policy?")
                        && request.executionContext().equals(expected)
                        && request.providerPolicy().equals(policy())));
  }

  @Test
  void failsClosedWhenTheBackendAiContextIsMissing() {
    AiChatCompletion completions = mock(AiChatCompletion.class);
    RagAnswerPolicy answers = new RagAnswerPolicy(completions, policy());

    assertThatThrownBy(() -> answers.answer("hello"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("No AI execution context");
    verifyNoInteractions(completions);
  }

  @Test
  void rejectsBlankQuestionsBeforeCallingTheCompletionPipeline() {
    AiChatCompletion completions = mock(AiChatCompletion.class);
    RagAnswerPolicy answers = new RagAnswerPolicy(completions, policy());

    assertThatThrownBy(() -> AiExecutionContextScope.call(context(), () -> answers.answer("  ")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("question must not be blank");
    verifyNoInteractions(completions);
  }

  @Test
  void generatesGroundedAnswersOnlyFromTheAcceptedDocuments() {
    AiChatCompletion completions = mock(AiChatCompletion.class);
    RagAnswerPolicy answers = new RagAnswerPolicy(completions, policy());
    KnowledgeQuery query = new KnowledgeQuery("What are the hours?", "es-MX", 5);
    List<RetrievedDocument> documents =
        List.of(new RetrievedDocument("hours", "We open at nine.", Map.of(), 0.92));
    org.mockito.Mockito.when(completions.complete(org.mockito.ArgumentMatchers.any()))
        .thenReturn(new ChatResponse("We open at nine.", "test", "test-v1", 0, 0));

    String answer =
        AiExecutionContextScope.call(
            context(),
            () -> answers.answer(query, documents, AiExecutionContextScope.requireCurrent()));

    assertThat(answer).isEqualTo("We open at nine.");
    verify(completions)
        .complete(
            org.mockito.ArgumentMatchers.argThat(
                request ->
                    request.conversationContext().equals("We open at nine.")
                        && request.userMessage().equals(query.text())
                        && request.providerPolicy().equals(policy())));
  }

  @Test
  void rejectsGroundedAnswersForAContextDifferentFromTheCurrentScope() {
    AiChatCompletion completions = mock(AiChatCompletion.class);
    RagAnswerPolicy answers = new RagAnswerPolicy(completions, policy());
    KnowledgeQuery query = new KnowledgeQuery("What are the hours?", "es-MX", 5);
    List<RetrievedDocument> documents =
        List.of(new RetrievedDocument("hours", "We open at nine.", Map.of(), 0.92));

    assertThatThrownBy(
            () ->
                AiExecutionContextScope.call(
                    context(), () -> answers.answer(query, documents, context())))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("context must match the current AI execution context");
    verifyNoInteractions(completions);
  }

  private static AiExecutionContext context() {
    UUID id = UUID.randomUUID();
    return new AiExecutionContext(
        UUID.randomUUID(), UUID.randomUUID(), Set.of("ROLE_tenant_client"), id, id, "trace", "id");
  }

  private static AiChatCompletion.ProviderPolicy policy() {
    return new AiChatCompletion.ProviderPolicy(List.of("test"), true);
  }
}
