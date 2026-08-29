package com.emme.assistant.ai.application.provider;

import com.emme.assistant.ai.application.port.out.ChatCompletionPort;
import com.emme.assistant.ai.application.port.out.RagAnswerPort;
import com.emme.kernel.context.AiExecutionContextScope;
import java.util.Objects;

/**
 * Exposes an ordered, provider-neutral completion chain as the RAG answer port.
 *
 * <p>The wrapped completion chain owns provider failover and admission. This facade adds the RAG
 * boundary contract and ensures that retrieval-backed answers always have trusted backend context.
 */
public final class RagAnswerProviderChain implements RagAnswerPort {

  private final ChatCompletionPort completions;

  public RagAnswerProviderChain(ChatCompletionPort completions) {
    this.completions = Objects.requireNonNull(completions, "completions must not be null");
  }

  @Override
  public String answer(String question) {
    AiExecutionContextScope.requireCurrent();
    if (question == null || question.isBlank()) {
      throw new IllegalArgumentException("question must not be blank");
    }
    return completions.complete("", question);
  }
}
