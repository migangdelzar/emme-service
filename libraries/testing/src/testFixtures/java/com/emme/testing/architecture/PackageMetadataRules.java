package com.emme.testing.architecture;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

/** Reusable package metadata checks for production architecture suites. */
public final class PackageMetadataRules {

  private PackageMetadataRules() {}

  /** Returns materialized production packages that do not contain package-info metadata. */
  public static Set<String> packagesMissingMetadata(Path... sourceRoots) {
    Set<Path> materializedPackages = new TreeSet<>();
    for (Path sourceRoot : sourceRoots) {
      try (Stream<Path> files = Files.walk(sourceRoot)) {
        files
            .filter(PackageMetadataRules::isProductionJavaSource)
            .map(Path::getParent)
            .filter(
                packageDirectory ->
                    !Files.isRegularFile(packageDirectory.resolve("package-info.java")))
            .forEach(materializedPackages::add);
      } catch (IOException exception) {
        throw new UncheckedIOException(
            "Cannot inspect package metadata under " + sourceRoot, exception);
      }
    }
    return materializedPackages.stream()
        .map(Path::toString)
        .collect(java.util.stream.Collectors.toUnmodifiableSet());
  }

  private static boolean isProductionJavaSource(Path path) {
    return Files.isRegularFile(path)
        && path.toString().contains("src/main/java")
        && !path.toString().contains("/build/")
        && path.toString().endsWith(".java")
        && !path.getFileName().toString().equals("package-info.java");
  }
}
