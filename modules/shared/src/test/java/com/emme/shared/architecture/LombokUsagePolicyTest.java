package com.emme.shared.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class LombokUsagePolicyTest {

  private static final Set<String> APPROVED_FILES =
      Set.of(
          "modules/assistant/src/main/java/com/emme/assistant/adapter/in/web/controller/ConversationController.java",
          "modules/assistant/src/main/java/com/emme/assistant/application/service/AddConversationEventService.java",
          "modules/assistant/src/main/java/com/emme/assistant/application/service/CloseConversationService.java",
          "modules/assistant/src/main/java/com/emme/assistant/application/service/ConfirmPendingActionService.java",
          "modules/assistant/src/main/java/com/emme/assistant/application/service/GetActiveActionsService.java",
          "modules/assistant/src/main/java/com/emme/assistant/application/service/GetConversationHistoryService.java",
          "modules/assistant/src/main/java/com/emme/assistant/application/service/GetConversationService.java",
          "modules/assistant/src/main/java/com/emme/assistant/application/service/ListConversationsService.java",
          "modules/assistant/src/main/java/com/emme/assistant/application/service/ProposePendingActionService.java",
          "modules/assistant/src/main/java/com/emme/assistant/application/service/RejectPendingActionService.java",
          "modules/assistant/src/main/java/com/emme/assistant/application/service/StartConversationService.java",
          "modules/appointments/src/main/java/com/emme/appointments/application/service/ListAppointmentsService.java",
          "modules/appointments/src/main/java/com/emme/appointments/application/service/FindAvailableSlotsService.java",
          "modules/catalog/src/main/java/com/emme/catalog/application/service/MatchCatalogItemsService.java",
          "modules/catalog/src/main/java/com/emme/catalog/application/service/AddCatalogItemImageService.java",
          "modules/catalog/src/main/java/com/emme/catalog/application/service/ListCatalogItemsService.java");

  @Test
  void onlyUsesLombokInApprovedProductionFiles() throws IOException {
    Path root = sourcePath();

    Set<String> lombokFiles =
        productionSources(root).stream()
            .filter(this::containsLombok)
            .map(path -> root.relativize(path).toString().replace('\\', '/'))
            .collect(Collectors.toSet());

    assertThat(lombokFiles).containsExactlyInAnyOrderElementsOf(APPROVED_FILES);
  }

  @Test
  void forbidsBroadLombokAnnotationsAndBoundaryUsage() throws IOException {
    Path root = sourcePath();

    for (Path source : productionSources(root)) {
      String normalized = source.toString().replace('\\', '/');
      String contents = Files.readString(source);

      if (normalized.contains("/domain/") || normalized.contains("/persistence/entity/")) {
        assertThat(contents)
            .as("forbidden Lombok source: %s", source)
            .doesNotContain("import lombok.");
      }
      assertThat(contents)
          .as("forbidden Lombok annotation: %s", source)
          .doesNotContainPattern("@Data\\b")
          .doesNotContainPattern("@Setter\\b")
          .doesNotContainPattern("@EqualsAndHashCode\\b")
          .doesNotContainPattern("@ToString\\b");

      if (contents.contains("@Builder")) {
        String relativePath = root.relativize(source).toString().replace('\\', '/');
        assertThat(APPROVED_FILES).contains(relativePath);
        assertThat(contents).contains("setterPrefix = \"with\"");
      }
    }
  }

  @Test
  void adoptsRequiredArgsConstructorsForApprovedApplicationServices() throws IOException {
    Path root = sourcePath();

    for (String relativePath : APPROVED_FILES) {
      assertThat(Files.readString(root.resolve(relativePath)))
          .as("Lombok pilot source: %s", relativePath)
          .contains("import lombok.RequiredArgsConstructor;")
          .contains("@RequiredArgsConstructor");
    }
  }

  private static List<Path> productionSources(Path root) throws IOException {
    try (Stream<Path> modules = Files.walk(root.resolve("modules"));
        Stream<Path> libraries = Files.walk(root.resolve("libraries"))) {
      return Stream.concat(modules, libraries)
          .filter(path -> path.toString().replace('\\', '/').contains("/src/main/java/"))
          .filter(path -> path.toString().endsWith(".java"))
          .toList();
    }
  }

  private boolean containsLombok(Path source) {
    try {
      String contents = Files.readString(source);
      return contents.contains("import lombok.") || contents.contains("lombok.");
    } catch (IOException exception) {
      throw new IllegalStateException("Cannot read source: " + source, exception);
    }
  }

  private static Path sourcePath() {
    Path current = Path.of("").toAbsolutePath();
    while (current != null) {
      if (Files.isDirectory(current.resolve("modules"))
          && Files.isDirectory(current.resolve("libraries"))) {
        return current;
      }
      current = current.getParent();
    }
    throw new IllegalStateException("Cannot locate repository root");
  }
}
