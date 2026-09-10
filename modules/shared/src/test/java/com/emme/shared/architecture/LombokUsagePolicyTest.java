package com.emme.shared.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
  private static final Pattern FORBIDDEN_ANNOTATION_PATTERN =
      Pattern.compile(
          "@(?:[A-Za-z_$][\\w$]*\\.)*(?:Data|Setter|EqualsAndHashCode|ToString|Delegate|SneakyThrows|Synchronized|ExtensionMethod|StandardException)\\b");
  private static final Pattern BUILDER_ANNOTATION_PATTERN =
      Pattern.compile("@(?:[A-Za-z_$][\\w$]*\\.)*Builder\\b(?:\\s*\\(([^)]*)\\))?");

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

  @Test
  void considersEveryOwnedSourceRootIncludingBuildLogic() throws IOException {
    Path root = sourcePath();

    assertThat(ownedSourceRoots(root))
        .containsExactlyInAnyOrder("modules", "libraries", "applications", "build-logic");
  }

  @Test
  void rejectsEachForbiddenAnnotationFamilyInSyntheticSources() {
    Set<String> forbiddenAnnotations =
        Set.of(
            "Data",
            "Setter",
            "EqualsAndHashCode",
            "ToString",
            "Delegate",
            "SneakyThrows",
            "Synchronized",
            "ExtensionMethod",
            "StandardException");

    for (String annotation : forbiddenAnnotations) {
      assertThat(containsForbiddenAnnotation("@" + annotation + "\nclass Synthetic {}"))
          .as("unqualified forbidden annotation: %s", annotation)
          .isTrue();
      assertThat(containsForbiddenAnnotation("@lombok." + annotation + "\nclass Synthetic {}"))
          .as("qualified forbidden annotation: %s", annotation)
          .isTrue();
    }
    assertThat(containsForbiddenAnnotation("@Nullable @lombok.Data\nclass Synthetic {}"))
        .as("forbidden annotations after another annotation on the same line")
        .isTrue();

    assertThat(containsForbiddenAnnotation("// @Data\nString description = \"@lombok.Setter\";"))
        .isFalse();
  }

  @Test
  void validatesBuilderPrefixOnlyInsideQualifiedBuilderAnnotation() {
    assertThat(builderHasRequiredSetterPrefix("@Builder(setterPrefix = \"with\")")).isTrue();
    assertThat(builderHasRequiredSetterPrefix("@lombok.Builder(setterPrefix = \"with\")")).isTrue();
    assertThat(builderHasRequiredSetterPrefix("@Builder"))
        .as("default builder prefix must be rejected")
        .isFalse();
    assertThat(builderHasRequiredSetterPrefix("@lombok.Builder(setterPrefix = \"set\")")).isFalse();
    assertThat(containsBuilderAnnotation("@Validated @Builder\nclass Synthetic {}"))
        .as("builder annotations after another annotation on the same line")
        .isTrue();
    assertThat(builderHasRequiredSetterPrefix("@Validated @Builder\nclass Synthetic {}"))
        .as("builder without a prefix must be rejected after another annotation")
        .isFalse();
    assertThat(
            builderHasRequiredSetterPrefix(
                "@Builder\n// setterPrefix = \"with\"\n"
                    + "String misleading = \"setterPrefix = \\\"with\\\"\";"))
        .as("comments and string literals must not satisfy the builder policy")
        .isFalse();
    assertThat(
            builderHasRequiredSetterPrefix(
                "@Builder(builderMethodName = \"setterPrefix = \\\"with\\\"\")"))
        .as("quoted text in another builder argument must not satisfy setterPrefix")
        .isFalse();
    assertThat(containsBuilderAnnotation("// @Builder\nString description = \"@Builder\";"))
        .as("comments and string literals must not count as builder annotations")
        .isFalse();
  }

  @Test
  void validatesApprovedTestMapEntriesAgainstOwnedNonProductionSourceSets() throws IOException {
    Path root = sourcePath();

    assertThat(
            isValidApprovedTestSource(
                root,
                "test",
                "modules/shared/src/test/java/com/emme/shared/architecture/LombokUsagePolicyTest.java"))
        .isTrue();
    assertThat(
            isValidApprovedTestSource(
                root,
                "integrationTest",
                "modules/shared/src/test/java/com/emme/shared/architecture/LombokUsagePolicyTest.java"))
        .isFalse();
    assertThat(
            isValidApprovedTestSource(
                root,
                "test",
                "modules/shared/src/main/java/com/emme/shared/configuration/I18nConfiguration.java"))
        .isFalse();
    assertThat(isValidApprovedTestSource(root, "test", "README.java")).isFalse();
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
          "modules/assistant/src/main/java/com/emme/assistant/adapter/in/webhook/WhatsAppWebhookController.java",
          "modules/assistant/src/main/java/com/emme/assistant/adapter/in/webhook/WhatsAppWebhookMapper.java",
          "modules/assistant/src/main/java/com/emme/assistant/adapter/out/persistence/adapter/WhatsAppWebhookEventPersistenceAdapter.java",
          "modules/assistant/src/main/java/com/emme/assistant/adapter/out/persistence/mapper/ConversationEventPersistenceMapper.java",
          "modules/assistant/src/main/java/com/emme/assistant/adapter/out/tenant/ConfiguredWhatsAppTenantResolver.java",
          "modules/assistant/src/main/java/com/emme/assistant/ai/adapter/in/messaging/SemanticCacheInvalidationListener.java",
          "modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/storage/CatalogDesignImageReader.java",
          "modules/appointments/src/main/java/com/emme/appointments/adapter/in/web/controller/DashboardController.java",
          "modules/appointments/src/main/java/com/emme/appointments/adapter/out/messaging/publisher/SpringAppointmentEventPublisher.java",
          "modules/appointments/src/main/java/com/emme/appointments/adapter/out/persistence/adapter/AppointmentCollisionAdapter.java",
          "modules/appointments/src/main/java/com/emme/appointments/application/service/ListAppointmentsService.java",
          "modules/appointments/src/main/java/com/emme/appointments/application/service/FindAvailableSlotsService.java",
          "modules/catalog/src/main/java/com/emme/catalog/application/service/MatchCatalogItemsService.java",
          "modules/catalog/src/main/java/com/emme/catalog/application/service/AddCatalogItemImageService.java",
          "modules/catalog/src/main/java/com/emme/catalog/application/service/ListCatalogItemsService.java",
          "modules/catalog/src/main/java/com/emme/catalog/adapter/in/web/controller/CatalogController.java",
          "modules/clients/src/main/java/com/emme/clients/application/service/CreateCustomerService.java",
          "modules/clients/src/main/java/com/emme/clients/application/service/GetCustomerService.java",
          "modules/clients/src/main/java/com/emme/clients/application/service/ListCustomersService.java",
          "modules/clients/src/main/java/com/emme/clients/application/service/ListTenantCustomersService.java",
          "modules/clients/src/main/java/com/emme/clients/application/service/RetireCustomerService.java",
          "modules/clients/src/main/java/com/emme/clients/application/service/SearchCustomersService.java",
          "modules/clients/src/main/java/com/emme/clients/application/service/UpdateCustomerService.java",
          "modules/clients/src/main/java/com/emme/clients/adapter/in/web/controller/CustomerController.java",
          "modules/documents/src/main/java/com/emme/documents/application/service/ChunkDocumentService.java",
          "modules/documents/src/main/java/com/emme/documents/application/service/FailDocumentService.java",
          "modules/documents/src/main/java/com/emme/documents/application/service/GetDocumentChunksService.java",
          "modules/documents/src/main/java/com/emme/documents/application/service/GetDocumentService.java",
          "modules/documents/src/main/java/com/emme/documents/application/service/ListDocumentsService.java",
          "modules/documents/src/main/java/com/emme/documents/application/service/ProcessDocumentService.java",
          "modules/documents/src/main/java/com/emme/documents/application/service/RetireDocumentService.java",
          "modules/documents/src/main/java/com/emme/documents/application/service/SearchDocumentChunksService.java",
          "modules/documents/src/main/java/com/emme/documents/application/service/UploadDocumentService.java",
          "modules/documents/src/main/java/com/emme/documents/adapter/in/web/controller/DocumentController.java",
          "modules/documents/src/main/java/com/emme/documents/adapter/out/persistence/adapter/DocumentPersistenceAdapter.java",
          "modules/documents/src/main/java/com/emme/documents/adapter/out/search/HybridDocumentSearchAdapter.java",
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
          "modules/calendar/src/main/java/com/emme/calendar/adapter/in/web/controller/CalendarController.java",
          "modules/calendar/src/main/java/com/emme/calendar/adapter/in/web/controller/ClientCalendarController.java",
          "modules/calendar/src/main/java/com/emme/calendar/adapter/in/web/controller/GoogleOAuthController.java",
          "modules/calendar/src/main/java/com/emme/calendar/adapter/in/web/controller/SheetsController.java",
          "modules/calendar/src/main/java/com/emme/calendar/adapter/out/google/adapter/GoogleSheetsAdapter.java",
          "modules/calendar/src/main/java/com/emme/calendar/adapter/out/google/oauth/OAuthTokenSource.java",
          "modules/calendar/src/main/java/com/emme/calendar/adapter/out/persistence/adapter/CalendarPersistenceAdapter.java",
          "modules/calendar/src/main/java/com/emme/calendar/adapter/out/persistence/adapter/GoogleSpreadsheetLinkQueryAdapter.java",
          "modules/payment/src/main/java/com/emme/payment/application/service/AuthorizePaymentService.java",
          "modules/payment/src/main/java/com/emme/payment/application/service/CapturePaymentService.java",
          "modules/payment/src/main/java/com/emme/payment/application/service/GetPaymentService.java",
          "modules/payment/src/main/java/com/emme/payment/application/service/InitiatePaymentService.java",
          "modules/payment/src/main/java/com/emme/payment/application/service/ListPaymentsService.java",
          "modules/payment/src/main/java/com/emme/payment/application/service/ProcessPaymentCallbackService.java",
          "modules/payment/src/main/java/com/emme/payment/application/service/RefundPaymentService.java",
          "modules/payment/src/main/java/com/emme/payment/adapter/out/messaging/publisher/SpringPaymentWorkflowEventPublisher.java",
          "modules/payment/src/main/java/com/emme/payment/adapter/out/persistence/adapter/PaymentWebhookEventPersistenceAdapter.java",
          "modules/notification/src/main/java/com/emme/notification/application/service/CancelNotificationService.java",
          "modules/notification/src/main/java/com/emme/notification/application/service/DeliverNotificationService.java",
          "modules/notification/src/main/java/com/emme/notification/application/service/GetNotificationService.java",
          "modules/notification/src/main/java/com/emme/notification/application/service/ListNotificationsService.java",
          "modules/notification/src/main/java/com/emme/notification/application/service/RequestNotificationService.java",
          "modules/notification/src/main/java/com/emme/notification/adapter/out/event/SpringNotificationEventPublisher.java",
          "modules/subscriptions/src/main/java/com/emme/subscriptions/application/service/ChangeSubscriptionPlanService.java",
          "modules/subscriptions/src/main/java/com/emme/subscriptions/application/service/CreateSubscriptionService.java",
          "modules/subscriptions/src/main/java/com/emme/subscriptions/application/service/EnforceEntitlementService.java",
          "modules/subscriptions/src/main/java/com/emme/subscriptions/application/service/EnsureTenantSubscriptionService.java",
          "modules/subscriptions/src/main/java/com/emme/subscriptions/application/service/GetSubscriptionPlanService.java",
          "modules/subscriptions/src/main/java/com/emme/subscriptions/application/service/GetSubscriptionService.java",
          "modules/subscriptions/src/main/java/com/emme/subscriptions/adapter/out/persistence/adapter/SubscriptionPersistenceAdapter.java",
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
          "modules/identity/src/main/java/com/emme/identity/adapter/in/messaging/consumer/AppointmentCreatedConsumer.java",
          "modules/identity/src/main/java/com/emme/identity/adapter/in/web/controller/AuthController.java",
          "modules/identity/src/main/java/com/emme/identity/adapter/in/web/controller/CurrentUserController.java",
          "modules/identity/src/main/java/com/emme/identity/adapter/in/web/controller/FeatureFlagController.java",
          "modules/identity/src/main/java/com/emme/identity/adapter/in/web/controller/IdentityController.java",
          "modules/identity/src/main/java/com/emme/identity/adapter/in/web/controller/TenantFeatureFlagController.java",
          "modules/identity/src/main/java/com/emme/identity/adapter/in/web/security/IdentityJwtAuthoritiesConverter.java",
          "modules/identity/src/main/java/com/emme/identity/adapter/in/web/security/IdentityUserAuthoritiesMapper.java",
          "modules/identity/src/main/java/com/emme/identity/adapter/out/client/keycloak/CustomerTokenDecoderAdapter.java",
          "modules/identity/src/main/java/com/emme/identity/adapter/out/client/subscription/SubscriptionPlanAdapter.java",
          "modules/identity/src/main/java/com/emme/identity/adapter/out/module/tenancy/TenantIdentityRealmAdapter.java",
          "modules/identity/src/main/java/com/emme/identity/adapter/out/persistence/adapter/CustomerMembershipPersistenceAdapter.java",
          "modules/identity/src/main/java/com/emme/identity/adapter/out/ratelimit/RedisLoginAttemptRateLimiter.java",
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
          "modules/tenancy/src/main/java/com/emme/tenancy/application/service/UpdateTenantService.java",
          "modules/tenancy/src/main/java/com/emme/tenancy/adapter/out/client/database/LiquibaseTenantSchemaMigrationAdapter.java",
          "modules/tenancy/src/main/java/com/emme/tenancy/adapter/out/messaging/publisher/SpringTenantEventPublisher.java",
          "modules/tenancy/src/main/java/com/emme/tenancy/adapter/out/persistence/adapter/AuditEventPersistenceAdapter.java",
          "modules/tenancy/src/main/java/com/emme/tenancy/adapter/out/persistence/adapter/TenantProvisioningPersistenceAdapter.java",
          "modules/tenancy/src/main/java/com/emme/tenancy/application/audit/AuditEventRecorder.java",
          "modules/shared/src/main/java/com/emme/shared/persistence/jdbc/BootstrapConnectionExecutor.java",
          "modules/shared/src/main/java/com/emme/shared/search/postgres/PostgresHybridSearch.java",
          "modules/shared/src/main/java/com/emme/shared/web/advice/GlobalExceptionHandler.java");

  private static final Set<String> APPROVED_LOGGER_FILES =
      Set.of(
          "modules/appointments/src/main/java/com/emme/appointments/adapter/in/web/controller/DashboardController.java",
          "modules/appointments/src/main/java/com/emme/appointments/adapter/in/web/sse/DashboardBroadcaster.java",
          "modules/assistant/src/main/java/com/emme/assistant/adapter/in/webhook/WhatsAppWebhookController.java",
          "modules/assistant/src/main/java/com/emme/assistant/adapter/in/webhook/WhatsAppWebhookMapper.java",
          "modules/assistant/src/main/java/com/emme/assistant/adapter/out/client/whatsapp/WhatsAppReplyAdapter.java",
          "modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/provider/springai/SpringAiNailDesignExtractor.java",
          "modules/assistant/src/main/java/com/emme/assistant/ai/application/provider/TracingAiChatCompletion.java",
          "modules/assistant/src/main/java/com/emme/assistant/ai/application/provider/TracingEmbeddingService.java",
          "modules/assistant/src/main/java/com/emme/assistant/ai/application/semantic/SemanticCacheInvalidationService.java",
          "modules/assistant/src/main/java/com/emme/assistant/ai/application/semantic/SemanticCacheResolver.java",
          "modules/assistant/src/main/java/com/emme/assistant/ai/application/semantic/SemanticChatCache.java",
          "modules/assistant/src/main/java/com/emme/assistant/ai/application/semantic/SemanticIntentClassifier.java",
          "modules/assistant/src/main/java/com/emme/assistant/ai/application/semantic/SemanticToolSelector.java",
          "modules/assistant/src/main/java/com/emme/assistant/ai/application/tool/AuthorizedAiToolGateway.java",
          "modules/calendar/src/main/java/com/emme/calendar/adapter/in/web/controller/GoogleOAuthController.java",
          "modules/calendar/src/main/java/com/emme/calendar/adapter/out/google/adapter/ClientCalendarSyncAdapter.java",
          "modules/calendar/src/main/java/com/emme/calendar/adapter/out/google/adapter/GoogleOAuthAdapter.java",
          "modules/calendar/src/main/java/com/emme/calendar/adapter/out/google/adapter/GoogleSheetsAdapter.java",
          "modules/calendar/src/main/java/com/emme/calendar/adapter/out/google/adapter/StaffCalendarSyncAdapter.java",
          "modules/calendar/src/main/java/com/emme/calendar/adapter/out/google/client/GoogleCalendarClient.java",
          "modules/calendar/src/main/java/com/emme/calendar/adapter/out/google/client/GoogleSheetsClient.java",
          "modules/calendar/src/main/java/com/emme/calendar/adapter/out/google/oauth/OAuthTokenSource.java",
          "modules/calendar/src/main/java/com/emme/calendar/application/service/GetBusyTimesService.java",
          "modules/calendar/src/main/java/com/emme/calendar/application/service/SyncCalendarEventsService.java",
          "modules/documents/src/main/java/com/emme/documents/application/service/FailDocumentService.java");

  @Test
  void onlyUsesLombokInApprovedProductionFiles() throws IOException {
    Path root = sourcePath();

    Set<String> lombokFiles =
        productionSources(root).stream()
            .filter(this::containsLombok)
            .map(path -> root.relativize(path).toString().replace('\\', '/'))
            .collect(Collectors.toSet());

    assertThat(lombokFiles)
        .containsExactlyInAnyOrderElementsOf(
            Stream.concat(APPROVED_FILES.stream(), APPROVED_LOGGER_FILES.stream())
                .collect(Collectors.toSet()));

    Set<String> approvedTestFiles = approvedTestFiles(root);
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
      assertThat(maskCommentsAndStrings(contents))
          .as("forbidden Lombok annotation: %s", source)
          .doesNotContainPattern(FORBIDDEN_ANNOTATION_PATTERN);

      if (!isPolicyTest(root, source) && containsBuilderAnnotation(contents)) {
        String relativePath = root.relativize(source).toString().replace('\\', '/');
        assertThat(approvedLombokFiles()).contains(relativePath);
        assertThat(builderHasRequiredSetterPrefix(contents))
            .as("every builder must use setterPrefix = \"with\": %s", source)
            .isTrue();
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

  @Test
  void adoptsSlf4jForApprovedLoggerFiles() throws IOException {
    Path root = sourcePath();

    for (String relativePath : APPROVED_LOGGER_FILES) {
      assertThat(Files.readString(root.resolve(relativePath)))
          .as("Lombok logger source: %s", relativePath)
          .doesNotContain("LoggerFactory.getLogger")
          .doesNotContain("import org.slf4j.Logger;")
          .contains("import lombok.extern.slf4j.Slf4j;")
          .contains("@Slf4j");
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
                return ownedSourceRoots(root).stream()
                        .anyMatch(sourceRoot -> normalized.startsWith(sourceRoot + "/"))
                    && normalized.contains("/src/")
                    && !normalized.contains("/build/")
                    && normalized.endsWith(".java");
              })
          .toList();
    }
  }

  private static Set<String> ownedSourceRoots(Path root) {
    return OWNED_SOURCE_ROOTS.stream()
        .filter(sourceRoot -> Files.isDirectory(root.resolve(sourceRoot)))
        .collect(Collectors.toSet());
  }

  private static boolean isProductionSource(Path path) {
    return path.toString().replace('\\', '/').contains("/src/main/java/");
  }

  private static boolean isPolicyTest(Path root, Path source) {
    return root.relativize(source).toString().replace('\\', '/').equals(POLICY_TEST);
  }

  private static Set<String> approvedLombokFiles() {
    return Stream.concat(
            Stream.concat(APPROVED_FILES.stream(), APPROVED_LOGGER_FILES.stream()),
            APPROVED_TEST_FILES_BY_SOURCE_SET.values().stream().flatMap(Set::stream))
        .collect(Collectors.toSet());
  }

  private static boolean containsForbiddenAnnotation(String contents) {
    return FORBIDDEN_ANNOTATION_PATTERN.matcher(maskCommentsAndStrings(contents)).find();
  }

  private static boolean containsBuilderAnnotation(String contents) {
    return BUILDER_ANNOTATION_PATTERN.matcher(maskCommentsAndStrings(contents)).find();
  }

  private static boolean builderHasRequiredSetterPrefix(String contents) {
    String maskedContents = maskCommentsAndStrings(contents);
    Matcher builders = BUILDER_ANNOTATION_PATTERN.matcher(maskedContents);
    boolean foundBuilder = false;
    while (builders.find()) {
      foundBuilder = true;
      if (builders.start(1) < 0
          || !hasRequiredSetterPrefix(contents.substring(builders.start(1), builders.end(1)))) {
        return false;
      }
    }
    return foundBuilder;
  }

  private static boolean hasRequiredSetterPrefix(String annotationBody) {
    int argumentStart = 0;
    while (argumentStart < annotationBody.length()) {
      int argumentEnd = findTopLevelDelimiter(annotationBody, argumentStart, ',');
      if (isRequiredSetterPrefixElement(annotationBody.substring(argumentStart, argumentEnd))) {
        return true;
      }
      if (argumentEnd == annotationBody.length()) {
        break;
      }
      argumentStart = argumentEnd + 1;
    }
    return false;
  }

  private static boolean isRequiredSetterPrefixElement(String argument) {
    int equals = findTopLevelDelimiter(argument, 0, '=');
    if (equals < 0) {
      return false;
    }
    String elementName = maskCommentsAndStrings(argument.substring(0, equals)).trim();
    return elementName.equals("setterPrefix")
        && isWithStringLiteral(argument.substring(equals + 1));
  }

  private static boolean isWithStringLiteral(String value) {
    int index = skipWhitespaceAndComments(value, 0);
    if (index >= value.length()
        || value.charAt(index) != '"'
        || value.startsWith("\"\"\"", index)) {
      return false;
    }
    index++;
    int valueStart = index;
    while (index < value.length() && value.charAt(index) != '"') {
      if (value.charAt(index) == '\\'
          || value.charAt(index) == '\n'
          || value.charAt(index) == '\r') {
        return false;
      }
      index++;
    }
    if (index >= value.length() || !value.substring(valueStart, index).equals("with")) {
      return false;
    }
    return skipWhitespaceAndComments(value, index + 1) == value.length();
  }

  private static int findTopLevelDelimiter(String source, int start, char delimiter) {
    int parentheses = 0;
    int braces = 0;
    int brackets = 0;
    int index = start;
    while (index < source.length()) {
      if (source.startsWith("//", index) || source.startsWith("/*", index)) {
        index = skipComment(source, index);
        continue;
      }
      if (source.charAt(index) == '"') {
        index = skipStringLiteral(source, index);
        continue;
      }
      switch (source.charAt(index)) {
        case '(' -> parentheses++;
        case ')' -> parentheses--;
        case '{' -> braces++;
        case '}' -> braces--;
        case '[' -> brackets++;
        case ']' -> brackets--;
        default -> {
          if (source.charAt(index) == delimiter
              && parentheses == 0
              && braces == 0
              && brackets == 0) {
            return index;
          }
        }
      }
      index++;
    }
    return source.length();
  }

  private static int skipWhitespaceAndComments(String source, int start) {
    int index = start;
    while (index < source.length()) {
      if (Character.isWhitespace(source.charAt(index))) {
        index++;
      } else if (source.startsWith("//", index) || source.startsWith("/*", index)) {
        index = skipComment(source, index);
      } else {
        break;
      }
    }
    return index;
  }

  private static int skipComment(String source, int start) {
    if (source.startsWith("//", start)) {
      int newline = source.indexOf('\n', start + 2);
      return newline < 0 ? source.length() : newline;
    }
    int closing = source.indexOf("*/", start + 2);
    return closing < 0 ? source.length() : closing + 2;
  }

  private static int skipStringLiteral(String source, int start) {
    if (source.startsWith("\"\"\"", start)) {
      int closing = source.indexOf("\"\"\"", start + 3);
      return closing < 0 ? source.length() : closing + 3;
    }
    int index = start + 1;
    while (index < source.length()) {
      if (source.charAt(index) == '\\') {
        index += Math.min(2, source.length() - index);
      } else if (source.charAt(index++) == '"') {
        break;
      }
    }
    return index;
  }

  private static String maskCommentsAndStrings(String source) {
    char[] masked = source.toCharArray();
    int index = 0;
    while (index < source.length()) {
      if (source.startsWith("//", index) || source.startsWith("/*", index)) {
        int end = skipComment(source, index);
        blankRange(masked, index, end);
        index = end;
      } else if (source.charAt(index) == '"') {
        int end = skipStringLiteral(source, index);
        blankRange(masked, index, end);
        index = end;
      } else {
        index++;
      }
    }
    return new String(masked);
  }

  private static void blankRange(char[] source, int start, int end) {
    for (int index = start; index < end; index++) {
      if (source[index] != '\n' && source[index] != '\r') {
        source[index] = ' ';
      }
    }
  }

  private static Set<String> approvedTestFiles(Path root) throws IOException {
    Set<String> approvedFiles =
        APPROVED_TEST_FILES_BY_SOURCE_SET.values().stream()
            .flatMap(Set::stream)
            .collect(Collectors.toSet());

    for (Map.Entry<String, Set<String>> entry : APPROVED_TEST_FILES_BY_SOURCE_SET.entrySet()) {
      for (String relativePath : entry.getValue()) {
        assertThat(isValidApprovedTestSource(root, entry.getKey(), relativePath))
            .as(
                "approved test source must be owned and in src/%s/java: %s",
                entry.getKey(), relativePath)
            .isTrue();
      }
    }
    return approvedFiles;
  }

  private static boolean isValidApprovedTestSource(Path root, String sourceSet, String relativePath)
      throws IOException {
    Path source = root.resolve(relativePath).normalize();
    if (!source.startsWith(root) || !Files.isRegularFile(source)) {
      return false;
    }
    String normalized = root.relativize(source).toString().replace('\\', '/');
    return ownedJavaSources(root).contains(source)
        && normalized.contains("/src/" + sourceSet + "/java/")
        && !isProductionSource(source);
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
