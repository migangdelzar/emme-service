package com.emme.assistant.ai.application.service;

import com.emme.assistant.ai.api.command.ProcessConversationCommand;
import com.emme.assistant.ai.api.result.ProcessConversationResult;
import com.emme.assistant.ai.api.usecase.ChatUseCase;
import com.emme.assistant.ai.api.usecase.ProcessConversationUseCase;
import com.emme.assistant.ai.application.port.out.ConversationMemoryPort;
import com.emme.assistant.ai.application.port.out.ConversationTurnIdempotencyPort;
import com.emme.assistant.ai.application.port.out.ConversationWorkflowPort;
import com.emme.assistant.ai.domain.workflow.ConversationWorkflowSnapshot;
import com.emme.assistant.ai.domain.workflow.ConversationWorkflowStatus;
import com.emme.assistant.api.result.ConversationEventDetails;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Coordinates durable conversation memory around the existing AI chat execution boundary. */
@Service
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class ProcessConversationService implements ProcessConversationUseCase {

  private static final ConversationWorkflowPort COMPLETED_WORKFLOW =
      ProcessConversationService::completed;

  private final ConversationMemoryPort memory;
  private final ChatUseCase chat;
  private final ConversationTurnIdempotencyPort idempotency;
  private final ConversationWorkflowPort workflow;

  @Autowired
  public ProcessConversationService(
      ConversationMemoryPort memory,
      ChatUseCase chat,
      ConversationTurnIdempotencyPort idempotency,
      ObjectProvider<ConversationWorkflowPort> workflow) {
    this(memory, chat, idempotency, workflow.getIfAvailable(() -> COMPLETED_WORKFLOW));
  }

  public ProcessConversationService(
      ConversationMemoryPort memory,
      ChatUseCase chat,
      ConversationTurnIdempotencyPort idempotency) {
    this(memory, chat, idempotency, COMPLETED_WORKFLOW);
  }

  public ProcessConversationService(
      ConversationMemoryPort memory,
      ChatUseCase chat,
      ConversationTurnIdempotencyPort idempotency,
      ConversationWorkflowPort workflow) {
    this.memory = Objects.requireNonNull(memory, "memory must not be null");
    this.chat = Objects.requireNonNull(chat, "chat must not be null");
    this.idempotency = Objects.requireNonNull(idempotency, "idempotency must not be null");
    this.workflow = Objects.requireNonNull(workflow, "workflow must not be null");
  }

  @Override
  public ProcessConversationResult process(ProcessConversationCommand command) {
    AiExecutionContext context = AiExecutionContextScope.requireCurrent();
    validate(command, context);
    var completed = idempotency.find(command.conversationId(), command.idempotencyKey());
    if (completed.isPresent()) {
      return validateCompletedResult(completed.get(), command, context);
    }
    boolean reserved = idempotency.reserve(command.conversationId(), command.idempotencyKey());
    boolean assistantResponsePersisted = false;
    try {
      var persistedAssistantResponse =
          memory.findAssistantResponse(command.conversationId(), command.idempotencyKey(), context);
      if (persistedAssistantResponse.isPresent()) {
        assistantResponsePersisted = true;
        return completeRecoveredTurn(command, context, persistedAssistantResponse.get());
      }
      if (!reserved) {
        var replay = idempotency.find(command.conversationId(), command.idempotencyKey());
        if (replay.isPresent()) {
          return validateCompletedResult(replay.get(), command, context);
        }
        ConversationWorkflowSnapshot existing =
            validateWorkflow(workflow.startOrResume(command, context), command, context);
        if (existing.status() != ConversationWorkflowStatus.SUCCEEDED) {
          return waitingResult(command, context, existing.status());
        }
        throw new IllegalStateException("AI conversation turn is already in progress");
      }
      ConversationMemoryPort.ConversationSnapshot snapshot =
          memory.load(command.conversationId(), context);
      if (!command.conversationId().equals(snapshot.conversationId())) {
        throw new SecurityException("Conversation memory returned an unexpected conversation");
      }
      if (memory
          .findUserMessage(command.conversationId(), command.idempotencyKey(), context)
          .isEmpty()) {
        memory.appendUserMessage(
            command.conversationId(), command.message(), command.idempotencyKey(), context);
      }
      ConversationWorkflowSnapshot workflowSnapshot =
          validateWorkflow(workflow.startOrResume(command, context), command, context);
      if (workflowSnapshot.status() != ConversationWorkflowStatus.SUCCEEDED) {
        return waitingResult(command, context, workflowSnapshot.status());
      }
      String response = chat.chat(conversationContext(snapshot), command.message());
      ProcessConversationResult result =
          validateCompletedResult(
              new ProcessConversationResult(
                  command.conversationId(), context.workflowId(), response),
              command,
              context);
      memory.appendAssistantMessage(
          command.conversationId(), response, command.idempotencyKey(), context);
      assistantResponsePersisted = true;
      idempotency.complete(command.conversationId(), command.idempotencyKey(), result);
      return result;
    } catch (RuntimeException failure) {
      if (reserved && !assistantResponsePersisted) {
        try {
          idempotency.release(command.conversationId(), command.idempotencyKey());
        } catch (RuntimeException cleanupFailure) {
          failure.addSuppressed(cleanupFailure);
        }
      }
      throw failure;
    }
  }

  private ProcessConversationResult completeRecoveredTurn(
      ProcessConversationCommand command, AiExecutionContext context, String response) {
    ProcessConversationResult result =
        validateCompletedResult(
            new ProcessConversationResult(command.conversationId(), context.workflowId(), response),
            command,
            context);
    var completed = idempotency.find(command.conversationId(), command.idempotencyKey());
    if (completed.isPresent()) {
      return validateCompletedResult(completed.get(), command, context);
    }
    idempotency.complete(command.conversationId(), command.idempotencyKey(), result);
    return result;
  }

  private static String conversationContext(ConversationMemoryPort.ConversationSnapshot snapshot) {
    return snapshot.events().stream()
        .map(ProcessConversationService::formatEvent)
        .collect(Collectors.joining("\n"));
  }

  private static String formatEvent(ConversationEventDetails event) {
    return event.eventType() + ": " + event.payload();
  }

  private static void validate(ProcessConversationCommand command, AiExecutionContext context) {
    Objects.requireNonNull(command, "command must not be null");
    if (command.conversationId() == null) {
      throw new IllegalArgumentException("conversationId must not be null");
    }
    requireText(command.message(), "message");
    requireText(command.idempotencyKey(), "idempotencyKey");
    if (!context.conversationId().equals(command.conversationId())) {
      throw new SecurityException("Conversation does not match the authenticated AI context");
    }
    if (!context.idempotencyKey().equals(command.idempotencyKey())) {
      throw new SecurityException("Idempotency key does not match the authenticated AI context");
    }
  }

  private static void requireText(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
  }

  private static void requireValidAssistantResponse(String response) {
    if (response == null || response.isBlank()) {
      throw new IllegalStateException("AI conversation response must not be blank");
    }
    if (response.indexOf('\u0000') >= 0) {
      throw new IllegalStateException(
          "AI conversation response contains invalid control characters");
    }
  }

  private static ProcessConversationResult validateCompletedResult(
      ProcessConversationResult result,
      ProcessConversationCommand command,
      AiExecutionContext context) {
    if (result == null) {
      throw new IllegalStateException("Completed AI conversation result must not be null");
    }
    requireValidAssistantResponse(result.response());
    if (result.conversationId() == null) {
      throw new IllegalStateException(
          "Completed AI conversation result must include conversationId");
    }
    if (!command.conversationId().equals(result.conversationId())) {
      throw new IllegalStateException(
          "Completed AI conversation result does not match the authenticated conversation");
    }
    if (result.workflowId() == null) {
      throw new IllegalStateException("Completed AI conversation result must include workflowId");
    }
    if (!context.workflowId().equals(result.workflowId())) {
      throw new IllegalStateException(
          "Completed AI conversation result does not match the authenticated workflow");
    }
    return result;
  }

  private static ConversationWorkflowSnapshot completed(
      ProcessConversationCommand command, AiExecutionContext context) {
    return new ConversationWorkflowSnapshot(
        context.workflowId(),
        command.conversationId(),
        ConversationWorkflowStatus.SUCCEEDED,
        context.tenantId(),
        context.principalId(),
        command.message(),
        command.idempotencyKey(),
        "workflow completed");
  }

  private static ConversationWorkflowSnapshot validateWorkflow(
      ConversationWorkflowSnapshot snapshot,
      ProcessConversationCommand command,
      AiExecutionContext context) {
    if (snapshot == null) {
      throw new IllegalStateException("Conversation workflow must return a snapshot");
    }
    if (!context.workflowId().equals(snapshot.workflowId())) {
      throw new IllegalStateException("Conversation workflow returned an unexpected workflow");
    }
    if (!command.conversationId().equals(snapshot.conversationId())) {
      throw new IllegalStateException("Conversation workflow returned an unexpected conversation");
    }
    if (!context.tenantId().equals(snapshot.tenantId())) {
      throw new SecurityException("Conversation workflow returned an unexpected tenant");
    }
    if (!context.principalId().equals(snapshot.principalId())) {
      throw new SecurityException("Conversation workflow returned an unexpected owner");
    }
    if (snapshot.status() == ConversationWorkflowStatus.FAILED
        || snapshot.status() == ConversationWorkflowStatus.REJECTED) {
      throw new IllegalStateException("Conversation workflow cannot execute the turn");
    }
    return snapshot;
  }

  private static ProcessConversationResult waitingResult(
      ProcessConversationCommand command,
      AiExecutionContext context,
      ConversationWorkflowStatus status) {
    String response =
        switch (status) {
          case WAITING_FOR_APPROVAL -> "Your request is waiting for staff approval.";
          case WAITING_FOR_CONFIRMATION -> "Please confirm before we continue.";
          case CLARIFICATION_REQUIRED ->
              "We need a little more information before we can continue.";
          default ->
              throw new IllegalArgumentException(
                  "Workflow status is not a waiting state: " + status);
        };
    return new ProcessConversationResult(
        command.conversationId(), context.workflowId(), response, status);
  }
}
