package com.emme.ai.contracts.rag;

import com.emme.ai.contracts.context.AiExecutionContext;
import java.util.List;

/** Port for tenant-filtered retrieval of unstructured knowledge. */
public interface KnowledgeRetriever {

  List<RetrievedDocument> retrieve(KnowledgeQuery query, AiExecutionContext context);
}
