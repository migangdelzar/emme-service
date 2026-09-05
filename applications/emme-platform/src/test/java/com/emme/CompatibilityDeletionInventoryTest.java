package com.emme;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** Prevents compatibility implementations from being deleted before their callers are migrated. */
class CompatibilityDeletionInventoryTest {

  private static final Pattern CANDIDATE_ROW =
      Pattern.compile("\\| `([^`]+)` \\| (Pending|Ready|Deleted) \\|.*");
  private static final String LEDGER =
      "docs/superpowers/migrations/framework-first-migration-ledger.md";

  @Test
  void everyDeletionCandidateHasAnExplicitStatusAndExistingPath() throws IOException {
    List<Candidate> candidates = candidates();

    assertThat(candidates).isNotEmpty();
    assertThat(candidates)
        .allSatisfy(
            candidate -> {
              assertThat(candidate.path()).isNotBlank();
              assertThat(Files.exists(repositoryPath(candidate.path())))
                  .as("pending or ready candidate must still exist: %s", candidate.path())
                  .isEqualTo(!candidate.status().equals("Deleted"));
            });
  }

  @Test
  void readyOrDeletedCandidatesHaveNoRemainingRepositoryReferences() throws IOException {
    for (Candidate candidate : candidates()) {
      if (candidate.status().equals("Pending")) {
        continue;
      }

      Path implementation = repositoryPath(candidate.path());
      String symbol = implementation.getFileName().toString().replace(".java", "");
      List<Path> references = repositoryReferences(symbol, implementation);

      assertThat(references)
          .as("%s candidate %s still has repository references", candidate.status(), symbol)
          .isEmpty();
    }
  }

  private static List<Candidate> candidates() throws IOException {
    List<Candidate> candidates = new ArrayList<>();
    for (String line : Files.readAllLines(sourcePath(LEDGER))) {
      Matcher matcher = CANDIDATE_ROW.matcher(line);
      if (matcher.matches()) {
        candidates.add(new Candidate(matcher.group(1), matcher.group(2)));
      }
    }
    return candidates;
  }

  private static Path repositoryPath(String relativePath) {
    return repositoryRoot().resolve(relativePath);
  }

  private static Path repositoryRoot() {
    return sourcePath(LEDGER).getParent().getParent().getParent().getParent();
  }

  private static List<Path> repositoryReferences(String symbol, Path implementation)
      throws IOException {
    Path repository = repositoryRoot();
    String relativeImplementation = repository.relativize(implementation).toString();
    String sourceMarker = "/src/main/java/";
    int sourceMarkerIndex = relativeImplementation.indexOf(sourceMarker);
    String packageName =
        relativeImplementation
            .substring(
                sourceMarkerIndex + sourceMarker.length(), relativeImplementation.lastIndexOf('/'))
            .replace('/', '.')
            .replace('\\', '.');
    String qualifiedName = packageName + "." + symbol;
    Path packageDirectory = implementation.getParent();

    try (Stream<Path> files = Files.walk(repository)) {
      return files
          .filter(Files::isRegularFile)
          .filter(path -> !path.startsWith(repository.resolve(".git")))
          .filter(path -> !path.startsWith(repository.resolve("build")))
          .filter(path -> !path.toString().contains("/build/"))
          .filter(path -> !path.startsWith(repository.resolve("docs")))
          .filter(path -> isSourceOrBuildFile(path))
          .filter(path -> !path.equals(implementation))
          .filter(
              path ->
                  !path.equals(
                      repository.resolve(
                          "applications/emme-platform/src/test/java/com/emme/CompatibilityDeletionInventoryTest.java")))
          .filter(
              path ->
                  contains(path, qualifiedName)
                      || (path.startsWith(packageDirectory) && contains(path, symbol)))
          .toList();
    }
  }

  private static boolean isSourceOrBuildFile(Path path) {
    String name = path.getFileName().toString();
    return name.endsWith(".java")
        || name.endsWith(".kt")
        || name.endsWith(".kts")
        || name.endsWith(".gradle")
        || name.endsWith(".xml")
        || name.endsWith(".yaml")
        || name.endsWith(".yml")
        || name.endsWith(".properties")
        || name.endsWith(".json");
  }

  private static boolean contains(Path path, String symbol) {
    try {
      return Files.readString(path).contains(symbol);
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

  private record Candidate(String path, String status) {}
}
