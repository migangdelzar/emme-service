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
  private static final Path LEGACY_USE_CASE_API =
      sourcePath("modules/identity/src/main/java/com/emme/identity/api/usecase/IdentityApi.java");
  private static final Path LEGACY_USE_CASE_SERVICE =
      sourcePath(
          "modules/identity/src/main/java/com/emme/identity/application/service/IdentityApiService.java");
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
  private static final Path ROLE_ENTITY =
      sourcePath(
          "modules/identity/src/main/java/com/emme/identity/adapter/out/persistence/entity/RoleEntity.java");
  private static final Path PERMISSION_ENTITY =
      sourcePath(
          "modules/identity/src/main/java/com/emme/identity/adapter/out/persistence/entity/PermissionEntity.java");
  private static final Path ROLE_PERMISSION_ENTITY =
      sourcePath(
          "modules/identity/src/main/java/com/emme/identity/adapter/out/persistence/entity/RolePermissionEntity.java");
  private static final Path LEGACY_ROLE =
      sourcePath(
          "modules/identity/src/main/java/com/emme/identity/adapter/out/persistence/entity/Role.java");
  private static final Path LEGACY_PERMISSION =
      sourcePath(
          "modules/identity/src/main/java/com/emme/identity/adapter/out/persistence/entity/Permission.java");
  private static final Path LEGACY_ROLE_PERMISSION =
      sourcePath(
          "modules/identity/src/main/java/com/emme/identity/adapter/out/persistence/entity/RolePermission.java");
  private static final Path PERSISTENCE_REPOSITORY =
      sourcePath(
          "modules/identity/src/main/java/com/emme/identity/adapter/out/persistence/repository/SpringDataMembershipRepository.java");
  private static final Path SECURITY_CONFIGURATION =
      sourcePath(
          "modules/identity/src/main/java/com/emme/identity/configuration/SecurityConfiguration.java");
  private static final Path LOGIN_RATE_LIMIT_FILTER =
      sourcePath(
          "modules/identity/src/main/java/com/emme/identity/adapter/in/web/filter/LoginRateLimitFilter.java");
  private static final Path LOGIN_ATTEMPT_RATE_LIMITER =
      sourcePath(
          "modules/identity/src/main/java/com/emme/identity/application/port/out/LoginAttemptRateLimiter.java");
  private static final Path MULTI_REALM_JWT_DECODER =
      sourcePath(
          "modules/identity/src/main/java/com/emme/identity/adapter/out/client/keycloak/MultiRealmJwtDecoder.java");
  private static final Path JWT_TRUST_POLICY =
      sourcePath(
          "modules/identity/src/main/java/com/emme/identity/adapter/out/client/keycloak/IdentityJwtTrustPolicy.java");
  private static final Path SECURITY_AUDIT_LOGGER =
      sourcePath(
          "modules/identity/src/main/java/com/emme/identity/adapter/out/observability/SecurityAuditLogger.java");
  private static final Path KEYCLOAK_ADMIN_CLIENT =
      sourcePath(
          "modules/identity/src/main/java/com/emme/identity/adapter/out/client/keycloak/KeycloakAdminClient.java");
  private static final Path REALM_PROVISIONING_PROCESS =
      sourcePath(
          "modules/identity/src/main/java/com/emme/identity/application/process/KeycloakRealmProvisioningProcessManager.java");
  private static final Path PROVISION_TENANT_IDENTITY_USE_CASE =
      sourcePath(
          "modules/identity/src/main/java/com/emme/identity/api/usecase/ProvisionTenantIdentityUseCase.java");
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
  private static final Path IDENTITY_CONTROLLER =
      sourcePath(
          "modules/identity/src/main/java/com/emme/identity/adapter/in/web/controller/IdentityController.java");
  private static final Path CURRENT_USER_CONTROLLER =
      sourcePath(
          "modules/identity/src/main/java/com/emme/identity/adapter/in/web/controller/CurrentUserController.java");
  private static final Path IDENTITY_WEB_MAPPER =
      sourcePath(
          "modules/identity/src/main/java/com/emme/identity/adapter/in/web/mapper/IdentityWebMapper.java");
  private static final Path FEATURE_FLAG_CONTROLLER =
      sourcePath(
          "modules/identity/src/main/java/com/emme/identity/adapter/in/web/controller/FeatureFlagController.java");
  private static final Path TENANT_FEATURE_FLAG_CONTROLLER =
      sourcePath(
          "modules/identity/src/main/java/com/emme/identity/adapter/in/web/controller/TenantFeatureFlagController.java");
  private static final Path FEATURE_FLAG_WEB_MAPPER =
      sourcePath(
          "modules/identity/src/main/java/com/emme/identity/adapter/in/web/mapper/FeatureFlagWebMapper.java");
  private static final Path IDENTITY_EXCEPTION_PACKAGE =
      sourcePath("modules/identity/src/main/java/com/emme/identity/api/exception");
  private static final Path INVALID_MEMBERSHIP_ROLE_EXCEPTION =
      sourcePath(
          "modules/identity/src/main/java/com/emme/identity/api/exception/InvalidMembershipRoleException.java");
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
  private static final Path USER_CONTEXT_SECURITY_PACKAGE =
      sourcePath(
          "modules/identity/src/main/java/com/emme/identity/adapter/in/web/security/UserContext.java");
  private static final Path USER_CONTEXT_HOLDER_SECURITY_PACKAGE =
      sourcePath(
          "modules/identity/src/main/java/com/emme/identity/adapter/in/web/security/UserContextHolder.java");
  private static final Path LEGACY_USER_CONTEXT =
      sourcePath("modules/identity/src/main/java/com/emme/identity/UserContext.java");
  private static final Path LEGACY_USER_CONTEXT_HOLDER =
      sourcePath("modules/identity/src/main/java/com/emme/identity/UserContextHolder.java");
  private static final Path IDENTITY_AUTHORIZATION_CONFIGURATION =
      sourcePath(
          "modules/identity/src/main/java/com/emme/identity/configuration/IdentityAuthorizationConfiguration.java");
  private static final Path IDENTITY_ROLE_AUTHORITY_MAPPER =
      sourcePath(
          "modules/identity/src/main/java/com/emme/identity/adapter/in/web/security/IdentityRoleAuthorityMapper.java");
  private static final Path IDENTITY_JWT_AUTHORITIES_CONVERTER =
      sourcePath(
          "modules/identity/src/main/java/com/emme/identity/adapter/in/web/security/IdentityJwtAuthoritiesConverter.java");
  private static final Path IDENTITY_USER_AUTHORITIES_MAPPER =
      sourcePath(
          "modules/identity/src/main/java/com/emme/identity/adapter/in/web/security/IdentityUserAuthoritiesMapper.java");
  private static final Path ROLE_DOMAIN_MODEL =
      sourcePath("modules/identity/src/main/java/com/emme/identity/domain/model/Role.java");
  private static final Path PERMISSION_DOMAIN_MODEL =
      sourcePath("modules/identity/src/main/java/com/emme/identity/domain/model/Permission.java");
  private static final Path ROLE_SCOPE_DOMAIN_MODEL =
      sourcePath("modules/identity/src/main/java/com/emme/identity/domain/model/RoleScope.java");
  private static final Path ROLE_PERSISTENCE_MAPPER =
      sourcePath(
          "modules/identity/src/main/java/com/emme/identity/adapter/out/persistence/mapper/RolePersistenceMapper.java");
  private static final Path PERMISSION_PERSISTENCE_MAPPER =
      sourcePath(
          "modules/identity/src/main/java/com/emme/identity/adapter/out/persistence/mapper/PermissionPersistenceMapper.java");
  private static final Path LEGACY_ROLE_SCOPE =
      sourcePath(
          "modules/identity/src/main/java/com/emme/identity/adapter/out/persistence/entity/RoleScope.java");

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
  void namesJpaRoleAndPermissionRepresentationsAsEntities() {
    assertThat(Files.exists(ROLE_ENTITY)).isTrue();
    assertThat(Files.exists(PERMISSION_ENTITY)).isTrue();
    assertThat(Files.exists(ROLE_PERMISSION_ENTITY)).isTrue();
    assertThat(Files.exists(LEGACY_ROLE)).isFalse();
    assertThat(Files.exists(LEGACY_PERMISSION)).isFalse();
    assertThat(Files.exists(LEGACY_ROLE_PERMISSION)).isFalse();
  }

  @Test
  void ownsRoleAndPermissionBusinessModelsOutsidePersistence() {
    assertThat(Files.exists(ROLE_DOMAIN_MODEL)).isTrue();
    assertThat(Files.exists(PERMISSION_DOMAIN_MODEL)).isTrue();
    assertThat(Files.exists(ROLE_SCOPE_DOMAIN_MODEL)).isTrue();
    assertThat(Files.exists(ROLE_PERSISTENCE_MAPPER)).isTrue();
    assertThat(Files.exists(PERMISSION_PERSISTENCE_MAPPER)).isTrue();
    assertThat(Files.exists(LEGACY_ROLE_SCOPE)).isFalse();
  }

  @Test
  void ownsSecurityAdaptersUnderCanonicalBoundaries() throws IOException {
    assertThat(Files.exists(SECURITY_CONFIGURATION)).isTrue();
    assertThat(Files.exists(LOGIN_RATE_LIMIT_FILTER)).isTrue();
    assertThat(Files.exists(MULTI_REALM_JWT_DECODER)).isTrue();
    assertThat(Files.exists(SECURITY_AUDIT_LOGGER)).isTrue();
    assertThat(Files.exists(KEYCLOAK_ADMIN_CLIENT)).isTrue();
    assertThat(Files.exists(REALM_PROVISIONING_PROCESS)).isTrue();
    assertThat(Files.exists(TENANT_CREATED_CONSUMER)).isTrue();
    String tenantCreatedConsumer = Files.readString(TENANT_CREATED_CONSUMER);
    assertThat(tenantCreatedConsumer).contains("@ApplicationModuleListener");
    assertThat(tenantCreatedConsumer).doesNotContain("@EventListener");
    assertThat(tenantCreatedConsumer).contains("ProvisionTenantIdentityUseCase");
    assertThat(tenantCreatedConsumer).doesNotContain("KeycloakRealmProvisioningProcessManager");
    assertThat(Files.exists(LEGACY_SECURITY_CONFIGURATION)).isFalse();
    assertThat(hasJavaSources(LEGACY_INFRASTRUCTURE)).isFalse();
  }

  @Test
  void keepsExceptionAdviceIndependentFromConcreteControllers() throws IOException {
    String source = Files.readString(IDENTITY_EXCEPTION_HANDLER);

    assertThat(source)
        .contains(
            "@RestControllerAdvice(basePackages = \"com.emme.identity.adapter.in.web.controller\")");
    assertThat(source)
        .doesNotContain("import com.emme.identity.adapter.in.web.controller.IdentityController;");
    assertThat(source).doesNotContain("basePackageClasses");
  }

  @Test
  void keepsLoginRateLimitStateOutsideTheInboundFilter() throws IOException {
    String source = Files.readString(LOGIN_RATE_LIMIT_FILTER);

    assertThat(Files.exists(LOGIN_ATTEMPT_RATE_LIMITER)).isTrue();
    assertThat(source).contains("LoginAttemptRateLimiter");
    assertThat(source).doesNotContain("ConcurrentHashMap");
  }

  @Test
  void keepsMembershipWebAdaptersIndependentFromApplicationImplementations() throws IOException {
    assertThat(Files.readString(IDENTITY_CONTROLLER))
        .doesNotContain("com.emme.identity.application.service.MembershipService")
        .doesNotContain("com.emme.identity.domain.model.Membership");
    assertThat(Files.readString(CURRENT_USER_CONTROLLER))
        .doesNotContain("com.emme.identity.application.service.MembershipService")
        .doesNotContain("com.emme.identity.domain.model.Membership");
    assertThat(Files.readString(IDENTITY_WEB_MAPPER))
        .doesNotContain("com.emme.identity.domain.model.Membership");
  }

  @Test
  void keepsFeatureFlagWebAdaptersIndependentFromApplicationImplementations() throws IOException {
    assertThat(Files.readString(FEATURE_FLAG_CONTROLLER))
        .doesNotContain("com.emme.identity.application.service.FeatureFlagService")
        .doesNotContain("com.emme.identity.domain.model.FeatureFlag");
    assertThat(Files.readString(TENANT_FEATURE_FLAG_CONTROLLER))
        .doesNotContain("com.emme.identity.application.service.FeatureFlagService")
        .doesNotContain("com.emme.identity.domain.model.FeatureFlag");
    assertThat(Files.readString(FEATURE_FLAG_WEB_MAPPER))
        .doesNotContain("com.emme.identity.domain.model.FeatureFlag");
  }

  @Test
  void keepsAppointmentConsumerIndependentFromApplicationImplementations() throws IOException {
    Path appointmentConsumer =
        sourcePath(
            "modules/identity/src/main/java/com/emme/identity/adapter/in/messaging/consumer/AppointmentCreatedConsumer.java");

    assertThat(Files.readString(appointmentConsumer))
        .doesNotContain("com.emme.identity.application.service.EnsureCustomerMembershipService")
        .doesNotContain("com.emme.identity.domain.model.CustomerMembership");
  }

  @Test
  void keepsAuthenticationApplicationServiceIndependentFromConfigurationProperties()
      throws IOException {
    Path authenticateUserService =
        sourcePath(
            "modules/identity/src/main/java/com/emme/identity/application/service/AuthenticateUserService.java");

    assertThat(Files.readString(authenticateUserService))
        .doesNotContain("com.emme.identity.configuration.IdentityKeycloakProperties")
        .doesNotContain("org.springframework.boot.context.properties");
  }

  @Test
  void keepsProvisioningProcessIndependentFromConfigurationProperties() throws IOException {
    Path provisioningProcess =
        sourcePath(
            "modules/identity/src/main/java/com/emme/identity/application/process/KeycloakRealmProvisioningProcessManager.java");

    assertThat(Files.readString(provisioningProcess))
        .doesNotContain("com.emme.identity.configuration.IdentityRealmProvisioningProperties")
        .doesNotContain("org.springframework.boot.context.properties");
  }

  @Test
  void keepsProvisioningUseCaseIndependentFromTenancyEventTransport() throws IOException {
    Path provisioningUseCase =
        sourcePath(
            "modules/identity/src/main/java/com/emme/identity/api/usecase/ProvisionTenantIdentityUseCase.java");

    assertThat(Files.readString(provisioningUseCase))
        .doesNotContain("com.emme.tenancy.api.event.TenantCreated");
  }

  @Test
  void keepsProvisioningProcessIndependentFromTenantModuleService() throws IOException {
    Path provisioningProcess =
        sourcePath(
            "modules/identity/src/main/java/com/emme/identity/application/process/KeycloakRealmProvisioningProcessManager.java");

    assertThat(Files.readString(provisioningProcess))
        .doesNotContain("com.emme.tenancy.api.usecase.TenantApi");
  }

  @Test
  void removesLegacyIdentityApiContractsAndImplementation() {
    assertThat(Files.exists(LEGACY_API)).isFalse();
    assertThat(Files.exists(LEGACY_USE_CASE_API)).isFalse();
    assertThat(Files.exists(LEGACY_USE_CASE_SERVICE)).isFalse();
  }

  @Test
  void eachIdentityApplicationServiceImplementsAtMostOneUseCase() throws IOException {
    try (var files = Files.walk(APPLICATION_SERVICE_PACKAGE)) {
      files
          .filter(path -> path.toString().endsWith("Service.java"))
          .forEach(
              path ->
                  assertThat(read(path))
                      .as("one use case per application service: %s", path)
                      .doesNotMatch("(?s).*implements\\s+[^\\{]*UseCase\\s*,.*"));
    }
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

  private static String read(Path path) {
    try {
      return Files.readString(path);
    } catch (IOException exception) {
      throw new IllegalStateException("Unable to read Identity source " + path, exception);
    }
  }

  @Test
  void ownsMembershipBusinessBehaviorAndPersistenceBehindApplicationPorts() {
    assertThat(hasJavaSource(DOMAIN_MODEL_PACKAGE, "Membership.java")).isTrue();
    assertThat(hasJavaSource(DOMAIN_MODEL_PACKAGE, "MembershipStatus.java")).isTrue();
    assertThat(hasJavaSource(APPLICATION_SERVICE_PACKAGE, "AssignMembershipService.java")).isTrue();
    assertThat(hasJavaSource(APPLICATION_SERVICE_PACKAGE, "GetCurrentUserMembershipsService.java"))
        .isTrue();
    assertThat(hasJavaSource(APPLICATION_SERVICE_PACKAGE, "RevokeMembershipService.java")).isTrue();
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
    assertThat(
            hasJavaSource(
                FEATURE_FLAG_APPLICATION_SERVICE_PACKAGE, "GetEffectiveFeatureFlagsService.java"))
        .isTrue();
    assertThat(hasJavaSource(API_USE_CASE_PACKAGE, "ListPlatformFeatureFlagsUseCase.java"))
        .isTrue();
    assertThat(
            hasJavaSource(
                FEATURE_FLAG_APPLICATION_SERVICE_PACKAGE, "ListPlatformFeatureFlagsService.java"))
        .isTrue();
    assertThat(
            hasJavaSource(
                FEATURE_FLAG_APPLICATION_SERVICE_PACKAGE, "SetPlatformFeatureFlagService.java"))
        .isTrue();
    assertThat(
            hasJavaSource(
                FEATURE_FLAG_APPLICATION_SERVICE_PACKAGE,
                "SetTenantFeatureFlagOverrideService.java"))
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
  void ownsUserSecurityContextUnderInboundWebSecurity() {
    assertThat(Files.exists(USER_CONTEXT_SECURITY_PACKAGE)).isTrue();
    assertThat(Files.exists(USER_CONTEXT_HOLDER_SECURITY_PACKAGE)).isTrue();
    assertThat(Files.exists(LEGACY_USER_CONTEXT)).isFalse();
    assertThat(Files.exists(LEGACY_USER_CONTEXT_HOLDER)).isFalse();
  }

  @Test
  void exposesOnlyTheExplicitSecurityNamedInterface() throws IOException {
    Path packageInfo =
        sourcePath(
            "modules/identity/src/main/java/com/emme/identity/adapter/in/web/security/package-info.java");

    assertThat(Files.readString(packageInfo))
        .contains("@org.springframework.modulith.NamedInterface(\"identity-security\")");
  }

  @Test
  void keepsAuthorizationMappingOutsideSecurityChainWiring() throws IOException {
    String securityConfiguration = Files.readString(SECURITY_CONFIGURATION);

    assertThat(Files.exists(IDENTITY_AUTHORIZATION_CONFIGURATION)).isTrue();
    assertThat(Files.exists(IDENTITY_ROLE_AUTHORITY_MAPPER)).isTrue();
    assertThat(Files.exists(IDENTITY_JWT_AUTHORITIES_CONVERTER)).isTrue();
    assertThat(Files.exists(IDENTITY_USER_AUTHORITIES_MAPPER)).isTrue();
    assertThat(securityConfiguration).doesNotContain("RoleHierarchyImpl");
    assertThat(securityConfiguration).doesNotContain("SimpleGrantedAuthority");
    assertThat(securityConfiguration).doesNotContain("realm_access");
  }

  @Test
  void ownsExpectedFailuresAndHttpTranslationAtIdentityBoundaries() {
    assertThat(Files.exists(IDENTITY_EXCEPTION_HANDLER)).isTrue();
    assertThat(Files.exists(INVALID_MEMBERSHIP_ROLE_EXCEPTION)).isTrue();
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
    String jwtDecoder = Files.readString(MULTI_REALM_JWT_DECODER);

    assertThat(Files.exists(IDENTITY_KEYCLOAK_PROPERTIES)).isTrue();
    assertThat(Files.exists(JWT_TRUST_POLICY)).isTrue();
    assertThat(jwtDecoder).contains("IdentityJwtTrustPolicy");
    assertThat(userAdapter).doesNotContain("@Value");
    assertThat(adminAdapter).doesNotContain("@Value");
  }

  @Test
  void keepsRealmProvisioningDependentOnAnApplicationPort() throws IOException {
    String processSource = Files.readString(REALM_PROVISIONING_PROCESS);

    assertThat(Files.exists(IDENTITY_PROVIDER_ADMINISTRATION_PORT)).isTrue();
    assertThat(Files.exists(PROVISION_TENANT_IDENTITY_USE_CASE)).isTrue();
    assertThat(processSource).contains("IdentityProviderAdministrationPort");
    assertThat(processSource).doesNotContain("KeycloakAdminClient");
    assertThat(processSource).doesNotContain("admin123");
    assertThat(processSource).doesNotContain("Thread.sleep");
  }

  @Test
  void exposesCurrentUserWorkflowThroughOneApplicationUseCase() throws IOException {
    Path query =
        sourcePath(
            "modules/identity/src/main/java/com/emme/identity/api/query/GetCurrentUserQuery.java");
    Path result =
        sourcePath(
            "modules/identity/src/main/java/com/emme/identity/api/result/CurrentUserInfo.java");
    Path useCase =
        sourcePath(
            "modules/identity/src/main/java/com/emme/identity/api/usecase/GetCurrentUserUseCase.java");
    Path service =
        sourcePath(
            "modules/identity/src/main/java/com/emme/identity/application/service/GetCurrentUserService.java");
    Path authController =
        sourcePath(
            "modules/identity/src/main/java/com/emme/identity/adapter/in/web/controller/AuthController.java");

    assertThat(Files.exists(query)).isTrue();
    assertThat(Files.exists(result)).isTrue();
    assertThat(Files.exists(useCase)).isTrue();
    assertThat(Files.exists(service)).isTrue();
    assertThat(Files.readString(authController))
        .contains("GetCurrentUserUseCase")
        .doesNotContain("CurrentUserController");
  }

  @Test
  void declaresIdentityEndpointsAsSpringMvcVersionOne() throws IOException {
    Path controller =
        sourcePath(
            "modules/identity/src/main/java/com/emme/identity/adapter/in/web/controller/IdentityController.java");

    assertThat(Files.readString(controller))
        .contains("@RequestMapping(path = \"/api/identity\", version = \"1.0\")")
        .doesNotContain("/api/v1/identity");
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
