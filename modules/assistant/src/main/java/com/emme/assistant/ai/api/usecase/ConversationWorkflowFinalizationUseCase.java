package com.emme.assistant.ai.api.usecase;

import com.emme.assistant.ai.api.result.ProcessConversationResult;
import com.emme.assistant.ai.domain.workflow.ConversationWorkflowSnapshot;

/** Completes the original conversation turn after an approved workflow reaches success. */
public interface ConversationWorkflowFinalizationUseCase {

  ProcessConversationResult finalize(ConversationWorkflowSnapshot workflow);
}
