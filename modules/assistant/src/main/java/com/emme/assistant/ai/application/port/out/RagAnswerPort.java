package com.emme.assistant.ai.application.port.out;

import com.emme.ai.contracts.rag.KnowledgeQuery;
import com.emme.ai.contracts.rag.RetrievedDocument;
import com.emme.kernel.context.AiExecutionContext;
import java.util.List;

/** Provider-neutral boundary for an answer generated with tenant-scoped retrieval augmentation. */
@FunctionalInterface
public interface RagAnswerPort {

  String answer(String question);

  /** Generates an answer from the exact documents accepted by the application quality gate. */
  default String answer(
      KnowledgeQuery query, List<RetrievedDocument> documents, AiExecutionContext context) {
    throw new UnsupportedOperationException("grounded retrieval answer is not configured");
  }
}
