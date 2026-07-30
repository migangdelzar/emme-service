package com.emme.shared;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.NoArgGenerator;
import java.util.UUID;

/**
 * Generates time-ordered UUIDv7 identifiers. UUIDv7 encodes a Unix timestamp in the first 48 bits,
 * making them sortable by creation time and index-friendly.
 */
public final class IdGenerator {

  private static final NoArgGenerator GENERATOR = Generators.timeBasedEpochGenerator();

  private IdGenerator() {
    throw new UnsupportedOperationException("Utility class");
  }

  public static UUID generate() {
    return GENERATOR.generate();
  }
}
