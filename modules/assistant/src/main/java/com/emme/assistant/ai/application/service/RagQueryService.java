package com.emme.assistant.ai.application.service;

import com.emme.ai.contracts.model.AiModelProvider;
import com.emme.ai.contracts.model.ModelCapability;
import com.emme.ai.contracts.model.ModelExecutionScheduler;
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
import com.emme.assistant.ai.configuration.AiExecutorProperties;
import com.emme.kernel.context.AiExecutionContextScope;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RagQueryService implements RagQueryUseCase {

  private static final String DEFAULT_LOCALE = "es-MX";

  private final AiProviderProperties properties;
  private final AiModelProvider modelProvider;
  private final KnowledgeSearch retrieval;
  private final Optional<ChatCompletionPort> chatCompletion;
  private final Optional<RagAnswerPort> ragAnswer;
  private final Optional<ModelExecutionScheduler> modelExecutionScheduler;
  private final Duration admissionTimeout;

  public RagQueryService(
      AiProviderProperties properties, AiModelProvider modelProvider, KnowledgeSearch retrieval) {
    this(
        properties,
        modelProvider,
        retrieval,
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Duration.ofSeconds(5));
  }

  @Autowired
  public RagQueryService(
      AiProviderProperties properties,
      AiModelProvider modelProvider,
      KnowledgeSearch retrieval,
      Optional<ChatCompletionPort> chatCompletion,
      Optional<RagAnswerPort> ragAnswer,
      Optional<ModelExecutionScheduler> modelExecutionScheduler,
      AiExecutorProperties executorProperties) {
    this(
        properties,
        modelProvider,
        retrieval,
        chatCompletion,
        ragAnswer,
        modelExecutionScheduler,
        executorProperties.modelAdmissionTimeout());
  }

  public RagQueryService(
      AiProviderProperties properties,
      AiModelProvider modelProvider,
      KnowledgeSearch retrieval,
      Optional<ChatCompletionPort> chatCompletion) {
    this(
        properties,
        modelProvider,
        retrieval,
        chatCompletion,
        Optional.empty(),
        Optional.empty(),
        Duration.ofSeconds(5));
  }

  public RagQueryService(
      AiProviderProperties properties,
      AiModelProvider modelProvider,
      KnowledgeSearch retrieval,
      Optional<ChatCompletionPort> chatCompletion,
      Optional<RagAnswerPort> ragAnswer) {
    this(
        properties,
        modelProvider,
        retrieval,
        chatCompletion,
        ragAnswer,
        Optional.empty(),
        Duration.ofSeconds(5));
  }

  private RagQueryService(
      AiProviderProperties properties,
      AiModelProvider modelProvider,
      KnowledgeSearch retrieval,
      Optional<ChatCompletionPort> chatCompletion,
      Optional<RagAnswerPort> ragAnswer,
      Optional<ModelExecutionScheduler> modelExecutionScheduler,
      Duration admissionTimeout) {
    this.properties = properties;
    this.modelProvider = modelProvider;
    this.retrieval = retrieval;
    this.chatCompletion = chatCompletion;
    this.ragAnswer = ragAnswer;
    this.modelExecutionScheduler = modelExecutionScheduler;
    this.admissionTimeout = admissionTimeout;
  }

  @Override
  public String query(String question) {
    var executionContext = AiExecutionContextScope.requireCurrent();
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
      return complete(context, question);
    } catch (RuntimeException failure) {
      SemanticFailurePolicy.rethrowSecurityFailure(failure);
      return "Retrieval unavailable.";
    }
  }

  private String complete(String context, String question) {
    try {
      return chatCompletion
          .map(chat -> chat.complete(context, question))
          .orElseGet(
              () ->
                  executeLegacy(
                      ModelCapability.GENERATION, () -> modelProvider.chat(context, question)));
    } catch (ChatProviderUnavailableException unavailable) {
      return executeLegacy(ModelCapability.GENERATION, () -> modelProvider.chat(context, question));
    }
  }

  private <T> T executeLegacy(ModelCapability capability, java.util.function.Supplier<T> action) {
    if (modelExecutionScheduler.isEmpty()) {
      return action.get();
    }
    return modelExecutionScheduler
        .orElseThrow()
        .execute(
            capability, AiExecutionContextScope.requireCurrent(), admissionTimeout, action::get);
  }

  private boolean isMock() {
    return properties.provider() == null || "mock".equalsIgnoreCase(properties.provider());
  }
}
