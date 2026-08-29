package com.emme.ai.contracts.graph;

import com.emme.kernel.context.AiExecutionContext;

/** Port for asynchronously projecting authoritative events into a disposable graph read model. */
public interface KnowledgeGraphProjector {

  void project(GraphProjection projection, AiExecutionContext context);
}
