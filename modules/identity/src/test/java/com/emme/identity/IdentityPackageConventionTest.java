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
          "modules/identity/src/main/java/com/emme/identity/adapter/out/persistence/entity/CustomerIdentity.java");
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
  private static final Path PERSISTENCE_ADAPTER_PACKAGE =
      sourcePath(
          "modules/identity/src/main/java/com/emme/identity/adapter/out/persistence/adapter");
  private static final Path PERSISTENCE_MAPPER_PACKAGE =
      sourcePath("modules/identity/src/main/java/com/emme/identity/adapter/out/persistence/mapper");
  private static final Path LEGACY_MEMBERSHIP_ENTITY =
      sourcePath(
          "modules/identity/src/main/java/com/emme/identity/adapter/out/persistence/entity/Membership.java");

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

    assertThat(hasJavaSource(WEB_MAPPER_PACKAGE, "IdentityWebMapper.java")).isTrue();
    assertThat(hasJavaSource(WEB_MAPPER_PACKAGE, "FeatureFlagWebMapper.java")).isTrue();
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
