package com.emme.ai.contracts.model;

import java.util.List;

/** Canonical provider-neutral embedding capability. */
public interface EmbeddingModel {

  /** Creates an embedding for the supplied text. */
  List<Float> embed(String text);
}
