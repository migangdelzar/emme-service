package com.emme.ai.contracts.rag;

import com.emme.kernel.context.AiExecutionContext;
import java.util.List;

/** Retrieves tenant-authorized reference knowledge without generating an answer. */
@FunctionalInterface
public interface KnowledgeRetriever {

  List<RetrievedDocument> search(KnowledgeQuery query, AiExecutionContext context);
}
