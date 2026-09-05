package com.emme.assistant.ai.application.rag;

import com.emme.ai.contracts.rag.RetrievedDocument;
import java.util.List;

/** Provider-neutral boundary for accepting or rejecting retrieved knowledge context. */
public interface RetrievalQualityGate {

  RetrievalQualityDecision evaluate(
      KnowledgeRoute route,
      String query,
      List<RetrievedDocument> documents,
      RetrievalQualityPolicy policy);
}
