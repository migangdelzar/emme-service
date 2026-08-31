package com.emme.assistant.ai.application.port.out;

import com.emme.assistant.ai.api.result.ProcessConversationResult;
import com.emme.assistant.ai.domain.workflow.ConversationWorkflowSnapshot;

/** Completes the original durable conversation turn after a workflow reaches a terminal success. */
@FunctionalInterface
public interface ConversationWorkflowFinalizationPort {

  ProcessConversationResult finalize(ConversationWorkflowSnapshot workflow);
}
