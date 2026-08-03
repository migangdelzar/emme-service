package com.emme.assistant.ai.api.usecase;

import java.util.List;

/** Generates an embedding vector through the configured AI capability. */
public interface EmbedTextUseCase {

  List<Float> embed(String text);
}
