package com.emme.assistant.ai.application.provider;

import com.emme.assistant.ai.application.port.out.ChatCompletionPort;
import com.emme.assistant.ai.application.port.out.RagAnswerPort;
import com.emme.kernel.context.AiExecutionContextScope;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;

/**
 * Exposes an ordered, provider-neutral completion chain as the RAG answer port.
 *
 * <p>The wrapped completion chain owns provider failover and admission. This facade adds the RAG
 * boundary contract and ensures that retrieval-backed answers always have trusted backend context.
 */
public final class RagAnswerProviderChain implements RagAnswerPort {

  private final ChatCompletionPort completions;
  private final DocumentRetriever retriever;

  public RagAnswerProviderChain(ChatCompletionPort completions, DocumentRetriever retriever) {
    this.completions = Objects.requireNonNull(completions, "completions must not be null");
    this.retriever = Objects.requireNonNull(retriever, "retriever must not be null");
  }

  @Override
  public String answer(String question) {
    AiExecutionContextScope.requireCurrent();
    if (question == null || question.isBlank()) {
      throw new IllegalArgumentException("question must not be blank");
    }
    final List<Document> documents;
    try {
      documents = retriever.retrieve(new Query(question));
    } catch (RuntimeException failure) {
      throw new RetrievalUnavailableException(failure);
    }
    String grounding =
        documents == null
            ? ""
            : documents.stream()
                .filter(Objects::nonNull)
                .map(Document::getText)
                .filter(text -> text != null && !text.isBlank())
                .collect(Collectors.joining("\n\n"));
    if (grounding.isBlank()) {
      throw new RetrievalUnavailableException();
    }
    return completions.complete(grounding, question);
  }
}
