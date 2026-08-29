package com.emme.ai.contracts.graph;

import com.emme.kernel.context.AiExecutionContext;
import java.util.List;

/** Port for curated, tenant-scoped graph retrieval used only for recommendations/explanations. */
public interface KnowledgeGraphRetriever {

  List<GraphRecommendation> retrieve(GraphTraversalQuery query, AiExecutionContext context);
}
