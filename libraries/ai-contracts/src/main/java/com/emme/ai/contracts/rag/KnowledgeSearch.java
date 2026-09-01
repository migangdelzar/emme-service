package com.emme.ai.contracts.rag;

import com.emme.kernel.context.AiExecutionContext;
import java.util.List;

public interface KnowledgeSearch {

  List<RetrievedDocument> search(KnowledgeQuery query, AiExecutionContext context);
}
