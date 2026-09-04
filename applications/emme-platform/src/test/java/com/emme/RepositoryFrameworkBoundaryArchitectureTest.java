package com.emme;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** Guards framework ownership before the repository-wide migration begins. */
class RepositoryFrameworkBoundaryArchitectureTest {

  private static final List<String> FORBIDDEN_AI_CONTRACT_IMPORTS =
      List.of(
          "org.springframework",
          "org.hibernate",
          "org.springframework.ai",
          "org.springframework.data",
          "org.springframework.jdbc",
          "org.springframework.kafka",
          "org.springframework.data.redis",
          "org.bsc.langgraph4j",
          "redis.clients",
          "okhttp3",
          "com.stripe",
          "software.amazon.awssdk");

  @Test
  void aiContractsRemainFrameworkAndProviderFree() throws IOException {
    Path contracts = sourcePath("libraries/ai-contracts/src/main/java");

    try (Stream<Path> files = Files.walk(contracts)) {
      files
          .filter(path -> path.toString().endsWith(".java"))
          .forEach(
              path -> {
                String source = read(path);
                FORBIDDEN_AI_CONTRACT_IMPORTS.forEach(
                    forbidden ->
                        assertThat(source)
                            .as("AI contract %s must not import %s", path, forbidden)
                            .doesNotContain(forbidden));
              });
    }
  }

  @Test
  void frameworkSpecificAiTypesStayAtAdapterOrConfigurationEdges() throws IOException {
    Path aiPlatform = sourcePath("modules/ai-platform/src/main/java/com/emme/ai/platform");
    Path assistantAi = sourcePath("modules/assistant/src/main/java/com/emme/assistant/ai");

    assertNoFrameworkTypeOutsideOwnedEdges(aiPlatform, "adapter", "configuration", "learning");
    assertNoFrameworkTypeOutsideOwnedEdges(assistantAi, "adapter", "configuration");
  }

  private static void assertNoFrameworkTypeOutsideOwnedEdges(Path root, String... allowedAreas)
      throws IOException {
    try (Stream<Path> files = Files.walk(root)) {
      files
          .filter(path -> path.toString().endsWith(".java"))
          .filter(path -> !isInAllowedArea(root, path, allowedAreas))
          .forEach(
              path -> {
                String source = read(path);
                assertThat(source)
                    .as(
                        "framework-specific AI types must stay at adapter/configuration edges: %s",
                        path)
                    .doesNotContain("org.springframework.ai")
                    .doesNotContain("org.bsc.langgraph4j")
                    .doesNotContain("org.springframework.jdbc")
                    .doesNotContain("redis.clients");
              });
    }
  }

  private static boolean isInAllowedArea(Path root, Path file, String... allowedAreas) {
    Path relative = root.relativize(file);
    for (String allowedArea : allowedAreas) {
      if (relative.startsWith(allowedArea)) {
        return true;
      }
    }
    return false;
  }

  private static String read(Path path) {
    try {
      return Files.readString(path);
    } catch (IOException exception) {
      throw new IllegalStateException("Cannot read " + path, exception);
    }
  }

  private static Path sourcePath(String relativePath) {
    Path current = Path.of("").toAbsolutePath();
    while (current != null) {
      Path candidate = current.resolve(relativePath);
      if (Files.exists(candidate)) {
        return candidate;
      }
      current = current.getParent();
    }
    throw new IllegalStateException("Cannot locate source path: " + relativePath);
  }
}
