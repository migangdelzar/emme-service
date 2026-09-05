package com.emme.assistant.ai.application.port.out;

import com.emme.ai.contracts.embedding.EmbeddingService;

/**
 * Compatibility port retained while assistant semantic values migrate to the shared embedding
 * contract.
 *
 * @deprecated migrate callers to {@code com.emme.ai.contracts.embedding.EmbeddingService}
 */
@Deprecated
public interface EmbeddingModelPort extends EmbeddingService {}
