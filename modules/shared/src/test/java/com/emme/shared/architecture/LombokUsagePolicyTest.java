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
          "modules/catalog/src/main/java/com/emme/catalog/application/service/ListCatalogItemsService.java",
          "modules/clients/src/main/java/com/emme/clients/application/service/CreateCustomerService.java",
          "modules/clients/src/main/java/com/emme/clients/application/service/GetCustomerService.java",
          "modules/clients/src/main/java/com/emme/clients/application/service/ListCustomersService.java",
          "modules/clients/src/main/java/com/emme/clients/application/service/ListTenantCustomersService.java",
          "modules/clients/src/main/java/com/emme/clients/application/service/RetireCustomerService.java",
          "modules/clients/src/main/java/com/emme/clients/application/service/SearchCustomersService.java",
          "modules/clients/src/main/java/com/emme/clients/application/service/UpdateCustomerService.java",
          "modules/documents/src/main/java/com/emme/documents/application/service/ChunkDocumentService.java",
          "modules/documents/src/main/java/com/emme/documents/application/service/FailDocumentService.java",
          "modules/documents/src/main/java/com/emme/documents/application/service/GetDocumentChunksService.java",
          "modules/documents/src/main/java/com/emme/documents/application/service/GetDocumentService.java",
          "modules/documents/src/main/java/com/emme/documents/application/service/ListDocumentsService.java",
          "modules/documents/src/main/java/com/emme/documents/application/service/ProcessDocumentService.java",
          "modules/documents/src/main/java/com/emme/documents/application/service/RetireDocumentService.java",
          "modules/documents/src/main/java/com/emme/documents/application/service/SearchDocumentChunksService.java",
          "modules/documents/src/main/java/com/emme/documents/application/service/UploadDocumentService.java",
          "modules/services/src/main/java/com/emme/services/application/service/AddArtistCapabilityService.java",
          "modules/services/src/main/java/com/emme/services/application/service/CreateArtistService.java",
          "modules/services/src/main/java/com/emme/services/application/service/DeactivateArtistService.java",
          "modules/services/src/main/java/com/emme/services/application/service/GetArtistService.java",
          "modules/services/src/main/java/com/emme/services/application/service/GetServiceCatalogEntryService.java",
          "modules/services/src/main/java/com/emme/services/application/service/ListActiveServiceCatalogEntriesService.java",
          "modules/services/src/main/java/com/emme/services/application/service/ListTenantArtistsService.java",
          "modules/services/src/main/java/com/emme/services/application/service/RemoveArtistCapabilityService.java",
          "modules/services/src/main/java/com/emme/services/application/service/UpdateArtistService.java",
          "modules/salon/src/main/java/com/emme/salon/application/service/GetBookingPolicyService.java",
          "modules/salon/src/main/java/com/emme/salon/application/service/GetBusinessProfileConfigService.java",
          "modules/salon/src/main/java/com/emme/salon/application/service/GetBusinessProfileService.java",
          "modules/salon/src/main/java/com/emme/salon/application/service/GetOperatingHoursService.java",
          "modules/salon/src/main/java/com/emme/salon/application/service/UpdateBookingPolicyService.java",
          "modules/salon/src/main/java/com/emme/salon/application/service/UpdateBusinessProfileService.java",
          "modules/salon/src/main/java/com/emme/salon/application/service/UpdateOperatingHoursService.java",
          "modules/calendar/src/main/java/com/emme/calendar/application/service/CompleteGoogleOAuthService.java",
          "modules/calendar/src/main/java/com/emme/calendar/application/service/CreateCalendarEventLinkService.java",
          "modules/calendar/src/main/java/com/emme/calendar/application/service/DisconnectGoogleOAuthService.java",
          "modules/calendar/src/main/java/com/emme/calendar/application/service/FindCalendarEventLinkService.java",
          "modules/calendar/src/main/java/com/emme/calendar/application/service/FindCalendarEventLinksService.java",
          "modules/calendar/src/main/java/com/emme/calendar/application/service/GetBusyTimesService.java",
          "modules/calendar/src/main/java/com/emme/calendar/application/service/GetGoogleOAuthStatusService.java",
          "modules/calendar/src/main/java/com/emme/calendar/application/service/MarkCalendarEventLinkSyncedService.java",
          "modules/calendar/src/main/java/com/emme/calendar/application/service/MarkCalendarEventLinksDeletedService.java",
          "modules/calendar/src/main/java/com/emme/calendar/application/service/MarkCalendarEventLinksFailedService.java",
          "modules/calendar/src/main/java/com/emme/calendar/application/service/StartGoogleOAuthService.java",
          "modules/calendar/src/main/java/com/emme/calendar/application/service/SyncCalendarEventsService.java",
          "modules/calendar/src/main/java/com/emme/calendar/application/service/SyncClientCalendarService.java",
          "modules/calendar/src/main/java/com/emme/calendar/application/service/UnsyncClientCalendarService.java",
          "modules/payment/src/main/java/com/emme/payment/application/service/AuthorizePaymentService.java",
          "modules/payment/src/main/java/com/emme/payment/application/service/CapturePaymentService.java",
          "modules/payment/src/main/java/com/emme/payment/application/service/GetPaymentService.java",
          "modules/payment/src/main/java/com/emme/payment/application/service/InitiatePaymentService.java",
          "modules/payment/src/main/java/com/emme/payment/application/service/ListPaymentsService.java",
          "modules/payment/src/main/java/com/emme/payment/application/service/ProcessPaymentCallbackService.java",
          "modules/payment/src/main/java/com/emme/payment/application/service/RefundPaymentService.java",
          "modules/notification/src/main/java/com/emme/notification/application/service/CancelNotificationService.java",
          "modules/notification/src/main/java/com/emme/notification/application/service/DeliverNotificationService.java",
          "modules/notification/src/main/java/com/emme/notification/application/service/GetNotificationService.java",
          "modules/notification/src/main/java/com/emme/notification/application/service/ListNotificationsService.java",
          "modules/notification/src/main/java/com/emme/notification/application/service/RequestNotificationService.java",
          "modules/subscriptions/src/main/java/com/emme/subscriptions/application/service/ChangeSubscriptionPlanService.java",
          "modules/subscriptions/src/main/java/com/emme/subscriptions/application/service/CreateSubscriptionService.java",
          "modules/subscriptions/src/main/java/com/emme/subscriptions/application/service/EnforceEntitlementService.java",
          "modules/subscriptions/src/main/java/com/emme/subscriptions/application/service/EnsureTenantSubscriptionService.java",
          "modules/subscriptions/src/main/java/com/emme/subscriptions/application/service/GetSubscriptionPlanService.java",
          "modules/subscriptions/src/main/java/com/emme/subscriptions/application/service/GetSubscriptionService.java");

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
