package com.emme.ai.contracts.embedding;

import java.util.List;

/** Generates an embedding vector through the configured AI capability. */
public interface EmbedTextUseCase {

  List<Float> embed(String text);
}
