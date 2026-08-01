package com.emme.tenancy;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class TenancyPackageConventionTest {

  private static final Path ROOT_PACKAGE_INFO =
      sourcePath("modules/tenancy/src/main/java/com/emme/tenancy/package-info.java");
  private static final Path API_PACKAGE_INFO =
      sourcePath("modules/tenancy/src/main/java/com/emme/tenancy/api/package-info.java");
  private static final Path USE_CASE_PACKAGE_INFO =
      sourcePath("modules/tenancy/src/main/java/com/emme/tenancy/api/usecase/package-info.java");
  private static final Path RESULT_PACKAGE_INFO =
      sourcePath("modules/tenancy/src/main/java/com/emme/tenancy/api/result/package-info.java");
  private static final Path LEGACY_API =
      sourcePath("modules/tenancy/src/main/java/com/emme/tenancy/api/TenantApi.java");
  private static final Path LEGACY_RESULT =
      sourcePath("modules/tenancy/src/main/java/com/emme/tenancy/api/TenantInfo.java");
  private static final Path EVENT =
      sourcePath("modules/tenancy/src/main/java/com/emme/tenancy/api/event/TenantCreated.java");
  private static final Path LEGACY_EVENT =
      sourcePath(
          "modules/tenancy/src/main/java/com/emme/tenancy/api/event/TenantCreatedEvent.java");
  private static final Path LEGACY_PERSISTENCE_PACKAGE =
      sourcePath("modules/tenancy/src/main/java/com/emme/tenancy/entity");
  private static final Path PERSISTENCE_PACKAGE =
      sourcePath("modules/tenancy/src/main/java/com/emme/tenancy/adapter/out/persistence");
  private static final Path PERSISTENCE_ENTITY =
      sourcePath(
          "modules/tenancy/src/main/java/com/emme/tenancy/adapter/out/persistence/entity/TenantEntity.java");
  private static final Path PERSISTENCE_REPOSITORY =
      sourcePath(
          "modules/tenancy/src/main/java/com/emme/tenancy/adapter/out/persistence/repository/SpringDataTenantRepository.java");
  private static final Path PERSISTENCE_ADAPTER =
      sourcePath(
          "modules/tenancy/src/main/java/com/emme/tenancy/adapter/out/persistence/adapter/TenantPersistenceAdapter.java");
  private static final Path DATABASE_REGISTRY_PORT =
      sourcePath(
          "modules/tenancy/src/main/java/com/emme/tenancy/application/port/out/DatabaseRegistryPort.java");
  private static final Path DATABASE_REGISTRY_ENTRY =
      sourcePath(
          "modules/tenancy/src/main/java/com/emme/tenancy/application/port/out/DatabaseRegistryEntry.java");
  private static final Path PERSISTENCE_MAPPER =
      sourcePath(
          "modules/tenancy/src/main/java/com/emme/tenancy/adapter/out/persistence/mapper/TenantPersistenceMapper.java");
  private static final Path PERSISTENCE_ASPECT =
      sourcePath(
          "modules/tenancy/src/main/java/com/emme/tenancy/adapter/out/persistence/aspect/TenantContextAspect.java");
  private static final Path LEGACY_TENANT_CONTEXT_ASPECT =
      sourcePath("modules/tenancy/src/main/java/com/emme/tenancy/TenantContextAspect.java");
  private static final Path APPLICATION_SERVICE_PACKAGE =
      sourcePath("modules/tenancy/src/main/java/com/emme/tenancy/application/service");
  private static final Path TENANT_SERVICE =
      sourcePath(
          "modules/tenancy/src/main/java/com/emme/tenancy/application/service/TenantService.java");
  private static final Path AUDIT_SERVICE =
      sourcePath(
          "modules/tenancy/src/main/java/com/emme/tenancy/application/service/AuditService.java");
  private static final Path PROVISIONING_SERVICE =
      sourcePath(
          "modules/tenancy/src/main/java/com/emme/tenancy/application/service/TenantProvisioningService.java");
  private static final Path PROVISIONING_IMPLEMENTATION =
      sourcePath(
          "modules/tenancy/src/main/java/com/emme/tenancy/application/service/JdbcTenantProvisioningService.java");
  private static final Path PROCESS_PACKAGE =
      sourcePath("modules/tenancy/src/main/java/com/emme/tenancy/application/process");
  private static final Path PROVISIONING_PROCESS =
      sourcePath(
          "modules/tenancy/src/main/java/com/emme/tenancy/application/process/TenantProvisioningProcessManager.java");
  private static final Path LEGACY_TENANT_SERVICE =
      sourcePath("modules/tenancy/src/main/java/com/emme/tenancy/application/TenantService.java");
  private static final Path LEGACY_AUDIT_SERVICE =
      sourcePath("modules/tenancy/src/main/java/com/emme/tenancy/application/AuditService.java");
  private static final Path LEGACY_PROVISIONING_SERVICE =
      sourcePath(
          "modules/tenancy/src/main/java/com/emme/tenancy/service/TenantProvisioningService.java");
  private static final Path LEGACY_PROVISIONING_IMPLEMENTATION =
      sourcePath(
          "modules/tenancy/src/main/java/com/emme/tenancy/service/DefaultTenantProvisioningService.java");
  private static final Path LEGACY_PROVISIONING_WORKER =
      sourcePath(
          "modules/tenancy/src/main/java/com/emme/tenancy/service/TenantProvisioningWorker.java");
  private static final Path WEB_CONTROLLER_PACKAGE =
      sourcePath("modules/tenancy/src/main/java/com/emme/tenancy/adapter/in/web/controller");
  private static final Path TENANT_CONTROLLER =
      sourcePath(
          "modules/tenancy/src/main/java/com/emme/tenancy/adapter/in/web/controller/TenantController.java");
  private static final Path PROVISIONING_CONTROLLER =
      sourcePath(
          "modules/tenancy/src/main/java/com/emme/tenancy/adapter/in/web/controller/TenantProvisioningController.java");
  private static final Path CREATE_TENANT_REQUEST =
      sourcePath(
          "modules/tenancy/src/main/java/com/emme/tenancy/adapter/in/web/request/CreateTenantRequest.java");
  private static final Path UPDATE_TENANT_REQUEST =
      sourcePath(
          "modules/tenancy/src/main/java/com/emme/tenancy/adapter/in/web/request/UpdateTenantRequest.java");
  private static final Path TENANT_RESPONSE =
      sourcePath(
          "modules/tenancy/src/main/java/com/emme/tenancy/adapter/in/web/response/TenantResponse.java");
  private static final Path PROVISION_TENANT_REQUEST =
      sourcePath(
          "modules/tenancy/src/main/java/com/emme/tenancy/adapter/in/web/request/ProvisionTenantRequest.java");
  private static final Path LEGACY_TENANT_CONTROLLER =
      sourcePath("modules/tenancy/src/main/java/com/emme/tenancy/web/TenantController.java");
  private static final Path LEGACY_PROVISIONING_CONTROLLER =
      sourcePath(
          "modules/tenancy/src/main/java/com/emme/tenancy/web/TenantProvisioningController.java");
  private static final Path WEB_FILTER_PACKAGE =
      sourcePath("modules/tenancy/src/main/java/com/emme/tenancy/adapter/in/web/filter");
  private static final Path TENANT_CONTEXT_FILTER =
      sourcePath(
          "modules/tenancy/src/main/java/com/emme/tenancy/adapter/in/web/filter/TenantContextFilter.java");
  private static final Path TRUSTED_TENANT_RESOLVER =
      sourcePath(
          "modules/tenancy/src/main/java/com/emme/tenancy/adapter/in/web/filter/TrustedTenantResolver.java");
  private static final Path TENANT_RATE_LIMIT_INTERCEPTOR =
      sourcePath(
          "modules/tenancy/src/main/java/com/emme/tenancy/adapter/in/web/filter/TenantRateLimitInterceptor.java");
  private static final Path CONFIGURATION_PACKAGE =
      sourcePath("modules/tenancy/src/main/java/com/emme/tenancy/configuration");
  private static final Path RATE_LIMIT_PROPERTIES =
      sourcePath(
          "modules/tenancy/src/main/java/com/emme/tenancy/configuration/RateLimitProperties.java");
  private static final Path WEB_MVC_CONFIGURATION =
      sourcePath(
          "modules/tenancy/src/main/java/com/emme/tenancy/configuration/WebMvcConfiguration.java");
  private static final Path DATA_SOURCE_CONFIGURATION =
      sourcePath(
          "modules/tenancy/src/main/java/com/emme/tenancy/configuration/DataSourceConfiguration.java");
  private static final Path TENANT_POOLING_PROPERTIES =
      sourcePath(
          "modules/tenancy/src/main/java/com/emme/tenancy/configuration/TenantPoolingProperties.java");
  private static final Path LEGACY_DATA_SOURCE_CONFIG =
      sourcePath("modules/tenancy/src/main/java/com/emme/tenancy/config/DataSourceConfig.java");
  private static final Path LEGACY_TENANT_POOLING_CONFIG =
      sourcePath("modules/tenancy/src/main/java/com/emme/tenancy/config/TenantPoolingConfig.java");
  private static final Path DATABASE_POOL_PROVIDER =
      sourcePath(
          "modules/tenancy/src/main/java/com/emme/tenancy/adapter/out/client/database/TenantDatabasePoolProvider.java");
  private static final Path TENANT_ROUTING_DATA_SOURCE =
      sourcePath(
          "modules/tenancy/src/main/java/com/emme/tenancy/adapter/out/client/database/TenantRoutingDataSource.java");
  private static final Path LEGACY_POOL_PACKAGE =
      sourcePath("modules/tenancy/src/main/java/com/emme/tenancy/pool");
  private static final Path LEGACY_TENANT_CONTEXT_FILTER =
      sourcePath("modules/tenancy/src/main/java/com/emme/tenancy/TenantContextFilter.java");
  private static final Path LEGACY_TRUSTED_TENANT_RESOLVER =
      sourcePath("modules/tenancy/src/main/java/com/emme/tenancy/TrustedTenantResolver.java");
  private static final Path LEGACY_RATE_LIMIT_INTERCEPTOR =
      sourcePath("modules/tenancy/src/main/java/com/emme/tenancy/web/RateLimitInterceptor.java");
  private static final Path LEGACY_RATE_LIMIT_PROPERTIES =
      sourcePath("modules/tenancy/src/main/java/com/emme/tenancy/web/RateLimitProperties.java");
  private static final Path LEGACY_WEB_MVC_CONFIGURATION =
      sourcePath("modules/tenancy/src/main/java/com/emme/tenancy/config/WebMvcConfig.java");

  @Test
  void keepsModuleMetadataAtTheModuleRoot() throws IOException {
    String source = Files.readString(ROOT_PACKAGE_INFO);

    assertThat(source).contains("@org.springframework.modulith.ApplicationModule");
    assertThat(source).contains("\"shared\"");
  }

  @Test
  void groupsPublicContractsByKind() throws IOException {
    assertThat(Files.readString(API_PACKAGE_INFO))
        .doesNotContain("@org.springframework.modulith.NamedInterface");
    assertThat(Files.readString(USE_CASE_PACKAGE_INFO))
        .contains("@org.springframework.modulith.NamedInterface(\"tenant-api\")");
    assertThat(Files.readString(RESULT_PACKAGE_INFO))
        .contains("@org.springframework.modulith.NamedInterface(\"tenant-api\")");
  }

  @Test
  void removesLegacyUngroupedContractFilesAndUsesPastTenseEventNaming() {
    assertThat(Files.exists(LEGACY_API)).isFalse();
    assertThat(Files.exists(LEGACY_RESULT)).isFalse();
    assertThat(Files.exists(EVENT)).isTrue();
    assertThat(Files.exists(LEGACY_EVENT)).isFalse();
  }

  @Test
  void ownsPersistenceTypesUnderOutboundPersistence() {
    assertThat(hasJavaSources(LEGACY_PERSISTENCE_PACKAGE)).isFalse();
    assertThat(Files.exists(PERSISTENCE_PACKAGE)).isTrue();
    assertThat(Files.exists(PERSISTENCE_ENTITY)).isTrue();
    assertThat(Files.exists(PERSISTENCE_REPOSITORY)).isTrue();
    assertThat(Files.exists(PERSISTENCE_ADAPTER)).isTrue();
    assertThat(Files.exists(PERSISTENCE_MAPPER)).isTrue();
  }

  @Test
  void exposesDatabaseRegistryThroughApplicationPort() throws IOException {
    assertThat(Files.exists(DATABASE_REGISTRY_PORT)).isTrue();
    assertThat(Files.exists(DATABASE_REGISTRY_ENTRY)).isTrue();

    String poolManager =
        Files.readString(
            sourcePath(
                "modules/tenancy/src/main/java/com/emme/tenancy/adapter/out/client/database/TenantDatabasePoolProvider.java"));
    assertThat(poolManager).contains("DatabaseRegistryPort");
    assertThat(poolManager).doesNotContain("DatabaseRegistryService");
    assertThat(poolManager).doesNotContain("adapter.out.persistence.entity.DatabaseRegistry");
  }

  @Test
  void ownsTenantContextAspectUnderOutboundPersistence() {
    assertThat(Files.exists(PERSISTENCE_ASPECT)).isTrue();
    assertThat(Files.exists(LEGACY_TENANT_CONTEXT_ASPECT)).isFalse();
  }

  @Test
  void ownsOrchestrationByApplicationResponsibility() {
    assertThat(Files.exists(APPLICATION_SERVICE_PACKAGE)).isTrue();
    assertThat(Files.exists(TENANT_SERVICE)).isTrue();
    assertThat(Files.exists(AUDIT_SERVICE)).isTrue();
    assertThat(Files.exists(PROVISIONING_SERVICE)).isTrue();
    assertThat(Files.exists(PROVISIONING_IMPLEMENTATION)).isTrue();
    assertThat(Files.exists(PROCESS_PACKAGE)).isTrue();
    assertThat(Files.exists(PROVISIONING_PROCESS)).isTrue();
    assertThat(Files.exists(LEGACY_TENANT_SERVICE)).isFalse();
    assertThat(Files.exists(LEGACY_AUDIT_SERVICE)).isFalse();
    assertThat(Files.exists(LEGACY_PROVISIONING_SERVICE)).isFalse();
    assertThat(Files.exists(LEGACY_PROVISIONING_IMPLEMENTATION)).isFalse();
    assertThat(Files.exists(LEGACY_PROVISIONING_WORKER)).isFalse();
  }

  @Test
  void ownsHttpEntryPointsUnderInboundWebAdapters() {
    assertThat(Files.exists(WEB_CONTROLLER_PACKAGE)).isTrue();
    assertThat(Files.exists(TENANT_CONTROLLER)).isTrue();
    assertThat(Files.exists(PROVISIONING_CONTROLLER)).isTrue();
    assertThat(Files.exists(CREATE_TENANT_REQUEST)).isTrue();
    assertThat(Files.exists(UPDATE_TENANT_REQUEST)).isTrue();
    assertThat(Files.exists(PROVISION_TENANT_REQUEST)).isTrue();
    assertThat(Files.exists(TENANT_RESPONSE)).isTrue();
    assertThat(Files.exists(LEGACY_TENANT_CONTROLLER)).isFalse();
    assertThat(Files.exists(LEGACY_PROVISIONING_CONTROLLER)).isFalse();
  }

  @Test
  void ownsRequestPipelineAndWebConfigurationByResponsibility() {
    assertThat(Files.exists(WEB_FILTER_PACKAGE)).isTrue();
    assertThat(Files.exists(TENANT_CONTEXT_FILTER)).isTrue();
    assertThat(Files.exists(TRUSTED_TENANT_RESOLVER)).isTrue();
    assertThat(Files.exists(TENANT_RATE_LIMIT_INTERCEPTOR)).isTrue();
    assertThat(Files.exists(CONFIGURATION_PACKAGE)).isTrue();
    assertThat(Files.exists(RATE_LIMIT_PROPERTIES)).isTrue();
    assertThat(Files.exists(WEB_MVC_CONFIGURATION)).isTrue();
    assertThat(Files.exists(DATA_SOURCE_CONFIGURATION)).isTrue();
    assertThat(Files.exists(TENANT_POOLING_PROPERTIES)).isTrue();
    assertThat(Files.exists(LEGACY_TENANT_CONTEXT_FILTER)).isFalse();
    assertThat(Files.exists(LEGACY_TRUSTED_TENANT_RESOLVER)).isFalse();
    assertThat(Files.exists(LEGACY_RATE_LIMIT_INTERCEPTOR)).isFalse();
    assertThat(Files.exists(LEGACY_RATE_LIMIT_PROPERTIES)).isFalse();
    assertThat(Files.exists(LEGACY_WEB_MVC_CONFIGURATION)).isFalse();
    assertThat(Files.exists(LEGACY_DATA_SOURCE_CONFIG)).isFalse();
    assertThat(Files.exists(LEGACY_TENANT_POOLING_CONFIG)).isFalse();
    assertThat(Files.exists(DATABASE_POOL_PROVIDER)).isTrue();
    assertThat(Files.exists(TENANT_ROUTING_DATA_SOURCE)).isTrue();
    assertThat(hasJavaSources(LEGACY_POOL_PACKAGE)).isFalse();
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
