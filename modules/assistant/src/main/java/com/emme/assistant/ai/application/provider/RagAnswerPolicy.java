package com.emme.assistant.ai.application.provider;

import com.emme.ai.contracts.rag.KnowledgeQuery;
import com.emme.ai.contracts.rag.RetrievedDocument;
import com.emme.assistant.ai.application.port.out.ChatCompletionPort;
import com.emme.assistant.ai.application.port.out.RagAnswerPort;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/** Applies RAG answer input policy while Spring AI owns retrieval augmentation. */
public final class RagAnswerPolicy implements RagAnswerPort {

  private final ChatCompletionPort completions;

  public RagAnswerPolicy(ChatCompletionPort completions) {
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

  @Override
  public String answer(
      KnowledgeQuery query, List<RetrievedDocument> documents, AiExecutionContext context) {
    AiExecutionContext current = AiExecutionContextScope.requireCurrent();
    if (query == null || query.text() == null || query.text().isBlank()) {
      throw new IllegalArgumentException("query must not be blank");
    }
    if (context == null) {
      throw new IllegalArgumentException("context must not be null");
    }
    if (!current.equals(context)) {
      throw new IllegalArgumentException("context must match the current AI execution context");
    }
    if (documents == null || documents.isEmpty()) {
      throw new IllegalArgumentException("documents must not be empty");
    }
    String groundedContext =
        documents.stream()
            .map(RetrievedDocument::content)
            .filter(content -> content != null && !content.isBlank())
            .collect(Collectors.joining("\n\n"));
    if (groundedContext.isBlank()) {
      throw new IllegalArgumentException("documents must contain text");
    }
    return completions.complete(groundedContext, query.text());
  }
}
