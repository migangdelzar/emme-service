package com.emme.ai.contracts.semantic;

import com.emme.ai.contracts.context.AiExecutionContext;
import java.util.Optional;

/** Port for tenant- and audience-scoped semantic response caching. */
public interface SemanticCache {

  Optional<SemanticCacheEntry> find(SemanticCacheQuery query, AiExecutionContext context);

  void put(SemanticCacheEntry entry, AiExecutionContext context);
}
