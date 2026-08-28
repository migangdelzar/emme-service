package com.emme.assistant.ai.application.port.out;

import com.emme.assistant.ai.application.service.EmbeddingVector;
import com.emme.assistant.ai.application.service.SemanticMatch;
import java.util.List;
import java.util.Set;

/** Tenant-scoped semantic search for backend-controlled intent and tool references. */
public interface SemanticReferenceSearchPort {

  /** Returns ranked intent candidates for the query embedding. */
  List<SemanticMatch> searchIntents(String locale, EmbeddingVector query, int limit);

  /** Returns ranked tool candidates restricted to the backend-authorized tool keys. */
  List<SemanticMatch> searchTools(
      String locale, EmbeddingVector query, Set<String> authorizedToolKeys, int limit);
}
