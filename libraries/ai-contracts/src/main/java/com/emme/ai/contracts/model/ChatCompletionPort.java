package com.emme.ai.contracts.model;

import com.emme.ai.contracts.context.AiExecutionContext;

/** Provider-neutral chat completion port. */
public interface ChatCompletionPort {

  ChatResponse complete(ChatRequest request, AiExecutionContext context);
}
