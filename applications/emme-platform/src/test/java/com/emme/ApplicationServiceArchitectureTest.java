package com.emme;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** Repository-wide checks for application-service cohesion and current project ownership. */
class ApplicationServiceArchitectureTest {

  private static final Set<String> NON_TRANSACTIONAL_SERVICES =
      Set.of(
          "AuthenticateUserService",
          "CaptionImageService",
          "ChatService",
          "DetectIntentService",
          "EmbedTextService",
          "RagQueryService",
          "StartGoogleOAuthService");

  private static final Pattern SERVICE_DECLARATION =
      Pattern.compile(
          "\\bclass\\s+(\\w+Service)\\b[^\\{]*?implements\\s+([^\\{]+)", Pattern.DOTALL);
  private static final Pattern USE_CASE = Pattern.compile("\\b(\\w+UseCase)\\b");

  @Test
  void everyApplicationServiceImplementsExactlyOneMatchingUseCase() throws IOException {
    Path modules = sourcePath("modules");
    try (Stream<Path> files = Files.walk(modules)) {
      files
          .filter(path -> path.toString().contains("src/main/java"))
          .filter(path -> path.toString().contains("/application/service/"))
          .filter(path -> path.getFileName().toString().endsWith("Service.java"))
          .forEach(this::assertServiceDeclaration);
    }
  }

  @Test
  void everyApplicationServiceDeclaresItsTransactionPolicy() throws IOException {
    Path modules = sourcePath("modules");
    try (Stream<Path> files = Files.walk(modules)) {
      files
          .filter(path -> path.toString().contains("src/main/java"))
          .filter(path -> path.toString().contains("/application/service/"))
          .filter(path -> path.getFileName().toString().endsWith("Service.java"))
          .forEach(this::assertTransactionPolicy);
    }
  }

  @Test
  void removedStudioApiProjectIsAbsentFromTheActiveBuild() throws IOException {
    assertThat(Files.readString(sourcePath("settings.gradle.kts"))).doesNotContain("studio-api");
    assertThat(Files.readString(sourcePath("applications/emme-platform/build.gradle.kts")))
        .doesNotContain("studio-api");
    assertThat(Files.exists(sourcePath("applications").resolve("studio-api"))).isFalse();
  }

  private void assertServiceDeclaration(Path sourcePath) {
    String source = read(sourcePath);
    Matcher declaration = SERVICE_DECLARATION.matcher(source);
    assertThat(declaration.find()).as("service declaration in %s", sourcePath).isTrue();

    String serviceName = declaration.group(1);
    Set<String> useCases = new HashSet<>();
    Matcher useCase = USE_CASE.matcher(declaration.group(2));
    while (useCase.find()) {
      useCases.add(useCase.group(1));
    }

    assertThat(useCases)
        .as("one use case implemented by %s", sourcePath)
        .containsExactly(
            serviceName.substring(0, serviceName.length() - "Service".length()) + "UseCase");
  }

  private void assertTransactionPolicy(Path sourcePath) {
    String source = read(sourcePath);
    String serviceName = sourcePath.getFileName().toString().replace(".java", "");
    if (NON_TRANSACTIONAL_SERVICES.contains(serviceName)) {
      assertThat(source)
          .as("documented non-transactional application service %s", sourcePath)
          .doesNotContain("@Transactional");
      return;
    }
    assertThat(source)
        .as("transaction policy for application service %s", sourcePath)
        .contains("@Transactional");
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
