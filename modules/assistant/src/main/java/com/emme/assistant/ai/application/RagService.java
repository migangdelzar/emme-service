package com.emme.assistant.ai.application;

import com.emme.assistant.ai.config.AiProperties;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class RagService {

  private final AiProperties properties;

  public RagService(AiProperties properties) {
    this.properties = properties;
  }

  /** RAG query — mock returns canned answer, real embeds + queries pgvector. */
  public String query(UUID tenantId, String question) {
    if (isMock()) {
      return "MOCK RAG: Based on your documents, the answer to your question about '"
          + question
          + "' is that you should contact the salon for specific details.";
    }
    // TODO: embed question, query pgvector, return top chunks
    return "[real] RAG response placeholder for tenant " + tenantId;
  }

  private boolean isMock() {
    return properties.provider() == null || "mock".equalsIgnoreCase(properties.provider());
  }
}
