package com.emme.assistant.ai.application.service;

import com.emme.ai.contracts.rag.KnowledgeQuery;
import com.emme.ai.contracts.rag.KnowledgeSearch;
import com.emme.ai.contracts.rag.RetrievedDocument;
import com.emme.ai.platform.configuration.AiProviderProperties;
import com.emme.assistant.ai.api.usecase.RagQueryUseCase;
import com.emme.assistant.ai.application.port.out.ChatCompletionPort;
import com.emme.assistant.ai.application.port.out.ChatProviderUnavailableException;
import com.emme.assistant.ai.application.port.out.RagAnswerPort;
import com.emme.assistant.ai.application.provider.RetrievalUnavailableException;
import com.emme.assistant.ai.application.semantic.SemanticFailurePolicy;
import com.emme.kernel.context.AiExecutionContextScope;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RagQueryService implements RagQueryUseCase {

  private static final String DEFAULT_LOCALE = "es-MX";

  private final AiProviderProperties properties;
  private final KnowledgeSearch retrieval;
  private final ChatCompletionPort chatCompletion;
  private final Optional<RagAnswerPort> ragAnswer;

  public RagQueryService(
      AiProviderProperties properties,
      KnowledgeSearch retrieval,
      ChatCompletionPort chatCompletion) {
    this(properties, retrieval, chatCompletion, Optional.empty());
  }

  @Autowired
  public RagQueryService(
      AiProviderProperties properties,
      KnowledgeSearch retrieval,
      ChatCompletionPort chatCompletion,
      Optional<RagAnswerPort> ragAnswer) {
    this.properties = properties;
    this.retrieval = retrieval;
    this.chatCompletion = chatCompletion;
    this.ragAnswer = ragAnswer;
  }

  @Override
  public String query(String question) {
    var executionContext = AiExecutionContextScope.requireCurrent();
    if (question == null || question.isBlank()) {
      throw new IllegalArgumentException("question must not be blank");
    }
    if (isMock()) {
      return "MOCK RAG: Based on your documents, the answer to your question about '"
          + question
          + "' is that you should contact the salon for specific details.";
    }
    try {
      if (ragAnswer.isPresent()) {
        try {
          return ragAnswer.orElseThrow().answer(question);
        } catch (RetrievalUnavailableException unavailable) {
          return "Retrieval unavailable.";
        }
      }
    } catch (ChatProviderUnavailableException unavailable) {
      // Preserve the provider-neutral retrieval path as the compatibility fallback.
    }
    try {
      List<RetrievedDocument> documents =
          retrieval.search(new KnowledgeQuery(question, DEFAULT_LOCALE, 5), executionContext);
      String context =
          documents.stream()
              .map(RetrievedDocument::content)
              .filter(content -> content != null && !content.isBlank())
              .reduce((left, right) -> left + "\n\n" + right)
              .orElse("");
      if (context.isBlank()) {
        return "No relevant documents were found.";
      }
      return chatCompletion.complete(context, question);
    } catch (RuntimeException failure) {
      SemanticFailurePolicy.rethrowSecurityFailure(failure);
      return "Retrieval unavailable.";
    }
  }

  private boolean isMock() {
    return properties.provider() == null || "mock".equalsIgnoreCase(properties.provider());
  }
}
