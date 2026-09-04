package com.emme.assistant.ai.application.provider;

import com.emme.ai.contracts.rag.KnowledgeQuery;
import com.emme.ai.contracts.rag.KnowledgeSearch;
import com.emme.ai.contracts.rag.RetrievedDocument;
import com.emme.assistant.ai.application.port.out.ChatCompletionPort;
import com.emme.assistant.ai.application.port.out.RagAnswerPort;
import com.emme.kernel.context.AiExecutionContextScope;
import java.util.List;
import java.util.Objects;

/**
 * Exposes an ordered, provider-neutral completion chain as the RAG answer port.
 *
 * <p>The wrapped completion chain owns provider failover, admission, and the Spring AI retrieval
 * advisor. This facade adds the RAG boundary contract and ensures that retrieval-backed answers
 * always have trusted backend context.
 */
public final class RagAnswerProviderChain implements RagAnswerPort {

  private static final String DEFAULT_LOCALE = "es-MX";
  private static final int RETRIEVAL_LIMIT = 5;

  private final ChatCompletionPort completions;
  private final KnowledgeSearch retriever;

  public RagAnswerProviderChain(ChatCompletionPort completions, KnowledgeSearch retriever) {
    this.completions = Objects.requireNonNull(completions, "completions must not be null");
    this.retriever = Objects.requireNonNull(retriever, "retriever must not be null");
  }

  @Override
  public String answer(String question) {
    AiExecutionContextScope.requireCurrent();
    if (question == null || question.isBlank()) {
      throw new IllegalArgumentException("question must not be blank");
    }
    String context = groundedContext(question);
    if (context.isBlank()) {
      throw new RetrievalUnavailableException();
    }
    return completions.complete(context, question);
  }

  private String groundedContext(String question) {
    List<RetrievedDocument> documents =
        retriever.search(
            new KnowledgeQuery(question, DEFAULT_LOCALE, RETRIEVAL_LIMIT),
            AiExecutionContextScope.requireCurrent());
    return documents.stream()
        .map(RetrievedDocument::content)
        .filter(content -> content != null && !content.isBlank())
        .reduce((left, right) -> left + "\n\n" + right)
        .orElse("");
  }
}
