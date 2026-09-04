package com.emme.ai.contracts.rag;

import com.emme.kernel.context.AiExecutionContext;

/** Applies answer-generation policy to a tenant-scoped knowledge question. */
@FunctionalInterface
public interface RagAnswerService {

  String answer(String question, AiExecutionContext context);
}
