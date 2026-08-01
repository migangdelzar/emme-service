package com.emme.identity;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class IdentityPackageConventionTest {

  private static final Path ROOT_PACKAGE_INFO =
      sourcePath("modules/identity/src/main/java/com/emme/identity/package-info.java");
  private static final Path API_PACKAGE_INFO =
      sourcePath("modules/identity/src/main/java/com/emme/identity/api/package-info.java");
  private static final Path USE_CASE_PACKAGE_INFO =
      sourcePath("modules/identity/src/main/java/com/emme/identity/api/usecase/package-info.java");
  private static final Path RESULT_PACKAGE_INFO =
      sourcePath("modules/identity/src/main/java/com/emme/identity/api/result/package-info.java");
  private static final Path LEGACY_API =
      sourcePath("modules/identity/src/main/java/com/emme/identity/api/IdentityApi.java");
  private static final Path LEGACY_MEMBERSHIP_RESULT =
      sourcePath("modules/identity/src/main/java/com/emme/identity/api/MembershipInfo.java");
  private static final Path LEGACY_USER_RESULT =
      sourcePath("modules/identity/src/main/java/com/emme/identity/api/UserInfo.java");
  private static final Path LEGACY_PERSISTENCE_PACKAGE =
      sourcePath("modules/identity/src/main/java/com/emme/identity/entity");
  private static final Path PERSISTENCE_PACKAGE =
      sourcePath("modules/identity/src/main/java/com/emme/identity/adapter/out/persistence");
  private static final Path PERSISTENCE_ENTITY =
      sourcePath(
          "modules/identity/src/main/java/com/emme/identity/adapter/out/persistence/entity/CustomerIdentityEntity.java");
  private static final Path PERSISTENCE_REPOSITORY =
      sourcePath(
          "modules/identity/src/main/java/com/emme/identity/adapter/out/persistence/repository/SpringDataMembershipRepository.java");
  private static final Path SECURITY_CONFIGURATION =
      sourcePath(
          "modules/identity/src/main/java/com/emme/identity/configuration/SecurityConfiguration.java");
  private static final Path LOGIN_RATE_LIMIT_FILTER =
      sourcePath(
          "modules/identity/src/main/java/com/emme/identity/adapter/in/web/filter/LoginRateLimitFilter.java");
  private static final Path MULTI_REALM_JWT_DECODER =
      sourcePath(
          "modules/identity/src/main/java/com/emme/identity/adapter/out/client/keycloak/MultiRealmJwtDecoder.java");
  private static final Path SECURITY_AUDIT_LOGGER =
      sourcePath(
          "modules/identity/src/main/java/com/emme/identity/adapter/out/observability/SecurityAuditLogger.java");
  private static final Path KEYCLOAK_ADMIN_CLIENT =
      sourcePath(
          "modules/identity/src/main/java/com/emme/identity/adapter/out/client/keycloak/KeycloakAdminClient.java");
  private static final Path REALM_PROVISIONING_PROCESS =
      sourcePath(
          "modules/identity/src/main/java/com/emme/identity/application/process/KeycloakRealmProvisioningProcessManager.java");
  private static final Path IDENTITY_PROVIDER_ADMINISTRATION_PORT =
      sourcePath(
          "modules/identity/src/main/java/com/emme/identity/application/port/out/IdentityProviderAdministrationPort.java");
  private static final Path TENANT_CREATED_CONSUMER =
      sourcePath(
          "modules/identity/src/main/java/com/emme/identity/adapter/in/messaging/consumer/TenantCreatedConsumer.java");
  private static final Path LEGACY_SECURITY_CONFIGURATION =
      sourcePath("modules/identity/src/main/java/com/emme/identity/config/SecurityConfig.java");
  private static final Path LEGACY_INFRASTRUCTURE =
      sourcePath("modules/identity/src/main/java/com/emme/identity/infrastructure");
  private static final Path WEB_CONTROLLER_PACKAGE =
      sourcePath("modules/identity/src/main/java/com/emme/identity/adapter/in/web/controller");
  private static final Path WEB_REQUEST_PACKAGE =
      sourcePath("modules/identity/src/main/java/com/emme/identity/adapter/in/web/request");
  private static final Path WEB_RESPONSE_PACKAGE =
      sourcePath("modules/identity/src/main/java/com/emme/identity/adapter/in/web/response");
  private static final Path WEB_MAPPER_PACKAGE =
      sourcePath("modules/identity/src/main/java/com/emme/identity/adapter/in/web/mapper");
  private static final Path LEGACY_WEB_PACKAGE =
      sourcePath("modules/identity/src/main/java/com/emme/identity/web");
  private static final Path DOMAIN_MODEL_PACKAGE =
      sourcePath("modules/identity/src/main/java/com/emme/identity/domain/model");
  private static final Path APPLICATION_SERVICE_PACKAGE =
      sourcePath("modules/identity/src/main/java/com/emme/identity/application/service");
  private static final Path APPLICATION_PORT_PACKAGE =
      sourcePath("modules/identity/src/main/java/com/emme/identity/application/port/out");
  private static final Path API_USE_CASE_PACKAGE =
      sourcePath("modules/identity/src/main/java/com/emme/identity/api/usecase");
  private static final Path PERSISTENCE_ADAPTER_PACKAGE =
      sourcePath(
          "modules/identity/src/main/java/com/emme/identity/adapter/out/persistence/adapter");
  private static final Path PERSISTENCE_MAPPER_PACKAGE =
      sourcePath("modules/identity/src/main/java/com/emme/identity/adapter/out/persistence/mapper");
  private static final Path LEGACY_MEMBERSHIP_ENTITY =
      sourcePath(
          "modules/identity/src/main/java/com/emme/identity/adapter/out/persistence/entity/Membership.java");
  private static final Path PERMISSION_PERSISTENCE_ADAPTER =
      sourcePath(
          "modules/identity/src/main/java/com/emme/identity/adapter/out/persistence/adapter/PermissionPersistenceAdapter.java");
  private static final Path LEGACY_IDENTITY_SERVICE =
      sourcePath(
          "modules/identity/src/main/java/com/emme/identity/application/IdentityService.java");
  private static final Path FEATURE_FLAG_DOMAIN_PACKAGE =
      sourcePath("modules/identity/src/main/java/com/emme/identity/domain/model");
  private static final Path FEATURE_FLAG_APPLICATION_PORT_PACKAGE =
      sourcePath("modules/identity/src/main/java/com/emme/identity/application/port/out");
  private static final Path FEATURE_FLAG_APPLICATION_SERVICE_PACKAGE =
      sourcePath("modules/identity/src/main/java/com/emme/identity/application/service");
  private static final Path FEATURE_FLAG_ENTITY =
      sourcePath(
          "modules/identity/src/main/java/com/emme/identity/adapter/out/persistence/entity/FeatureFlagEntity.java");
  private static final Path FEATURE_FLAG_REPOSITORY =
      sourcePath(
          "modules/identity/src/main/java/com/emme/identity/adapter/out/persistence/repository/SpringDataFeatureFlagRepository.java");
  private static final Path FEATURE_FLAG_MAPPER =
      sourcePath(
          "modules/identity/src/main/java/com/emme/identity/adapter/out/persistence/mapper/FeatureFlagPersistenceMapper.java");
  private static final Path FEATURE_FLAG_ADAPTER =
      sourcePath(
          "modules/identity/src/main/java/com/emme/identity/adapter/out/persistence/adapter/FeatureFlagPersistenceAdapter.java");
  private static final Path SUBSCRIPTION_PLAN_PORT =
      sourcePath(
          "modules/identity/src/main/java/com/emme/identity/application/port/out/SubscriptionPlanPort.java");
  private static final Path SUBSCRIPTION_PLAN_ADAPTER =
      sourcePath(
          "modules/identity/src/main/java/com/emme/identity/adapter/out/client/subscription/SubscriptionPlanAdapter.java");
  private static final Path LEGACY_FEATURE_FLAG_ENTITY =
      sourcePath(
          "modules/identity/src/main/java/com/emme/identity/adapter/out/persistence/entity/FeatureFlag.java");
  private static final Path CUSTOMER_MEMBERSHIP_ENTITY =
      sourcePath(
          "modules/identity/src/main/java/com/emme/identity/adapter/out/persistence/entity/CustomerMembershipEntity.java");
  private static final Path CUSTOMER_MEMBERSHIP_ID =
      sourcePath(
          "modules/identity/src/main/java/com/emme/identity/adapter/out/persistence/entity/CustomerMembershipId.java");
  private static final Path CUSTOMER_MEMBERSHIP_REPOSITORY =
      sourcePath(
          "modules/identity/src/main/java/com/emme/identity/adapter/out/persistence/repository/SpringDataCustomerMembershipRepository.java");
  private static final Path CUSTOMER_MEMBERSHIP_MAPPER =
      sourcePath(
          "modules/identity/src/main/java/com/emme/identity/adapter/out/persistence/mapper/CustomerMembershipPersistenceMapper.java");
  private static final Path CUSTOMER_MEMBERSHIP_ADAPTER =
      sourcePath(
          "modules/identity/src/main/java/com/emme/identity/adapter/out/persistence/adapter/CustomerMembershipPersistenceAdapter.java");
  private static final Path CUSTOMER_MEMBERSHIP_CONSUMER =
      sourcePath(
          "modules/identity/src/main/java/com/emme/identity/adapter/in/messaging/consumer/AppointmentCreatedConsumer.java");
  private static final Path CUSTOMER_MEMBERSHIP_DOMAIN =
      sourcePath(
          "modules/identity/src/main/java/com/emme/identity/domain/model/CustomerMembership.java");
  private static final Path CUSTOMER_MEMBERSHIP_PORT =
      sourcePath(
          "modules/identity/src/main/java/com/emme/identity/application/port/out/CustomerMembershipRepository.java");
  private static final Path CUSTOMER_MEMBERSHIP_SERVICE =
      sourcePath(
          "modules/identity/src/main/java/com/emme/identity/application/service/EnsureCustomerMembershipService.java");
  private static final Path LEGACY_CUSTOMER_MEMBERSHIP_ENTITY =
      sourcePath(
          "modules/identity/src/main/java/com/emme/identity/adapter/out/persistence/entity/CustomerMembership.java");
  private static final Path LEGACY_CUSTOMER_MEMBERSHIP_REPOSITORY =
      sourcePath(
          "modules/identity/src/main/java/com/emme/identity/adapter/out/persistence/repository/CustomerMembershipRepository.java");
  private static final Path LEGACY_CUSTOMER_MEMBERSHIP_LISTENER =
      sourcePath(
          "modules/identity/src/main/java/com/emme/identity/application/CustomerMembershipListener.java");
  private static final Path CUSTOMER_IDENTITY_DOMAIN =
      sourcePath(
          "modules/identity/src/main/java/com/emme/identity/domain/model/CustomerIdentity.java");
  private static final Path CUSTOMER_IDENTITY_PROVIDER =
      sourcePath(
          "modules/identity/src/main/java/com/emme/identity/domain/model/SocialProvider.java");
  private static final Path CUSTOMER_AUTH_COMMAND =
      sourcePath(
          "modules/identity/src/main/java/com/emme/identity/api/command/AuthenticateCustomerCommand.java");
  private static final Path CUSTOMER_PROFILE_COMMAND =
      sourcePath(
          "modules/identity/src/main/java/com/emme/identity/api/command/UpdateCustomerPhoneCommand.java");
  private static final Path CUSTOMER_DETAILS_RESULT =
      sourcePath(
          "modules/identity/src/main/java/com/emme/identity/api/result/CustomerDetails.java");
  private static final Path CUSTOMER_LOGIN_RESULT =
      sourcePath(
          "modules/identity/src/main/java/com/emme/identity/api/result/CustomerLoginResult.java");
  private static final Path CUSTOMER_AUTH_USE_CASE =
      sourcePath(
          "modules/identity/src/main/java/com/emme/identity/api/usecase/AuthenticateCustomerUseCase.java");
  private static final Path CUSTOMER_PROFILE_USE_CASE =
      sourcePath(
          "modules/identity/src/main/java/com/emme/identity/api/usecase/UpdateCustomerProfileUseCase.java");
  private static final Path CUSTOMER_IDENTITY_PORT =
      sourcePath(
          "modules/identity/src/main/java/com/emme/identity/application/port/out/CustomerIdentityRepository.java");
  private static final Path CUSTOMER_TOKEN_DECODER_PORT =
      sourcePath(
          "modules/identity/src/main/java/com/emme/identity/application/port/out/CustomerTokenDecoder.java");
  private static final Path CUSTOMER_TOKEN_CLAIMS =
      sourcePath(
          "modules/identity/src/main/java/com/emme/identity/application/port/out/CustomerTokenClaims.java");
  private static final Path CUSTOMER_AUTH_SERVICE =
      sourcePath(
          "modules/identity/src/main/java/com/emme/identity/application/service/AuthenticateCustomerService.java");
  private static final Path CUSTOMER_PROFILE_SERVICE =
      sourcePath(
          "modules/identity/src/main/java/com/emme/identity/application/service/UpdateCustomerProfileService.java");
  private static final Path CUSTOMER_IDENTITY_ENTITY =
      sourcePath(
          "modules/identity/src/main/java/com/emme/identity/adapter/out/persistence/entity/CustomerIdentityEntity.java");
  private static final Path CUSTOMER_IDENTITY_REPOSITORY =
      sourcePath(
          "modules/identity/src/main/java/com/emme/identity/adapter/out/persistence/repository/SpringDataCustomerIdentityRepository.java");
  private static final Path CUSTOMER_IDENTITY_MAPPER =
      sourcePath(
          "modules/identity/src/main/java/com/emme/identity/adapter/out/persistence/mapper/CustomerIdentityPersistenceMapper.java");
  private static final Path CUSTOMER_IDENTITY_ADAPTER =
      sourcePath(
          "modules/identity/src/main/java/com/emme/identity/adapter/out/persistence/adapter/CustomerIdentityPersistenceAdapter.java");
  private static final Path CUSTOMER_TOKEN_ADAPTER =
      sourcePath(
          "modules/identity/src/main/java/com/emme/identity/adapter/out/client/keycloak/CustomerTokenDecoderAdapter.java");
  private static final Path LEGACY_CUSTOMER_AUTH_SERVICE =
      sourcePath(
          "modules/identity/src/main/java/com/emme/identity/application/CustomerAuthService.java");
  private static final Path LEGACY_CUSTOMER_IDENTITY_ENTITY =
      sourcePath(
          "modules/identity/src/main/java/com/emme/identity/adapter/out/persistence/entity/CustomerIdentity.java");
  private static final Path LEGACY_CUSTOMER_IDENTITY_REPOSITORY =
      sourcePath(
          "modules/identity/src/main/java/com/emme/identity/adapter/out/persistence/repository/CustomerIdentityRepository.java");
  private static final Path IDENTITY_SECURITY_PROPERTIES =
      sourcePath(
          "modules/identity/src/main/java/com/emme/identity/configuration/IdentitySecurityProperties.java");
  private static final Path IDENTITY_EXCEPTION_HANDLER =
      sourcePath(
          "modules/identity/src/main/java/com/emme/identity/adapter/in/web/advice/IdentityExceptionHandler.java");
  private static final Path IDENTITY_EXCEPTION_PACKAGE =
      sourcePath("modules/identity/src/main/java/com/emme/identity/api/exception");
  private static final Path USER_AUTH_USE_CASE =
      sourcePath(
          "modules/identity/src/main/java/com/emme/identity/api/usecase/AuthenticateUserUseCase.java");
  private static final Path USER_AUTH_SERVICE =
      sourcePath(
          "modules/identity/src/main/java/com/emme/identity/application/service/AuthenticateUserService.java");
  private static final Path USER_AUTH_PORT =
      sourcePath(
          "modules/identity/src/main/java/com/emme/identity/application/port/out/UserAuthenticationPort.java");
  private static final Path KEYCLOAK_USER_ADAPTER =
      sourcePath(
          "modules/identity/src/main/java/com/emme/identity/adapter/out/client/keycloak/KeycloakUserAuthenticationAdapter.java");
  private static final Path IDENTITY_CLIENT_CONFIGURATION =
      sourcePath(
          "modules/identity/src/main/java/com/emme/identity/configuration/IdentityClientConfiguration.java");
  private static final Path IDENTITY_KEYCLOAK_PROPERTIES =
      sourcePath(
          "modules/identity/src/main/java/com/emme/identity/configuration/IdentityKeycloakProperties.java");
  private static final Path LEGACY_KEYCLOAK_AUTH_SERVICE =
      sourcePath(
          "modules/identity/src/main/java/com/emme/identity/application/KeycloakAuthService.java");

  @Test
  void keepsModuleMetadataAndExplicitAllowedDependencies() throws IOException {
    String source = Files.readString(ROOT_PACKAGE_INFO);

    assertThat(source).contains("@org.springframework.modulith.ApplicationModule");
    assertThat(source).contains("\"tenancy :: tenant-api\"");
    assertThat(source).contains("\"tenancy :: tenant-events\"");
  }

  @Test
  void groupsPublicContractsByKind() throws IOException {
    assertThat(Files.readString(API_PACKAGE_INFO))
        .doesNotContain("@org.springframework.modulith.NamedInterface");
    assertThat(Files.readString(USE_CASE_PACKAGE_INFO))
        .contains("@org.springframework.modulith.NamedInterface(\"identity-api\")");
    assertThat(Files.readString(RESULT_PACKAGE_INFO))
        .contains("@org.springframework.modulith.NamedInterface(\"identity-api\")");
  }

  @Test
  void removesLegacyUngroupedContractFiles() {
    assertThat(Files.exists(LEGACY_API)).isFalse();
    assertThat(Files.exists(LEGACY_MEMBERSHIP_RESULT)).isFalse();
    assertThat(Files.exists(LEGACY_USER_RESULT)).isFalse();
  }

  @Test
  void ownsPersistenceTypesUnderOutboundPersistence() {
    assertThat(hasJavaSources(LEGACY_PERSISTENCE_PACKAGE)).isFalse();
    assertThat(Files.exists(PERSISTENCE_PACKAGE)).isTrue();
    assertThat(Files.exists(PERSISTENCE_ENTITY)).isTrue();
    assertThat(Files.exists(PERSISTENCE_REPOSITORY)).isTrue();
  }

  @Test
  void ownsSecurityAdaptersUnderCanonicalBoundaries() {
    assertThat(Files.exists(SECURITY_CONFIGURATION)).isTrue();
    assertThat(Files.exists(LOGIN_RATE_LIMIT_FILTER)).isTrue();
    assertThat(Files.exists(MULTI_REALM_JWT_DECODER)).isTrue();
    assertThat(Files.exists(SECURITY_AUDIT_LOGGER)).isTrue();
    assertThat(Files.exists(KEYCLOAK_ADMIN_CLIENT)).isTrue();
    assertThat(Files.exists(REALM_PROVISIONING_PROCESS)).isTrue();
    assertThat(Files.exists(TENANT_CREATED_CONSUMER)).isTrue();
    assertThat(Files.exists(LEGACY_SECURITY_CONFIGURATION)).isFalse();
    assertThat(hasJavaSources(LEGACY_INFRASTRUCTURE)).isFalse();
  }

  @Test
  void ownsHttpEntryPointsAndWireModelsUnderInboundWebAdapters() {
    assertThat(hasJavaSource(WEB_CONTROLLER_PACKAGE, "IdentityController.java")).isTrue();
    assertThat(hasJavaSource(WEB_CONTROLLER_PACKAGE, "AuthController.java")).isTrue();
    assertThat(hasJavaSource(WEB_CONTROLLER_PACKAGE, "CurrentUserController.java")).isTrue();
    assertThat(hasJavaSource(WEB_CONTROLLER_PACKAGE, "FeatureFlagController.java")).isTrue();
    assertThat(hasJavaSource(WEB_CONTROLLER_PACKAGE, "TenantFeatureFlagController.java")).isTrue();

    assertThat(hasJavaSource(WEB_REQUEST_PACKAGE, "LoginRequest.java")).isTrue();
    assertThat(hasJavaSource(WEB_REQUEST_PACKAGE, "AssignMembershipRequest.java")).isTrue();
    assertThat(hasJavaSource(WEB_REQUEST_PACKAGE, "CreateFeatureFlagRequest.java")).isTrue();
    assertThat(hasJavaSource(WEB_REQUEST_PACKAGE, "UpdateFeatureFlagRequest.java")).isTrue();
    assertThat(hasJavaSource(WEB_REQUEST_PACKAGE, "OverrideFeatureFlagRequest.java")).isTrue();

    assertThat(hasJavaSource(WEB_RESPONSE_PACKAGE, "CurrentUserResponse.java")).isTrue();
    assertThat(hasJavaSource(WEB_RESPONSE_PACKAGE, "TenantMembershipResponse.java")).isTrue();
    assertThat(hasJavaSource(WEB_RESPONSE_PACKAGE, "BusinessProfileResponse.java")).isTrue();
    assertThat(hasJavaSource(WEB_RESPONSE_PACKAGE, "MembershipResponse.java")).isTrue();
    assertThat(hasJavaSource(WEB_RESPONSE_PACKAGE, "FeatureFlagResponse.java")).isTrue();
    assertThat(hasJavaSource(WEB_RESPONSE_PACKAGE, "TokenLoginResponse.java")).isTrue();
    assertThat(hasJavaSource(WEB_RESPONSE_PACKAGE, "CustomerLoginResponse.java")).isTrue();
    assertThat(hasJavaSource(WEB_RESPONSE_PACKAGE, "CustomerProfileResponse.java")).isTrue();

    assertThat(hasJavaSource(WEB_MAPPER_PACKAGE, "IdentityWebMapper.java")).isTrue();
    assertThat(hasJavaSource(WEB_MAPPER_PACKAGE, "FeatureFlagWebMapper.java")).isTrue();
    assertThat(hasJavaSource(WEB_MAPPER_PACKAGE, "CustomerWebMapper.java")).isTrue();
    assertThat(hasJavaSources(LEGACY_WEB_PACKAGE)).isFalse();
  }

  @Test
  void ownsMembershipBusinessBehaviorAndPersistenceBehindApplicationPorts() {
    assertThat(hasJavaSource(DOMAIN_MODEL_PACKAGE, "Membership.java")).isTrue();
    assertThat(hasJavaSource(DOMAIN_MODEL_PACKAGE, "MembershipStatus.java")).isTrue();
    assertThat(hasJavaSource(APPLICATION_SERVICE_PACKAGE, "MembershipService.java")).isTrue();
    assertThat(hasJavaSource(APPLICATION_PORT_PACKAGE, "MembershipRepository.java")).isTrue();
    assertThat(hasJavaSource(APPLICATION_PORT_PACKAGE, "RoleRepository.java")).isTrue();
    assertThat(hasJavaSource(PERSISTENCE_ADAPTER_PACKAGE, "MembershipPersistenceAdapter.java"))
        .isTrue();
    assertThat(hasJavaSource(PERSISTENCE_MAPPER_PACKAGE, "MembershipPersistenceMapper.java"))
        .isTrue();
    assertThat(Files.exists(LEGACY_MEMBERSHIP_ENTITY)).isFalse();
  }

  @Test
  void ownsPermissionResolutionBehindAnApplicationUseCaseAndPort() {
    assertThat(hasJavaSource(API_USE_CASE_PACKAGE, "GetUserPermissionsUseCase.java")).isTrue();
    assertThat(hasJavaSource(APPLICATION_PORT_PACKAGE, "PermissionPort.java")).isTrue();
    assertThat(hasJavaSource(APPLICATION_SERVICE_PACKAGE, "GetUserPermissionsService.java"))
        .isTrue();
    assertThat(Files.exists(PERMISSION_PERSISTENCE_ADAPTER)).isTrue();
    assertThat(Files.exists(LEGACY_IDENTITY_SERVICE)).isFalse();
  }

  @Test
  void ownsFeatureFlagsBehindDomainApplicationAndPersistenceBoundaries() {
    assertThat(hasJavaSource(FEATURE_FLAG_DOMAIN_PACKAGE, "FeatureFlag.java")).isTrue();
    assertThat(hasJavaSource(FEATURE_FLAG_APPLICATION_PORT_PACKAGE, "FeatureFlagRepository.java"))
        .isTrue();
    assertThat(hasJavaSource(FEATURE_FLAG_APPLICATION_PORT_PACKAGE, "SubscriptionPlanPort.java"))
        .isTrue();
    assertThat(hasJavaSource(FEATURE_FLAG_APPLICATION_SERVICE_PACKAGE, "FeatureFlagService.java"))
        .isTrue();
    assertThat(Files.exists(FEATURE_FLAG_ENTITY)).isTrue();
    assertThat(Files.exists(FEATURE_FLAG_REPOSITORY)).isTrue();
    assertThat(Files.exists(FEATURE_FLAG_MAPPER)).isTrue();
    assertThat(Files.exists(FEATURE_FLAG_ADAPTER)).isTrue();
    assertThat(Files.exists(SUBSCRIPTION_PLAN_PORT)).isTrue();
    assertThat(Files.exists(SUBSCRIPTION_PLAN_ADAPTER)).isTrue();
    assertThat(Files.exists(LEGACY_FEATURE_FLAG_ENTITY)).isFalse();
  }

  @Test
  void ownsCustomerMembershipEventsBehindApplicationAndAdapterBoundaries() {
    assertThat(Files.exists(CUSTOMER_MEMBERSHIP_DOMAIN)).isTrue();
    assertThat(Files.exists(CUSTOMER_MEMBERSHIP_PORT)).isTrue();
    assertThat(Files.exists(CUSTOMER_MEMBERSHIP_SERVICE)).isTrue();
    assertThat(Files.exists(CUSTOMER_MEMBERSHIP_ENTITY)).isTrue();
    assertThat(Files.exists(CUSTOMER_MEMBERSHIP_ID)).isTrue();
    assertThat(Files.exists(CUSTOMER_MEMBERSHIP_REPOSITORY)).isTrue();
    assertThat(Files.exists(CUSTOMER_MEMBERSHIP_MAPPER)).isTrue();
    assertThat(Files.exists(CUSTOMER_MEMBERSHIP_ADAPTER)).isTrue();
    assertThat(Files.exists(CUSTOMER_MEMBERSHIP_CONSUMER)).isTrue();
    assertThat(Files.exists(LEGACY_CUSTOMER_MEMBERSHIP_ENTITY)).isFalse();
    assertThat(Files.exists(LEGACY_CUSTOMER_MEMBERSHIP_REPOSITORY)).isFalse();
    assertThat(Files.exists(LEGACY_CUSTOMER_MEMBERSHIP_LISTENER)).isFalse();
  }

  @Test
  void ownsCustomerAuthenticationBehindPublicApplicationAndTechnicalBoundaries() {
    assertThat(Files.exists(CUSTOMER_IDENTITY_DOMAIN)).isTrue();
    assertThat(Files.exists(CUSTOMER_IDENTITY_PROVIDER)).isTrue();
    assertThat(Files.exists(CUSTOMER_AUTH_COMMAND)).isTrue();
    assertThat(Files.exists(CUSTOMER_PROFILE_COMMAND)).isTrue();
    assertThat(Files.exists(CUSTOMER_DETAILS_RESULT)).isTrue();
    assertThat(Files.exists(CUSTOMER_LOGIN_RESULT)).isTrue();
    assertThat(Files.exists(CUSTOMER_AUTH_USE_CASE)).isTrue();
    assertThat(Files.exists(CUSTOMER_PROFILE_USE_CASE)).isTrue();
    assertThat(Files.exists(CUSTOMER_IDENTITY_PORT)).isTrue();
    assertThat(Files.exists(CUSTOMER_TOKEN_DECODER_PORT)).isTrue();
    assertThat(Files.exists(CUSTOMER_TOKEN_CLAIMS)).isTrue();
    assertThat(Files.exists(CUSTOMER_AUTH_SERVICE)).isTrue();
    assertThat(Files.exists(CUSTOMER_PROFILE_SERVICE)).isTrue();
    assertThat(Files.exists(CUSTOMER_IDENTITY_ENTITY)).isTrue();
    assertThat(Files.exists(CUSTOMER_IDENTITY_REPOSITORY)).isTrue();
    assertThat(Files.exists(CUSTOMER_IDENTITY_MAPPER)).isTrue();
    assertThat(Files.exists(CUSTOMER_IDENTITY_ADAPTER)).isTrue();
    assertThat(Files.exists(CUSTOMER_TOKEN_ADAPTER)).isTrue();
    assertThat(Files.exists(LEGACY_CUSTOMER_AUTH_SERVICE)).isFalse();
    assertThat(Files.exists(LEGACY_CUSTOMER_IDENTITY_ENTITY)).isFalse();
    assertThat(Files.exists(LEGACY_CUSTOMER_IDENTITY_REPOSITORY)).isFalse();
  }

  @Test
  void ownsSecurityDefaultsInTypedIdentityConfiguration() {
    assertThat(Files.exists(IDENTITY_SECURITY_PROPERTIES)).isTrue();
  }

  @Test
  void ownsExpectedFailuresAndHttpTranslationAtIdentityBoundaries() {
    assertThat(Files.exists(IDENTITY_EXCEPTION_HANDLER)).isTrue();
    assertThat(hasJavaSource(IDENTITY_EXCEPTION_PACKAGE, "CustomerNotFoundException.java"))
        .isTrue();
    assertThat(hasJavaSource(IDENTITY_EXCEPTION_PACKAGE, "InvalidCustomerTokenException.java"))
        .isTrue();
  }

  @Test
  void ownsPasswordAuthenticationBehindApplicationAndKeycloakAdapterBoundaries() {
    assertThat(Files.exists(USER_AUTH_USE_CASE)).isTrue();
    assertThat(Files.exists(USER_AUTH_SERVICE)).isTrue();
    assertThat(Files.exists(USER_AUTH_PORT)).isTrue();
    assertThat(Files.exists(KEYCLOAK_USER_ADAPTER)).isTrue();
    assertThat(Files.exists(IDENTITY_CLIENT_CONFIGURATION)).isTrue();
    assertThat(Files.exists(LEGACY_KEYCLOAK_AUTH_SERVICE)).isFalse();
  }

  @Test
  void keepsKeycloakClientSettingsTypedAndAdapterOwned() throws IOException {
    String userAdapter = Files.readString(KEYCLOAK_USER_ADAPTER);
    String adminAdapter = Files.readString(KEYCLOAK_ADMIN_CLIENT);

    assertThat(Files.exists(IDENTITY_KEYCLOAK_PROPERTIES)).isTrue();
    assertThat(userAdapter).doesNotContain("@Value");
    assertThat(adminAdapter).doesNotContain("@Value");
  }

  @Test
  void keepsRealmProvisioningDependentOnAnApplicationPort() throws IOException {
    String processSource = Files.readString(REALM_PROVISIONING_PROCESS);

    assertThat(Files.exists(IDENTITY_PROVIDER_ADMINISTRATION_PORT)).isTrue();
    assertThat(processSource).contains("IdentityProviderAdministrationPort");
    assertThat(processSource).doesNotContain("KeycloakAdminClient");
    assertThat(processSource).doesNotContain("admin123");
    assertThat(processSource).doesNotContain("Thread.sleep");
  }

  private static boolean hasJavaSource(Path directory, String filename) {
    return Files.exists(directory.resolve(filename));
  }

  private static boolean hasJavaSources(Path directory) {
    if (!Files.isDirectory(directory)) return false;
    try (var files = Files.walk(directory)) {
      return files.anyMatch(path -> path.toString().endsWith(".java"));
    } catch (IOException e) {
      throw new IllegalStateException("Cannot inspect source tree: " + directory, e);
    }
  }

  private static Path sourcePath(String relativePath) {
    Path current = Path.of("").toAbsolutePath();
    for (int attempt = 0; attempt < 8 && current != null; attempt++) {
      if (Files.exists(current.resolve("settings.gradle.kts"))) {
        return current.resolve(relativePath);
      }
      current = current.getParent();
    }
    throw new IllegalStateException("Cannot locate repository source: " + relativePath);
  }
}
