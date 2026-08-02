package com.emme.assistant;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
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
    assertThat(hasJavaSources(ROOT.resolve("ai/adapter/out/client"))).isFalse();
    assertThat(hasJavaSources(ROOT.resolve("ai/config"))).isFalse();
  }

  @Test
  void consumesDocumentsOnlyThroughThePublicDocumentsContract() throws IOException {
    String build = Files.readString(sourcePath("modules/assistant/build.gradle.kts"));
    String metadata = Files.readString(ROOT.resolve("package-info.java"));
    String ragService =
        Files.readString(ROOT.resolve("ai/application/service/RagQueryService.java"));

    assertThat(build).contains("implementation(project(\":modules:studio\"))");
    assertThat(metadata).contains("studio :: documents-api");
    assertThat(ragService)
        .contains("com.emme.studio.documents.api")
        .doesNotContain("com.emme.studio.documents.adapter")
        .doesNotContain("com.emme.studio.documents.application")
        .doesNotContain("com.emme.studio.domain");
    assertThat(Arrays.stream(build.split("\\R")))
        .noneMatch(line -> line.contains("testImplementation(project(\":modules:studio\"))"));
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

  @Test
  void requiresTenantScopedLookupBoundariesForUserOwnedData() throws Exception {
    String conversationPort =
        Files.readString(ROOT.resolve("application/port/out/ConversationRepository.java"));
    String conversationAdapter =
        Files.readString(
            ROOT.resolve("adapter/out/persistence/adapter/ConversationPersistenceAdapter.java"));
    String eventPort =
        Files.readString(ROOT.resolve("application/port/out/ConversationEventRepository.java"));
    String eventAdapter =
        Files.readString(
            ROOT.resolve(
                "adapter/out/persistence/adapter/ConversationEventPersistenceAdapter.java"));
    String actionPort =
        Files.readString(ROOT.resolve("application/port/out/PendingActionRepository.java"));
    String actionAdapter =
        Files.readString(
            ROOT.resolve("adapter/out/persistence/adapter/PendingActionPersistenceAdapter.java"));
    String controller =
        Files.readString(ROOT.resolve("adapter/in/web/controller/ConversationController.java"));

    assertThat(conversationPort).contains("findByTenantIdAndId(");
    assertThat(conversationPort).doesNotContain("findById(UUID conversationId)");
    assertThat(conversationAdapter).contains("findByTenantIdAndId(");
    assertThat(conversationAdapter).doesNotContain("repository.findById(");
    assertThat(eventPort).contains("tenantId");
    assertThat(eventAdapter).contains("tenantId");
    assertThat(actionPort).contains("findByTenantIdAndId(");
    assertThat(actionPort).doesNotContain("findById(UUID actionId)");
    assertThat(actionAdapter).contains("findByTenantIdAndId(");
    assertThat(actionAdapter).doesNotContain("repository.findById(");
    assertThat(controller).contains("withCurrentTenant");
    assertThat(controller).contains("new GetConversationQuery(tenantId, id)");
    assertThat(controller).contains("new CloseConversationCommand(tenantId, id)");
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
      return paths.anyMatch(
          path ->
              path.toString().endsWith(".java")
                  && !path.getFileName().toString().equals("package-info.java"));
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
