package com.emme.assistant.ai.application.provider;

import com.emme.ai.contracts.rag.KnowledgeSearch;
import com.emme.assistant.ai.application.port.out.ChatCompletionPort;
import com.emme.assistant.ai.application.port.out.RagAnswerPort;
import com.emme.kernel.context.AiExecutionContextScope;
import java.util.Objects;

/**
 * Exposes an ordered, provider-neutral completion chain as the RAG answer port.
 *
 * <p>The wrapped completion chain owns provider failover, admission, and the Spring AI retrieval
 * advisor. This facade adds the RAG boundary contract and ensures that retrieval-backed answers
 * always have trusted backend context.
 */
public final class RagAnswerProviderChain implements RagAnswerPort {

  private final ChatCompletionPort completions;

  public RagAnswerProviderChain(ChatCompletionPort completions, KnowledgeSearch retriever) {
    this.completions = Objects.requireNonNull(completions, "completions must not be null");
    Objects.requireNonNull(retriever, "retriever must not be null");
  }

  @Override
  public String answer(String question) {
    AiExecutionContextScope.requireCurrent();
    if (question == null || question.isBlank()) {
      throw new IllegalArgumentException("question must not be blank");
    }
    // RetrievalAugmentationAdvisor owns the single KnowledgeSearch execution for this request.
    // Spring AI's default contextual augmenter fails closed when that retrieval is empty.
    return completions.complete("", question);
  }
}
