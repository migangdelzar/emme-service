package com.emme.assistant.ai.application.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.emme.assistant.ai.application.port.out.ChatCompletionPort;
import com.emme.assistant.ai.application.port.out.KnowledgeDocument;
import com.emme.assistant.ai.application.port.out.KnowledgeRetrievalPort;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RagAnswerProviderChainTest {

  @Test
  void delegatesRagQuestionsToTheOrderedCompletionChainWithoutConversationContext() {
    ChatCompletionPort completions = mock(ChatCompletionPort.class);
    KnowledgeRetrievalPort retriever = mock(KnowledgeRetrievalPort.class);
    when(completions.complete("24 hours.", "What is the cancellation policy?"))
        .thenReturn("The salon requires 24 hours.");
    when(retriever.retrieve("What is the cancellation policy?", 5))
        .thenReturn(List.of(new KnowledgeDocument("1", "24 hours.", 0.9)));
    RagAnswerProviderChain answers = new RagAnswerProviderChain(completions, retriever);

    String answer =
        AiExecutionContextScope.call(
            context(), () -> answers.answer("What is the cancellation policy?"));

    assertThat(answer).isEqualTo("The salon requires 24 hours.");
    verify(completions).complete("24 hours.", "What is the cancellation policy?");
  }

  @Test
  void failsClosedWhenTheBackendAiContextIsMissing() {
    ChatCompletionPort completions = mock(ChatCompletionPort.class);
    RagAnswerProviderChain answers =
        new RagAnswerProviderChain(completions, mock(KnowledgeRetrievalPort.class));

    assertThatThrownBy(() -> answers.answer("hello"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("No AI execution context");
    verifyNoInteractions(completions);
  }

  @Test
  void rejectsBlankQuestionsBeforeCallingAProvider() {
    ChatCompletionPort completions = mock(ChatCompletionPort.class);
    RagAnswerProviderChain answers =
        new RagAnswerProviderChain(completions, mock(KnowledgeRetrievalPort.class));

    assertThatThrownBy(() -> AiExecutionContextScope.call(context(), () -> answers.answer("  ")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("question must not be blank");
    verifyNoInteractions(completions);
  }

  @Test
  void refusesToCallTheLlmWhenRetrievalReturnsNoGrounding() {
    ChatCompletionPort completions = mock(ChatCompletionPort.class);
    KnowledgeRetrievalPort retriever = mock(KnowledgeRetrievalPort.class);
    when(retriever.retrieve("hello", 5)).thenReturn(List.of());
    RagAnswerProviderChain answers = new RagAnswerProviderChain(completions, retriever);

    assertThatThrownBy(() -> AiExecutionContextScope.call(context(), () -> answers.answer("hello")))
        .isInstanceOf(RetrievalUnavailableException.class)
        .hasMessage("RAG retrieval unavailable");
    verifyNoInteractions(completions);
  }

  private static AiExecutionContext context() {
    UUID id = UUID.randomUUID();
    return new AiExecutionContext(
        UUID.randomUUID(), UUID.randomUUID(), Set.of("ROLE_tenant_client"), id, id, "trace", "id");
  }
}
