package com.emme.ai.contracts.model;

import com.emme.ai.contracts.embedding.EmbeddingService;

/**
 * Provider-mechanics compatibility name retained for existing Spring AI adapter wiring.
 *
 * @deprecated use {@link EmbeddingService} for the cross-module embedding capability
 */
@Deprecated
public interface EmbeddingModel extends EmbeddingService {}
