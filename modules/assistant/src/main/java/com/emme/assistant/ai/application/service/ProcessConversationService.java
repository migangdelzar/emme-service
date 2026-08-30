package com.emme.assistant.ai.application.service;

import com.emme.assistant.ai.api.command.ProcessConversationCommand;
import com.emme.assistant.ai.api.result.ProcessConversationResult;
import com.emme.assistant.ai.api.usecase.ChatUseCase;
import com.emme.assistant.ai.api.usecase.ProcessConversationUseCase;
import com.emme.assistant.ai.application.port.out.ConversationMemoryPort;
import com.emme.assistant.ai.application.port.out.ConversationTurnIdempotencyPort;
import com.emme.assistant.api.result.ConversationEventDetails;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Coordinates durable conversation memory around the existing AI chat execution boundary. */
@Service
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class ProcessConversationService implements ProcessConversationUseCase {

  private final ConversationMemoryPort memory;
  private final ChatUseCase chat;
  private final ConversationTurnIdempotencyPort idempotency;

  public ProcessConversationService(
      ConversationMemoryPort memory,
      ChatUseCase chat,
      ConversationTurnIdempotencyPort idempotency) {
    this.memory = Objects.requireNonNull(memory, "memory must not be null");
    this.chat = Objects.requireNonNull(chat, "chat must not be null");
    this.idempotency = Objects.requireNonNull(idempotency, "idempotency must not be null");
  }

  @Override
  public ProcessConversationResult process(ProcessConversationCommand command) {
    AiExecutionContext context = AiExecutionContextScope.requireCurrent();
    validate(command, context);
    var completed = idempotency.find(command.conversationId(), command.idempotencyKey());
    if (completed.isPresent()) {
      return completed.get();
    }
    if (!idempotency.reserve(command.conversationId(), command.idempotencyKey())) {
      return idempotency
          .find(command.conversationId(), command.idempotencyKey())
          .orElseThrow(
              () -> new IllegalStateException("AI conversation turn is already in progress"));
    }
    try {
      return processReservedTurn(command, context);
    } catch (RuntimeException failure) {
      try {
        idempotency.release(command.conversationId(), command.idempotencyKey());
      } catch (RuntimeException cleanupFailure) {
        failure.addSuppressed(cleanupFailure);
      }
      throw failure;
    }
  }

  private ProcessConversationResult processReservedTurn(
      ProcessConversationCommand command, AiExecutionContext context) {
    ConversationMemoryPort.ConversationSnapshot snapshot =
        memory.load(command.conversationId(), context);
    if (!command.conversationId().equals(snapshot.conversationId())) {
      throw new SecurityException("Conversation memory returned an unexpected conversation");
    }

    memory.appendUserMessage(command.conversationId(), command.message(), context);
    String response = chat.chat(conversationContext(snapshot), command.message());
    if (response == null || response.isBlank()) {
      throw new IllegalStateException("AI conversation response must not be blank");
    }
    memory.appendAssistantMessage(command.conversationId(), response, context);
    ProcessConversationResult result =
        new ProcessConversationResult(command.conversationId(), context.workflowId(), response);
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
}
