package com.emme.assistant.ai.adapter.out.graph;

import com.emme.ai.contracts.graph.GraphProjection;
import com.emme.ai.contracts.graph.GraphRecommendation;
import com.emme.ai.contracts.graph.GraphTraversalQuery;
import com.emme.ai.contracts.graph.KnowledgeGraphProjector;
import com.emme.ai.contracts.graph.KnowledgeGraphRetriever;
import com.emme.assistant.ai.configuration.SpringAiAgeProperties;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import java.util.List;
import java.util.Objects;

/** Tenant-bound facade over the optional AGE graph client. */
public final class AgeGraphAdapter implements KnowledgeGraphProjector, KnowledgeGraphRetriever {

  private final AgeGraphClient client;
  private final SpringAiAgeProperties properties;

  public AgeGraphAdapter(AgeGraphClient client, SpringAiAgeProperties properties) {
    this.client = Objects.requireNonNull(client, "client must not be null");
    this.properties = Objects.requireNonNull(properties, "properties must not be null");
  }

  @Override
  public void project(GraphProjection projection, AiExecutionContext context) {
    Objects.requireNonNull(projection, "projection must not be null");
    AiExecutionContext bound = requireBoundContext(context);
    if (!client.available()) {
      throw new AgeGraphUnavailableException();
    }
    client.project(graphName(bound), bound.tenantId(), projection, java.time.Instant.now());
  }

  @Override
  public List<GraphRecommendation> retrieve(GraphTraversalQuery query, AiExecutionContext context) {
    Objects.requireNonNull(query, "query must not be null");
    AiExecutionContext bound = requireBoundContext(context);
    if (!client.available()) {
      return List.of();
    }
    return client.retrieve(graphName(bound), bound.tenantId(), boundedQuery(query));
  }

  private AiExecutionContext requireBoundContext(AiExecutionContext context) {
    AiExecutionContext bound = AiExecutionContextScope.requireCurrent();
    if (!bound.equals(context)) {
      throw new IllegalArgumentException("graph context must match the bound backend context");
    }
    return bound;
  }

  private String graphName(AiExecutionContext context) {
    return properties.graphPrefix() + context.tenantId().toString().replace("-", "");
  }

  private GraphTraversalQuery boundedQuery(GraphTraversalQuery query) {
    return query.limit() <= properties.retrievalLimit()
        ? query
        : new GraphTraversalQuery(query.kind(), query.sourceId(), properties.retrievalLimit());
  }
}
