package com.emme.assistant.ai.application.semantic;

import com.emme.kernel.context.AiExecutionContext;

/** Creates one provider-neutral semantic query for the current authenticated operation. */
public interface SemanticQueryFactory {

  SemanticQuery create(String rawText, AiExecutionContext context);
}
