package com.emme.testing.architecture;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/** Source-level checks for Spring Modulith named-interface declarations. */
public final class NamedInterfaceRules {

  private static final Pattern NAMED_INTERFACE =
      Pattern.compile(
          "@(?:org\\.springframework\\.modulith\\.)?NamedInterface\\(\\\"([^\\\"]+)\\\"\\)");
  private static final Pattern API_INTERFACE_NAME =
      Pattern.compile("[a-z0-9]+(?:-[a-z0-9]+)*-(?:api|events|types|results|usecases)");

  private NamedInterfaceRules() {}

  /** Returns violations for named interfaces under a production source root. */
  public static List<String> violations(Path sourceRoot) {
    List<String> violations = new ArrayList<>();
    try (Stream<Path> files = Files.walk(sourceRoot)) {
      files
          .filter(NamedInterfaceRules::isPackageMetadata)
          .forEach(path -> inspect(path, violations));
    } catch (IOException exception) {
      throw new UncheckedIOException(
          "Cannot inspect named interfaces under " + sourceRoot, exception);
    }
    return List.copyOf(violations);
  }

  private static void inspect(Path packageInfo, List<String> violations) {
    String source = read(packageInfo);
    Matcher matcher = NAMED_INTERFACE.matcher(source);
    while (matcher.find()) {
      String interfaceName = matcher.group(1);
      Path packageDirectory = packageInfo.getParent();
      if (!hasProductionType(packageDirectory)) {
        violations.add(packageInfo + " declares an empty named interface " + interfaceName);
      }
      if (isApiPackage(packageDirectory) && !API_INTERFACE_NAME.matcher(interfaceName).matches()) {
        violations.add(
            packageInfo
                + " uses non-canonical API named interface name "
                + interfaceName
                + "; expected <capability>-api, -events, -types, -results, or -usecases");
      }
    }
  }

  private static boolean hasProductionType(Path packageDirectory) {
    try (Stream<Path> files = Files.walk(packageDirectory)) {
      return files.anyMatch(
          path ->
              Files.isRegularFile(path)
                  && path.toString().endsWith(".java")
                  && !path.getFileName().toString().equals("package-info.java"));
    } catch (IOException exception) {
      throw new UncheckedIOException("Cannot inspect package " + packageDirectory, exception);
    }
  }

  private static boolean isPackageMetadata(Path path) {
    return Files.isRegularFile(path)
        && path.getFileName().toString().equals("package-info.java")
        && path.toString().contains("/src/main/java/");
  }

  private static boolean isApiPackage(Path packageDirectory) {
    String normalized = packageDirectory.toString().replace('\\', '/');
    return normalized.matches(".*(?:/|^)api(?:/.*)?$");
  }

  private static String read(Path path) {
    try {
      return Files.readString(path);
    } catch (IOException exception) {
      throw new UncheckedIOException("Cannot read " + path, exception);
    }
  }
}
