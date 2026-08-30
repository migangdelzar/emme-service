package com.emme.assistant.ai.api.usecase;

import com.emme.assistant.ai.api.command.ProcessConversationCommand;
import com.emme.assistant.ai.api.result.ProcessConversationResult;

/** Processes one authenticated, durable AI conversation turn. */
public interface ProcessConversationUseCase {

  ProcessConversationResult process(ProcessConversationCommand command);
}
