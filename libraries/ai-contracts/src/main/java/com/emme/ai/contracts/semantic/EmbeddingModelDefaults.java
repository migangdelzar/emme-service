package com.emme.ai.contracts.semantic;

/** Canonical local text-embedding identity shared by providers and semantic indexes. */
public final class EmbeddingModelDefaults {

  public static final String MODEL_NAME = "embeddinggemma:300m";
  public static final String MODEL_VERSION = "ollama-embeddinggemma:300m";
  public static final int DIMENSION = 768;

  private EmbeddingModelDefaults() {
    throw new UnsupportedOperationException("Utility class");
  }
}
