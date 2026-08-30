package com.emme.assistant.ai.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emme.assistant.ai.api.command.ProcessConversationCommand;
import com.emme.assistant.ai.api.usecase.ChatUseCase;
import com.emme.assistant.ai.application.port.out.ConversationMemoryPort;
import com.emme.assistant.ai.application.port.out.ConversationTurnIdempotencyPort;
import com.emme.assistant.ai.application.port.out.ConversationWorkflowPort;
import com.emme.assistant.ai.domain.workflow.ConversationWorkflowSnapshot;
import com.emme.assistant.ai.domain.workflow.ConversationWorkflowStatus;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class ProcessConversationServiceWorkflowTest {

  private static final UUID TENANT_ID = UUID.randomUUID();
  private static final UUID PRINCIPAL_ID = UUID.randomUUID();
  private static final UUID CONVERSATION_ID = UUID.randomUUID();
  private static final UUID WORKFLOW_ID = UUID.randomUUID();

  @Test
  void startsTheTrustedWorkflowBeforeExecutingTheConversationBoundary() {
    ConversationMemoryPort memory = mock(ConversationMemoryPort.class);
    ChatUseCase chat = mock(ChatUseCase.class);
    ConversationTurnIdempotencyPort idempotency = mock(ConversationTurnIdempotencyPort.class);
    ConversationWorkflowPort workflow = mock(ConversationWorkflowPort.class);
    AiExecutionContext context = context();
    ProcessConversationCommand command =
        new ProcessConversationCommand(CONVERSATION_ID, "hello", "idempotency-2");
    when(idempotency.find(CONVERSATION_ID, "idempotency-2")).thenReturn(Optional.empty());
    when(idempotency.reserve(CONVERSATION_ID, "idempotency-2")).thenReturn(true);
    when(memory.findAssistantResponse(CONVERSATION_ID, "idempotency-2", context))
        .thenReturn(Optional.empty());
    when(memory.load(CONVERSATION_ID, context))
        .thenReturn(new ConversationMemoryPort.ConversationSnapshot(CONVERSATION_ID, List.of()));
    when(chat.chat("", "hello")).thenReturn("answer");
    when(workflow.startOrResume(command, context))
        .thenReturn(
            new ConversationWorkflowSnapshot(
                WORKFLOW_ID, CONVERSATION_ID, ConversationWorkflowStatus.SUCCEEDED));

    ProcessConversationService service =
        new ProcessConversationService(memory, chat, idempotency, workflow);

    AiExecutionContextScope.run(context, () -> service.process(command));

    InOrder calls = inOrder(workflow, memory, chat);
    calls.verify(memory).findAssistantResponse(CONVERSATION_ID, "idempotency-2", context);
    calls.verify(workflow).startOrResume(command, context);
    calls.verify(memory).load(CONVERSATION_ID, context);
    calls.verify(memory).appendUserMessage(CONVERSATION_ID, "hello", "idempotency-2", context);
    calls.verify(chat).chat("", "hello");
    verify(idempotency)
        .complete(eq(CONVERSATION_ID), eq("idempotency-2"), org.mockito.ArgumentMatchers.any());
  }

  @Test
  void returnsADurableWaitingResponseWithoutCallingTheChatModel() {
    ConversationMemoryPort memory = mock(ConversationMemoryPort.class);
    ChatUseCase chat = mock(ChatUseCase.class);
    ConversationTurnIdempotencyPort idempotency = mock(ConversationTurnIdempotencyPort.class);
    ConversationWorkflowPort workflow = mock(ConversationWorkflowPort.class);
    AiExecutionContext context = context();
    ProcessConversationCommand command =
        new ProcessConversationCommand(CONVERSATION_ID, "hello", "idempotency-2");
    when(idempotency.find(CONVERSATION_ID, "idempotency-2")).thenReturn(Optional.empty());
    when(idempotency.reserve(CONVERSATION_ID, "idempotency-2")).thenReturn(true);
    when(memory.findAssistantResponse(CONVERSATION_ID, "idempotency-2", context))
        .thenReturn(Optional.empty());
    when(workflow.startOrResume(command, context))
        .thenReturn(
            new ConversationWorkflowSnapshot(
                WORKFLOW_ID,
                CONVERSATION_ID,
                ConversationWorkflowStatus.WAITING_FOR_APPROVAL,
                TENANT_ID,
                PRINCIPAL_ID));
    ProcessConversationService service =
        new ProcessConversationService(memory, chat, idempotency, workflow);

    var result = AiExecutionContextScope.call(context, () -> service.process(command));

    assertThat(result.workflowStatus()).isEqualTo(ConversationWorkflowStatus.WAITING_FOR_APPROVAL);
    assertThat(result.isWaiting()).isTrue();
    verify(chat, never())
        .chat(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
    verify(idempotency).complete(eq(CONVERSATION_ID), eq("idempotency-2"), eq(result));
  }

  private static AiExecutionContext context() {
    return new AiExecutionContext(
        TENANT_ID,
        PRINCIPAL_ID,
        Set.of("CLIENT"),
        CONVERSATION_ID,
        WORKFLOW_ID,
        "trace-2",
        "idempotency-2");
  }
}
