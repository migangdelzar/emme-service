package com.emme.assistant.ai.application.rag;

import com.emme.kernel.context.AiExecutionContext;
import java.util.List;

/** Provider-neutral boundary for bounded retrieval-query improvement. */
public interface QueryImprover {

  List<String> improve(
      String originalQuery,
      KnowledgeRoute route,
      RetrievalQualityDecision previous,
      AiExecutionContext context,
      QueryImprovementPolicy policy);
}
