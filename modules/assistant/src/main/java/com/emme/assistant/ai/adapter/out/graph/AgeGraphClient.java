package com.emme.assistant.ai.adapter.out.graph;

import com.emme.ai.contracts.graph.GraphProjection;
import com.emme.ai.contracts.graph.GraphRecommendation;
import com.emme.ai.contracts.graph.GraphTraversalQuery;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Infrastructure protocol for the PostgreSQL/AGE client. */
public interface AgeGraphClient {

  boolean available();

  void project(String graphName, UUID tenantId, GraphProjection projection, Instant projectedAt);

  List<GraphRecommendation> retrieve(String graphName, UUID tenantId, GraphTraversalQuery query);
}
