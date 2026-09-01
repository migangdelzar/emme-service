package com.emme.assistant.ai.application.service;

import com.emme.ai.contracts.model.AiModelProvider;
import com.emme.ai.contracts.model.ModelCapability;
import com.emme.ai.contracts.model.ModelExecutionScheduler;
import com.emme.assistant.ai.api.usecase.RagQueryUseCase;
import com.emme.assistant.ai.application.port.out.ChatCompletionPort;
import com.emme.assistant.ai.application.port.out.ChatProviderUnavailableException;
import com.emme.assistant.ai.application.port.out.EmbeddingModelPort;
import com.emme.assistant.ai.application.port.out.RagAnswerPort;
import com.emme.assistant.ai.application.semantic.SemanticFailurePolicy;
import com.emme.assistant.ai.configuration.AiExecutorProperties;
import com.emme.assistant.ai.configuration.AiProperties;
import com.emme.documents.api.query.SearchDocumentChunksQuery;
import com.emme.documents.api.result.DocumentChunkDetails;
import com.emme.documents.api.usecase.SearchDocumentChunksUseCase;
import com.emme.kernel.context.AiExecutionContextScope;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RagQueryService implements RagQueryUseCase {

  private final AiProperties properties;
  private final AiModelProvider modelProvider;
  private final SearchDocumentChunksUseCase searchDocuments;
  private final Optional<EmbeddingModelPort> embeddingModel;
  private final Optional<ChatCompletionPort> chatCompletion;
  private final Optional<RagAnswerPort> ragAnswer;
  private final Optional<ModelExecutionScheduler> modelExecutionScheduler;
  private final Duration admissionTimeout;

  public RagQueryService(
      AiProperties properties,
      AiModelProvider modelProvider,
      SearchDocumentChunksUseCase searchDocuments) {
    this(
        properties,
        modelProvider,
        searchDocuments,
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Duration.ofSeconds(5));
  }

  @Autowired
  public RagQueryService(
      AiProperties properties,
      AiModelProvider modelProvider,
      SearchDocumentChunksUseCase searchDocuments,
      Optional<EmbeddingModelPort> embeddingModel,
      Optional<ChatCompletionPort> chatCompletion,
      Optional<RagAnswerPort> ragAnswer,
      Optional<ModelExecutionScheduler> modelExecutionScheduler,
      AiExecutorProperties executorProperties) {
    this(
        properties,
        modelProvider,
        searchDocuments,
        embeddingModel,
        chatCompletion,
        ragAnswer,
        modelExecutionScheduler,
        executorProperties.modelAdmissionTimeout());
  }

  public RagQueryService(
      AiProperties properties,
      AiModelProvider modelProvider,
      SearchDocumentChunksUseCase searchDocuments,
      Optional<EmbeddingModelPort> embeddingModel,
      Optional<ChatCompletionPort> chatCompletion) {
    this(
        properties,
        modelProvider,
        searchDocuments,
        embeddingModel,
        chatCompletion,
        Optional.empty(),
        Optional.empty(),
        Duration.ofSeconds(5));
  }

  public RagQueryService(
      AiProperties properties,
      AiModelProvider modelProvider,
      SearchDocumentChunksUseCase searchDocuments,
      Optional<EmbeddingModelPort> embeddingModel,
      Optional<ChatCompletionPort> chatCompletion,
      Optional<RagAnswerPort> ragAnswer) {
    this(
        properties,
        modelProvider,
        searchDocuments,
        embeddingModel,
        chatCompletion,
        ragAnswer,
        Optional.empty(),
        Duration.ofSeconds(5));
  }

  private RagQueryService(
      AiProperties properties,
      AiModelProvider modelProvider,
      SearchDocumentChunksUseCase searchDocuments,
      Optional<EmbeddingModelPort> embeddingModel,
      Optional<ChatCompletionPort> chatCompletion,
      Optional<RagAnswerPort> ragAnswer,
      Optional<ModelExecutionScheduler> modelExecutionScheduler,
      Duration admissionTimeout) {
    this.properties = properties;
    this.modelProvider = modelProvider;
    this.searchDocuments = searchDocuments;
    this.embeddingModel = embeddingModel;
    this.chatCompletion = chatCompletion;
    this.ragAnswer = ragAnswer;
    this.modelExecutionScheduler = modelExecutionScheduler;
    this.admissionTimeout = admissionTimeout;
  }

  /** RAG query — mock returns canned answer, real embeds + queries pgvector. */
  @Override
  public String query(String question) {
    var executionContext = AiExecutionContextScope.requireCurrent();
    var tenantId = executionContext.tenantId();
    if (isMock()) {
      return "MOCK RAG: Based on your documents, the answer to your question about '"
          + question
          + "' is that you should contact the salon for specific details.";
    }
    try {
      if (ragAnswer.isPresent()) {
        return ragAnswer.orElseThrow().answer(question);
      }
    } catch (ChatProviderUnavailableException unavailable) {
      // Preserve the existing provider-neutral retrieval path as the compatibility fallback.
    }
    try {
      var queryVector = embed(question);
      var chunks =
          searchDocuments.search(new SearchDocumentChunksQuery(tenantId, queryVector, question, 5));
      String context =
          chunks.stream()
              .map(DocumentChunkDetails::content)
              .filter(content -> content != null && !content.isBlank())
              .collect(Collectors.joining("\n\n"));
      if (context.isBlank()) {
        return "No relevant documents were found.";
      }
      return complete(context, question);
    } catch (RuntimeException failure) {
      SemanticFailurePolicy.rethrowSecurityFailure(failure);
      return complete("", question);
    }
  }

  private List<Float> embed(String question) {
    return embeddingModel
        .map(model -> model.embed(question).values())
        .orElseGet(
            () -> executeLegacy(ModelCapability.EMBEDDING, () -> modelProvider.embed(question)));
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
