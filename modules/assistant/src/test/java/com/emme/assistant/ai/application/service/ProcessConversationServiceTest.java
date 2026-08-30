package com.emme.assistant.ai.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.emme.assistant.ai.api.command.ProcessConversationCommand;
import com.emme.assistant.ai.api.result.ProcessConversationResult;
import com.emme.assistant.ai.api.usecase.ChatUseCase;
import com.emme.assistant.ai.application.port.out.ConversationMemoryPort;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class ProcessConversationServiceTest {

  private static final UUID TENANT_ID = UUID.randomUUID();
  private static final UUID CONVERSATION_ID = UUID.randomUUID();
  private static final UUID WORKFLOW_ID = UUID.randomUUID();
  private static final String IDEMPOTENCY_KEY = "conversation-turn-1";

  @Test
  void rejectsConversationOutsideAuthenticatedTenant() {
    ConversationMemoryPort memory = mock(ConversationMemoryPort.class);
    ChatUseCase chat = mock(ChatUseCase.class);
    ProcessConversationService service = new ProcessConversationService(memory, chat);
    ProcessConversationCommand command = commandFor(UUID.randomUUID());
    when(memory.load(eq(command.conversationId()), any(AiExecutionContext.class)))
        .thenThrow(
            new SecurityException("Conversation is not accessible for the authenticated tenant"));

    assertThatThrownBy(() -> inContext(() -> service.process(command)))
        .isInstanceOf(SecurityException.class);
  }

  @Test
  void rejectsBlankMessageAndMissingIdempotencyKey() {
    ConversationMemoryPort memory = mock(ConversationMemoryPort.class);
    ChatUseCase chat = mock(ChatUseCase.class);
    ProcessConversationService service = new ProcessConversationService(memory, chat);

    assertThatIllegalArgumentException()
        .isThrownBy(
            () ->
                inContext(
                    () ->
                        service.process(
                            new ProcessConversationCommand(
                                CONVERSATION_ID, " ", "WEB", IDEMPOTENCY_KEY))))
        .withMessage("message must not be blank");
    assertThatIllegalArgumentException()
        .isThrownBy(
            () ->
                inContext(
                    () ->
                        service.process(
                            new ProcessConversationCommand(
                                CONVERSATION_ID, "question", "WEB", " "))))
        .withMessage("idempotencyKey must not be blank");
  }

  @Test
  void persistsBothSidesOfACompletedConversationTurn() {
    ConversationMemoryPort memory = mock(ConversationMemoryPort.class);
    ChatUseCase chat = mock(ChatUseCase.class);
    ProcessConversationService service = new ProcessConversationService(memory, chat);
    ProcessConversationCommand command = commandFor(CONVERSATION_ID);
    AiExecutionContext context = context();
    when(memory.load(CONVERSATION_ID, context))
        .thenReturn(new ConversationMemoryPort.ConversationSnapshot(CONVERSATION_ID, List.of()));
    when(chat.chat("", "question")).thenReturn("answer");

    ProcessConversationResult result =
        AiExecutionContextScope.call(context, () -> service.process(command));

    assertThat(result.conversationId()).isEqualTo(CONVERSATION_ID);
    assertThat(result.workflowId()).isEqualTo(WORKFLOW_ID);
    assertThat(result.response()).isEqualTo("answer");
    InOrder calls = inOrder(memory, chat);
    calls.verify(memory).appendUserMessage(CONVERSATION_ID, "question", context);
    calls.verify(chat).chat("", "question");
    calls.verify(memory).appendAssistantMessage(CONVERSATION_ID, "answer", context);
  }

  private static ProcessConversationCommand commandFor(UUID conversationId) {
    return new ProcessConversationCommand(conversationId, "question", "WEB", IDEMPOTENCY_KEY);
  }

  private static AiExecutionContext context() {
    return new AiExecutionContext(
        TENANT_ID,
        UUID.randomUUID(),
        Set.of("ROLE_tenant_client"),
        CONVERSATION_ID,
        WORKFLOW_ID,
        "trace-conversation-1",
        IDEMPOTENCY_KEY);
  }

  private static <T> T inContext(java.util.function.Supplier<T> action) {
    return AiExecutionContextScope.call(context(), action::get);
  }
}
