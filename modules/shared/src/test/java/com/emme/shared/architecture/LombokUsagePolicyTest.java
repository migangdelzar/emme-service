package com.emme.shared.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class LombokUsagePolicyTest {

  private static final List<String> OWNED_SOURCE_ROOTS =
      List.of("modules", "libraries", "applications", "build-logic");
  private static final String POLICY_TEST =
      "modules/shared/src/test/java/com/emme/shared/architecture/LombokUsagePolicyTest.java";
  private static final Map<String, Set<String>> APPROVED_TEST_FILES_BY_SOURCE_SET =
      Map.of(
          "test", Set.<String>of(),
          "integrationTest", Set.<String>of(),
          "testFixtures", Set.<String>of(),
          "e2eTest", Set.<String>of());

  @Test
  void scansAllOwnedSourceSetsWithoutGeneratedBuildOutput() throws IOException {
    Path root = sourcePath();

    assertThat(ownedJavaSources(root))
        .allMatch(path -> path.toString().replace('\\', '/').contains("/src/"))
        .anyMatch(path -> path.toString().replace('\\', '/').contains("/applications/"))
        .anyMatch(path -> path.toString().replace('\\', '/').contains("/src/test/java/"))
        .anyMatch(path -> path.toString().replace('\\', '/').contains("/src/integrationTest/java/"))
        .anyMatch(path -> path.toString().replace('\\', '/').contains("/src/e2eTest/java/"))
        .anyMatch(path -> path.toString().replace('\\', '/').contains("/src/testFixtures/java/"))
        .noneMatch(path -> path.toString().replace('\\', '/').contains("/build/"));
  }

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
          "modules/subscriptions/src/main/java/com/emme/subscriptions/application/service/GetSubscriptionService.java",
          "modules/identity/src/main/java/com/emme/identity/application/service/AssignMembershipService.java",
          "modules/identity/src/main/java/com/emme/identity/application/service/AuthenticateCustomerService.java",
          "modules/identity/src/main/java/com/emme/identity/application/service/AuthenticateUserService.java",
          "modules/identity/src/main/java/com/emme/identity/application/service/EnsureCustomerMembershipService.java",
          "modules/identity/src/main/java/com/emme/identity/application/service/EnsureTenantMembershipService.java",
          "modules/identity/src/main/java/com/emme/identity/application/service/GetCurrentUserMembershipsService.java",
          "modules/identity/src/main/java/com/emme/identity/application/service/GetCurrentUserService.java",
          "modules/identity/src/main/java/com/emme/identity/application/service/GetEffectiveFeatureFlagsService.java",
          "modules/identity/src/main/java/com/emme/identity/application/service/GetUserPermissionsService.java",
          "modules/identity/src/main/java/com/emme/identity/application/service/ListPlatformFeatureFlagsService.java",
          "modules/identity/src/main/java/com/emme/identity/application/service/RevokeMembershipService.java",
          "modules/identity/src/main/java/com/emme/identity/application/service/SetPlatformFeatureFlagService.java",
          "modules/identity/src/main/java/com/emme/identity/application/service/UpdateCustomerProfileService.java",
          "modules/tenancy/src/main/java/com/emme/tenancy/application/service/CreateTenantService.java",
          "modules/tenancy/src/main/java/com/emme/tenancy/application/service/GetTenantProvisioningStatusService.java",
          "modules/tenancy/src/main/java/com/emme/tenancy/application/service/GetTenantService.java",
          "modules/tenancy/src/main/java/com/emme/tenancy/application/service/ListActiveTenantsService.java",
          "modules/tenancy/src/main/java/com/emme/tenancy/application/service/ListTenantsService.java",
          "modules/tenancy/src/main/java/com/emme/tenancy/application/service/ReactivateTenantService.java",
          "modules/tenancy/src/main/java/com/emme/tenancy/application/service/RequestTenantProvisioningService.java",
          "modules/tenancy/src/main/java/com/emme/tenancy/application/service/ResolveTenantDatabaseIdService.java",
          "modules/tenancy/src/main/java/com/emme/tenancy/application/service/ResolveTenantIdBySlugService.java",
          "modules/tenancy/src/main/java/com/emme/tenancy/application/service/StageDeleteTenantService.java",
          "modules/tenancy/src/main/java/com/emme/tenancy/application/service/SuspendTenantService.java",
          "modules/tenancy/src/main/java/com/emme/tenancy/application/service/UpdateTenantIdentityRealmService.java",
          "modules/tenancy/src/main/java/com/emme/tenancy/application/service/UpdateTenantService.java");

  @Test
  void onlyUsesLombokInApprovedProductionFiles() throws IOException {
    Path root = sourcePath();

    Set<String> lombokFiles =
        productionSources(root).stream()
            .filter(this::containsLombok)
            .map(path -> root.relativize(path).toString().replace('\\', '/'))
            .collect(Collectors.toSet());

    assertThat(lombokFiles).containsExactlyInAnyOrderElementsOf(APPROVED_FILES);

    Set<String> approvedTestFiles =
        APPROVED_TEST_FILES_BY_SOURCE_SET.values().stream()
            .flatMap(Set::stream)
            .collect(Collectors.toSet());
    Set<String> testLombokFiles =
        ownedJavaSources(root).stream()
            .filter(path -> !isProductionSource(path))
            .filter(path -> !isPolicyTest(root, path))
            .filter(this::containsLombok)
            .map(path -> root.relativize(path).toString().replace('\\', '/'))
            .collect(Collectors.toSet());

    assertThat(testLombokFiles).containsExactlyInAnyOrderElementsOf(approvedTestFiles);
  }

  @Test
  void forbidsBroadLombokAnnotationsAndBoundaryUsage() throws IOException {
    Path root = sourcePath();

    for (Path source : ownedJavaSources(root)) {
      String normalized = source.toString().replace('\\', '/');
      String contents = Files.readString(source);

      if (normalized.contains("/domain/") || normalized.contains("/persistence/entity/")) {
        assertThat(contents)
            .as("forbidden Lombok source: %s", source)
            .doesNotContain("import lombok.");
      }
      assertThat(contents)
          .as("forbidden Lombok annotation: %s", source)
          .doesNotContainPattern(
              "(?m)^\\s*@(Data|Setter|EqualsAndHashCode|ToString|Delegate|SneakyThrows|Synchronized|ExtensionMethod|StandardException)\\b");

      if (!isPolicyTest(root, source) && containsAnnotation(contents, "Builder")) {
        String relativePath = root.relativize(source).toString().replace('\\', '/');
        assertThat(approvedLombokFiles()).contains(relativePath);
        assertThat(contents).containsPattern("setterPrefix\\s*=\\s*\"with\"");
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
    return ownedJavaSources(root).stream()
        .filter(LombokUsagePolicyTest::isProductionSource)
        .toList();
  }

  private static List<Path> ownedJavaSources(Path root) throws IOException {
    try (Stream<Path> sources = Files.walk(root)) {
      return sources
          .filter(
              path -> {
                String normalized = root.relativize(path).toString().replace('\\', '/');
                return OWNED_SOURCE_ROOTS.stream()
                        .anyMatch(sourceRoot -> normalized.startsWith(sourceRoot + "/"))
                    && normalized.contains("/src/")
                    && !normalized.contains("/build/")
                    && normalized.endsWith(".java");
              })
          .toList();
    }
  }

  private static boolean isProductionSource(Path path) {
    return path.toString().replace('\\', '/').contains("/src/main/java/");
  }

  private static boolean isPolicyTest(Path root, Path source) {
    return root.relativize(source).toString().replace('\\', '/').equals(POLICY_TEST);
  }

  private static Set<String> approvedLombokFiles() {
    return Stream.concat(
            APPROVED_FILES.stream(),
            APPROVED_TEST_FILES_BY_SOURCE_SET.values().stream().flatMap(Set::stream))
        .collect(Collectors.toSet());
  }

  private static boolean containsAnnotation(String contents, String annotation) {
    return contents.lines().anyMatch(line -> line.matches("\\s*@" + annotation + "\\b.*"));
  }

  private boolean containsLombok(Path source) {
    try {
      String contents = Files.readString(source);
      return contents.lines().anyMatch(line -> line.matches("\\s*import\\s+lombok\\..*"))
          || contents.lines().anyMatch(line -> line.matches("\\s*@lombok\\.[A-Za-z].*"));
    } catch (IOException exception) {
      throw new IllegalStateException("Cannot read source: " + source, exception);
    }
  }

  private static Path sourcePath() {
    Path current = Path.of("").toAbsolutePath();
    while (current != null) {
      Path candidate = current;
      if (OWNED_SOURCE_ROOTS.stream()
          .allMatch(sourceRoot -> Files.isDirectory(candidate.resolve(sourceRoot)))) {
        return current;
      }
      current = current.getParent();
    }
    throw new IllegalStateException("Cannot locate repository root");
  }
}
