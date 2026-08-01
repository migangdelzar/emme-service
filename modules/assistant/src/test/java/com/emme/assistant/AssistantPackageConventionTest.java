package com.emme.assistant;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class AssistantPackageConventionTest {

  private static final Path ROOT = sourcePath("modules/assistant/src/main/java/com/emme/assistant");

  @Test
  void removesLegacyImplementationPackages() {
    assertThat(hasJavaSources(ROOT.resolve("entity"))).isFalse();
    assertThat(hasJavaSources(ROOT.resolve("web"))).isFalse();
    assertThat(hasDirectJavaSources(ROOT.resolve("application"))).isFalse();
    assertThat(Files.exists(ROOT.resolve("domain/model/Conversation.java"))).isTrue();
    assertThat(Files.exists(ROOT.resolve("adapter/in/web/controller/ConversationController.java")))
        .isTrue();
    assertThat(Files.exists(ROOT.resolve("adapter/out/persistence/entity/ConversationEntity.java")))
        .isTrue();
    assertThat(Files.exists(ROOT.resolve("api/usecase/StartConversationUseCase.java"))).isTrue();
  }

  @Test
  void exposesAiAsASeparateCapabilityBoundary() {
    assertThat(Files.exists(ROOT.resolve("ai/api/usecase/ChatUseCase.java"))).isTrue();
    assertThat(Files.exists(ROOT.resolve("ai/application/service/ChatService.java"))).isTrue();
    assertThat(Files.exists(ROOT.resolve("ai/adapter/in/web/controller/AiController.java")))
        .isTrue();
  }

  @Test
  void keepsApplicationFreeOfPersistenceAndAdapterDependencies() throws Exception {
    Path application = ROOT.resolve("application");
    assertThat(Files.exists(application.resolve("port/out/ConversationRepository.java"))).isTrue();
    assertThat(Files.exists(application.resolve("service/StartConversationService.java"))).isTrue();
    assertThat(Files.exists(application.resolve("service/CloseConversationService.java"))).isTrue();
    try (Stream<Path> paths = Files.walk(application)) {
      for (Path path : paths.filter(candidate -> candidate.toString().endsWith(".java")).toList()) {
        assertThat(Files.readString(path))
            .as("application source: %s", path)
            .doesNotContain("com.emme.assistant.adapter.out.persistence")
            .doesNotContain("jakarta.persistence")
            .doesNotContain("org.springframework.data");
      }
    }
  }

  private static boolean hasJavaSources(Path directory) {
    if (!Files.isDirectory(directory)) {
      return false;
    }
    try (Stream<Path> paths = Files.walk(directory)) {
      return paths.anyMatch(path -> path.toString().endsWith(".java"));
    } catch (Exception exception) {
      throw new IllegalStateException("Cannot inspect " + directory, exception);
    }
  }

  private static boolean hasDirectJavaSources(Path directory) {
    if (!Files.isDirectory(directory)) {
      return false;
    }
    try (Stream<Path> paths = Files.list(directory)) {
      return paths.anyMatch(path -> path.toString().endsWith(".java"));
    } catch (Exception exception) {
      throw new IllegalStateException("Cannot inspect " + directory, exception);
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
