package com.emme.assistant.ai.application.service;

import com.emme.ai.contracts.guardrail.GroundingRequest;
import com.emme.ai.contracts.guardrail.GuardrailAction;
import com.emme.ai.contracts.model.AiChatCompletion;
import com.emme.ai.contracts.rag.KnowledgeQuery;
import com.emme.ai.contracts.rag.KnowledgeRetriever;
import com.emme.ai.contracts.rag.RetrievedDocument;
import com.emme.ai.platform.configuration.AiProviderProperties;
import com.emme.assistant.ai.api.usecase.RagQueryUseCase;
import com.emme.assistant.ai.application.guardrail.GroundingGuard;
import com.emme.assistant.ai.application.port.out.ChatProviderUnavailableException;
import com.emme.assistant.ai.application.port.out.RagAnswerPort;
import com.emme.assistant.ai.application.provider.RetrievalUnavailableException;
import com.emme.assistant.ai.application.rag.KnowledgeAnswerService;
import com.emme.assistant.ai.application.rag.KnowledgeRoute;
import com.emme.assistant.ai.application.semantic.SemanticFailurePolicy;
import com.emme.assistant.ai.configuration.SpringAiChatProperties;
import com.emme.kernel.context.AiExecutionContextScope;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class RagQueryService implements RagQueryUseCase {

  private static final String DEFAULT_LOCALE = "es-MX";
  private static final String NO_RELEVANT_DOCUMENTS = "No relevant documents were found.";

  private final AiProviderProperties properties;
  private final KnowledgeRetriever retrieval;
  private final AiChatCompletion chatCompletion;
  private final AiChatCompletion.ProviderPolicy providerPolicy;
  private final Optional<RagAnswerPort> ragAnswer;
  private final Optional<KnowledgeAnswerService> knowledgeAnswer;
  private final Optional<GroundingGuard> groundingGuard;

  public RagQueryService(
      AiProviderProperties properties,
      KnowledgeRetriever retrieval,
      AiChatCompletion chatCompletion) {
    this(properties, retrieval, chatCompletion, Optional.empty(), Optional.empty());
  }

  public RagQueryService(
      AiProviderProperties properties,
      KnowledgeRetriever retrieval,
      AiChatCompletion chatCompletion,
      Optional<RagAnswerPort> ragAnswer) {
    this(properties, retrieval, chatCompletion, ragAnswer, Optional.empty());
  }

  public RagQueryService(
      AiProviderProperties properties,
      KnowledgeRetriever retrieval,
      AiChatCompletion chatCompletion,
      @Qualifier("aiGroundedRagAnswer") Optional<RagAnswerPort> ragAnswer,
      Optional<KnowledgeAnswerService> knowledgeAnswer) {
    this(properties, retrieval, chatCompletion, ragAnswer, knowledgeAnswer, Optional.empty());
  }

  public RagQueryService(
      AiProviderProperties properties,
      KnowledgeRetriever retrieval,
      AiChatCompletion chatCompletion,
      @Qualifier("aiGroundedRagAnswer") Optional<RagAnswerPort> ragAnswer,
      Optional<KnowledgeAnswerService> knowledgeAnswer,
      Optional<GroundingGuard> groundingGuard) {
    this(
        properties,
        retrieval,
        chatCompletion,
        ragAnswer,
        knowledgeAnswer,
        groundingGuard,
        Optional.empty());
  }

  @Autowired
  public RagQueryService(
      AiProviderProperties properties,
      KnowledgeRetriever retrieval,
      AiChatCompletion chatCompletion,
      @Qualifier("aiGroundedRagAnswer") Optional<RagAnswerPort> ragAnswer,
      Optional<KnowledgeAnswerService> knowledgeAnswer,
      Optional<GroundingGuard> groundingGuard,
      Optional<SpringAiChatProperties> chatProperties) {
    this.properties = properties;
    this.retrieval = retrieval;
    this.chatCompletion = chatCompletion;
    this.providerPolicy = providerPolicy(properties, chatProperties);
    this.ragAnswer = ragAnswer;
    this.knowledgeAnswer = knowledgeAnswer;
    this.groundingGuard = groundingGuard;
  }

  private static AiChatCompletion.ProviderPolicy providerPolicy(
      AiProviderProperties properties, Optional<SpringAiChatProperties> chatProperties) {
    List<String> admittedProviders =
        chatProperties
            .filter(configured -> !configured.providers().isEmpty())
            .map(
                configured ->
                    configured.providers().stream()
                        .map(SpringAiChatProperties.Provider::key)
                        .toList())
            .orElseGet(() -> List.of(properties.provider()));
    return new AiChatCompletion.ProviderPolicy(admittedProviders, true);
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
        var groundedAnswer =
            knowledgeAnswer
                .orElseThrow()
                .answer(
                    new KnowledgeQuery(question, DEFAULT_LOCALE, 5),
                    KnowledgeRoute.GENERAL,
                    executionContext);
        if (groundingGuard.isPresent()) {
          var decision =
              groundingGuard
                  .orElseThrow()
                  .check(
                      new GroundingRequest(
                          groundedAnswer.grounded(),
                          groundedAnswer.retrieval().topScore(),
                          groundedAnswer.retrieval().margin(),
                          groundedAnswer.sourceIds()),
                      executionContext);
          if (decision.action() != GuardrailAction.ALLOW) {
            return NO_RELEVANT_DOCUMENTS;
          }
        }
        return groundedAnswer.text();
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
      return chatCompletion
          .complete(
              new AiChatCompletion.Request(context, question, executionContext, providerPolicy))
          .content();
    } catch (RuntimeException failure) {
      SemanticFailurePolicy.rethrowSecurityFailure(failure);
      return "Retrieval unavailable.";
    }
  }

  private boolean isMock() {
    return properties.provider() == null || "mock".equalsIgnoreCase(properties.provider());
  }
}
