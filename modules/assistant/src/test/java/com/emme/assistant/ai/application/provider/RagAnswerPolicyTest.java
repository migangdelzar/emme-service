package com.emme.assistant.ai.application.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.emme.assistant.ai.application.port.out.ChatCompletionPort;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RagAnswerPolicyTest {

  @Test
  void delegatesGroundingAndProviderSelectionToTheSpringAiCompletionPipeline() {
    ChatCompletionPort completions = mock(ChatCompletionPort.class);
    RagAnswerPolicy answers = new RagAnswerPolicy(completions);
    org.mockito.Mockito.when(completions.complete("", "What is the cancellation policy?"))
        .thenReturn("The salon requires 24 hours.");

    String answer =
        AiExecutionContextScope.call(
            context(), () -> answers.answer("What is the cancellation policy?"));

    assertThat(answer).isEqualTo("The salon requires 24 hours.");
    verify(completions).complete("", "What is the cancellation policy?");
  }

  @Test
  void failsClosedWhenTheBackendAiContextIsMissing() {
    ChatCompletionPort completions = mock(ChatCompletionPort.class);
    RagAnswerPolicy answers = new RagAnswerPolicy(completions);

    assertThatThrownBy(() -> answers.answer("hello"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("No AI execution context");
    verifyNoInteractions(completions);
  }

  @Test
  void rejectsBlankQuestionsBeforeCallingTheCompletionPipeline() {
    ChatCompletionPort completions = mock(ChatCompletionPort.class);
    RagAnswerPolicy answers = new RagAnswerPolicy(completions);

    assertThatThrownBy(() -> AiExecutionContextScope.call(context(), () -> answers.answer("  ")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("question must not be blank");
    verifyNoInteractions(completions);
  }

  private static AiExecutionContext context() {
    UUID id = UUID.randomUUID();
    return new AiExecutionContext(
        UUID.randomUUID(), UUID.randomUUID(), Set.of("ROLE_tenant_client"), id, id, "trace", "id");
  }
}
