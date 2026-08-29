package com.emme.ai.contracts.model;

import com.emme.kernel.context.AiExecutionContext;

/** Provider-neutral chat completion port. */
public interface ChatCompletionPort {

  ChatResponse complete(ChatRequest request, AiExecutionContext context);
}
