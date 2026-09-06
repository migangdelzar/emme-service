package com.emme.ai.platform.adapter.out.provider.springai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.emme.ai.contracts.model.AiChatCompletion;
import com.emme.ai.contracts.model.ChatResponse;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SpringAiChatCompletionTest {

  @Test
  void returnsTheProviderIdentifiedResponseForTheBoundExecutionContext() {
    SpringAiChatModel model = mock(SpringAiChatModel.class);
    when(model.provider()).thenReturn("ollama");
    when(model.modelVersion()).thenReturn("qwen-v1");
    when(model.complete("context", "hello")).thenReturn("hola");
    SpringAiChatCompletion completion = new SpringAiChatCompletion(model);
    AiExecutionContext context = context();

    ChatResponse response =
        AiExecutionContextScope.call(
            context, () -> completion.complete(request(context, "context", "hello")));

    assertThat(response).isEqualTo(new ChatResponse("hola", "ollama", "qwen-v1", 0, 0));
  }

  @Test
  void rejectsARequestThatDoesNotMatchTheBoundExecutionContext() {
    SpringAiChatCompletion completion = new SpringAiChatCompletion(mock(SpringAiChatModel.class));
    AiExecutionContext bound = context();
    AiExecutionContext foreign = context();

    assertThatThrownBy(
            () ->
                AiExecutionContextScope.call(
                    bound, () -> completion.complete(request(foreign, "context", "hello"))))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("chat request context must match the bound AI execution context");
  }

  private static AiChatCompletion.Request request(
      AiExecutionContext context, String conversationContext, String userMessage) {
    return new AiChatCompletion.Request(
        conversationContext,
        userMessage,
        context,
        new AiChatCompletion.ProviderPolicy(List.of("ollama"), false));
  }

  private static AiExecutionContext context() {
    return new AiExecutionContext(
        UUID.randomUUID(),
        UUID.randomUUID(),
        Set.of("CLIENT"),
        UUID.randomUUID(),
        UUID.randomUUID(),
        "trace-1",
        "idempotency-1");
  }
}
