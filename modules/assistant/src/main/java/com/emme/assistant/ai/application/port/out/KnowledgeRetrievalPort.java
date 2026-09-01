package com.emme.assistant.ai.application.port.out;

import java.util.List;

/** Framework-neutral tenant-scoped retrieval boundary for answerable knowledge. */
public interface KnowledgeRetrievalPort {

  List<KnowledgeDocument> retrieve(String question, int limit);
}
