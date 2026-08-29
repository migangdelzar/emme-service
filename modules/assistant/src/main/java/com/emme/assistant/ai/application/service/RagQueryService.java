package com.emme.assistant.ai.application.service;

import com.emme.ai.contracts.model.AiModelProvider;
import com.emme.assistant.ai.api.usecase.RagQueryUseCase;
import com.emme.assistant.ai.configuration.AiProperties;
import com.emme.documents.api.query.SearchDocumentChunksQuery;
import com.emme.documents.api.result.DocumentChunkDetails;
import com.emme.documents.api.usecase.SearchDocumentChunksUseCase;
import com.emme.kernel.context.AiExecutionContextScope;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class RagQueryService implements RagQueryUseCase {

  private final AiProperties properties;
  private final AiModelProvider modelProvider;
  private final SearchDocumentChunksUseCase searchDocuments;

  public RagQueryService(
      AiProperties properties,
      AiModelProvider modelProvider,
      SearchDocumentChunksUseCase searchDocuments) {
    this.properties = properties;
    this.modelProvider = modelProvider;
    this.searchDocuments = searchDocuments;
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
    var queryVector = modelProvider.embed(question);
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
    return modelProvider.chat(context, question);
  }

  private boolean isMock() {
    return properties.provider() == null || "mock".equalsIgnoreCase(properties.provider());
  }
}
