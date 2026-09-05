package com.emme.assistant.ai.application.service;

import com.emme.ai.contracts.rag.KnowledgeQuery;
import com.emme.ai.contracts.rag.KnowledgeRetriever;
import com.emme.ai.contracts.rag.RetrievedDocument;
import com.emme.ai.platform.configuration.AiProviderProperties;
import com.emme.assistant.ai.api.usecase.RagQueryUseCase;
import com.emme.assistant.ai.application.port.out.ChatCompletionPort;
import com.emme.assistant.ai.application.port.out.ChatProviderUnavailableException;
import com.emme.assistant.ai.application.port.out.RagAnswerPort;
import com.emme.assistant.ai.application.provider.RetrievalUnavailableException;
import com.emme.assistant.ai.application.rag.KnowledgeAnswerService;
import com.emme.assistant.ai.application.rag.KnowledgeRoute;
import com.emme.assistant.ai.application.semantic.SemanticFailurePolicy;
import com.emme.kernel.context.AiExecutionContextScope;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class RagQueryService implements RagQueryUseCase {

  private static final String DEFAULT_LOCALE = "es-MX";

  private final AiProviderProperties properties;
  private final KnowledgeRetriever retrieval;
  private final ChatCompletionPort chatCompletion;
  private final Optional<RagAnswerPort> ragAnswer;
  private final Optional<KnowledgeAnswerService> knowledgeAnswer;

  public RagQueryService(
      AiProviderProperties properties,
      KnowledgeRetriever retrieval,
      ChatCompletionPort chatCompletion) {
    this(properties, retrieval, chatCompletion, Optional.empty(), Optional.empty());
  }

  @Autowired
  public RagQueryService(
      AiProviderProperties properties,
      KnowledgeRetriever retrieval,
      ChatCompletionPort chatCompletion,
      Optional<RagAnswerPort> ragAnswer) {
    this(properties, retrieval, chatCompletion, ragAnswer, Optional.empty());
  }

  @Autowired
  public RagQueryService(
      AiProviderProperties properties,
      KnowledgeRetriever retrieval,
      ChatCompletionPort chatCompletion,
      @Qualifier("aiGroundedRagAnswer") Optional<RagAnswerPort> ragAnswer,
      Optional<KnowledgeAnswerService> knowledgeAnswer) {
    this.properties = properties;
    this.retrieval = retrieval;
    this.chatCompletion = chatCompletion;
    this.ragAnswer = ragAnswer;
    this.knowledgeAnswer = knowledgeAnswer;
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
    if (knowledgeAnswer.isPresent()) {
      try {
        return knowledgeAnswer
            .orElseThrow()
            .answer(
                new KnowledgeQuery(question, DEFAULT_LOCALE, 5),
                KnowledgeRoute.GENERAL,
                executionContext)
            .text();
      } catch (RuntimeException failure) {
        SemanticFailurePolicy.rethrowSecurityFailure(failure);
        if (failure instanceof ChatProviderUnavailableException
            || failure instanceof RetrievalUnavailableException
            || SemanticFailurePolicy.isTransientVectorOrProviderFailure(failure)) {
          return "Retrieval unavailable.";
        }
        throw failure;
      }
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
      return "Retrieval unavailable.";
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
