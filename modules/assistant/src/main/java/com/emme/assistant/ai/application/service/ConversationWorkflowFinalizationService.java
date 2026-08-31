package com.emme.assistant.ai.application.service;

import com.emme.assistant.ai.api.result.ProcessConversationResult;
import com.emme.assistant.ai.api.usecase.ConversationWorkflowFinalizationUseCase;
import com.emme.assistant.ai.application.port.out.ConversationMemoryPort;
import com.emme.assistant.ai.application.port.out.ConversationTurnIdempotencyPort;
import com.emme.assistant.ai.application.port.out.ConversationWorkflowFinalizationPort;
import com.emme.assistant.ai.domain.workflow.ConversationWorkflowSnapshot;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Finalizes a resumed workflow as the original conversation owner, never as the reviewing staff.
 */
@Service
@Transactional
public class ConversationWorkflowFinalizationService
    implements ConversationWorkflowFinalizationPort, ConversationWorkflowFinalizationUseCase {

  private final ConversationMemoryPort memory;
  private final ConversationTurnIdempotencyPort idempotency;

  public ConversationWorkflowFinalizationService(
      ConversationMemoryPort memory, ConversationTurnIdempotencyPort idempotency) {
    this.memory = Objects.requireNonNull(memory, "memory must not be null");
    this.idempotency = Objects.requireNonNull(idempotency, "idempotency must not be null");
  }

  @Override
  public ProcessConversationResult finalize(ConversationWorkflowSnapshot workflow) {
    Objects.requireNonNull(workflow, "workflow must not be null");
    AiExecutionContext reviewer = AiExecutionContextScope.requireCurrent();
    AiExecutionContext ownerContext =
        new AiExecutionContext(
            workflow.tenantId(),
            workflow.principalId(),
            java.util.Set.of(),
            workflow.conversationId(),
            workflow.workflowId(),
            reviewer.traceId(),
            workflow.idempotencyKey());
    return AiExecutionContextScope.call(ownerContext, () -> finalizeOwnerTurn(workflow));
  }

  private ProcessConversationResult finalizeOwnerTurn(ConversationWorkflowSnapshot workflow) {
    return idempotency
        .find(workflow.conversationId(), workflow.idempotencyKey())
        .orElseGet(
            () -> {
              if (memory
                  .findUserMessage(
                      workflow.conversationId(),
                      workflow.idempotencyKey(),
                      AiExecutionContextScope.requireCurrent())
                  .isEmpty()) {
                memory.appendUserMessage(
                    workflow.conversationId(),
                    workflow.message(),
                    workflow.idempotencyKey(),
                    AiExecutionContextScope.requireCurrent());
              }
              if (memory
                  .findAssistantResponse(
                      workflow.conversationId(),
                      workflow.idempotencyKey(),
                      AiExecutionContextScope.requireCurrent())
                  .isEmpty()) {
                memory.appendAssistantMessage(
                    workflow.conversationId(),
                    workflow.response(),
                    workflow.idempotencyKey(),
                    AiExecutionContextScope.requireCurrent());
              }
              ProcessConversationResult result =
                  new ProcessConversationResult(
                      workflow.conversationId(), workflow.workflowId(), workflow.response());
              idempotency.complete(workflow.conversationId(), workflow.idempotencyKey(), result);
              return result;
            });
  }
}
