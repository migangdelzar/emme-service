package com.emme;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Protects the sole deployable application contract. */
class PlatformApplicationParityTest {

  @Test
  void canonicalApplicationUsesTheMvpRuntimeContract() throws IOException {
    String applicationConfiguration =
        Files.readString(Path.of("src/main/resources/application.yml"));

    assertThat(applicationConfiguration)
        .contains("name: emme-platform")
        .contains("port: 8081")
        .contains("probes:")
        .contains("enabled: true")
        .contains("include: health,info,metrics,prometheus,env,loggers,modulith");
  }

  @Test
  void canonicalApplicationComposesThePlatformModules() throws IOException {
    String buildConfiguration = Files.readString(Path.of("build.gradle.kts"));
    List<String> requiredModules =
        List.of(
            ":modules:shared",
            ":modules:tenancy",
            ":modules:identity",
            ":modules:services",
            ":modules:clients",
            ":modules:appointments",
            ":modules:salon",
            ":modules:subscriptions",
            ":modules:documents",
            ":modules:staffing",
            ":modules:catalog",
            ":modules:booking",
            ":modules:calendar",
            ":modules:notification",
            ":modules:payment",
            ":modules:assistant",
            ":modules:audit");

    requiredModules.forEach(
        module ->
            assertThat(buildConfiguration).contains("implementation(project(\"" + module + "\"))"));
  }

  @Test
  void applicationImageUsesSpringBootBuildpacksInsteadOfTheDockerfileConvention()
      throws IOException {
    String buildConfiguration = Files.readString(Path.of("build.gradle.kts"));

    assertThat(buildConfiguration)
        .doesNotContain("id(\"emme.container\")")
        .doesNotContain("emmeContainer {");
    assertThat(readSource(".github/workflows/container-image.yml"))
        .contains(":emme-platform:bootBuildImage");
  }

  @Test
  void applicationCoverageExcludesOnlyTheBootstrapEntrypoint() throws IOException {
    String buildConfiguration = readSource("applications/emme-platform/build.gradle.kts");

    assertThat(buildConfiguration)
        .contains("exclude(\"com/emme/EmmeApplication.class\")")
        .doesNotContain("exclude(\"com/emme/configuration/JacksonConfiguration.class\")");
  }

  @Test
  void everyMaterializedApplicationConfigurationPackageHasLocalMetadata() throws IOException {
    Path configurationPackage =
        sourcePath("applications/emme-platform/src/main/java/com/emme/configuration");

    assertThat(configurationPackage.resolve("package-info.java")).exists();
  }

  @Test
  void ephemeralTestProfilesKeepSchemasAvailableDuringFrameworkShutdown() throws IOException {
    List<String> profiles =
        List.of(
            "applications/emme-platform/src/main/resources/application-test.yml",
            "applications/emme-platform/src/integrationTest/resources/application-kafka-test.yml",
            "libraries/testing/src/testFixtures/resources/application-test.yml",
            "libraries/testing/src/testFixtures/resources/application-repository.yml",
            "libraries/testing/src/testFixtures/resources/application-web.yml",
            "libraries/testing/src/testFixtures/resources/application-resttest.yml",
            "libraries/testing/src/testFixtures/resources/application-integration-test.yml");

    profiles.forEach(
        profile ->
            assertThat(readSource(profile))
                .as("ephemeral database profile: %s", profile)
                .contains("ddl-auto: create\n")
                .doesNotContain("ddl-auto: create-drop"));
  }

  @Test
  void ephemeralTestProfilesDisableDatabaseBackedScheduling() throws IOException {
    List<String> profiles =
        List.of(
            "applications/emme-platform/src/main/resources/application-test.yml",
            "applications/emme-platform/src/integrationTest/resources/application-kafka-test.yml",
            "libraries/testing/src/testFixtures/resources/application.yml",
            "libraries/testing/src/testFixtures/resources/application-test.yml",
            "libraries/testing/src/testFixtures/resources/application-repository.yml",
            "libraries/testing/src/testFixtures/resources/application-web.yml",
            "libraries/testing/src/testFixtures/resources/application-resttest.yml",
            "libraries/testing/src/testFixtures/resources/application-integration-test.yml");

    profiles.forEach(
        profile ->
            assertThat(readSource(profile))
                .as("ephemeral Spring profile: %s", profile)
                .contains("scheduling:\n      enabled: false")
                .contains("tenant:\n    provisioning:\n      enabled: false"));
  }

  @Test
  void mvpProfilesDoNotActivateKafkaProvider() throws IOException {
    List<String> profiles =
        List.of(
            "applications/emme-platform/src/main/resources/application.yml",
            "applications/emme-platform/src/main/resources/application-production.yml",
            "applications/emme-platform/src/main/resources/application-test.yml");

    profiles.forEach(
        profile ->
            assertThat(readSource(profile))
                .as("Kafka provider must remain deferred in %s", profile)
                .doesNotContain("EMME_KAFKA_EVENTS_ENABLED:true")
                .doesNotContain("app:\n  messaging:\n    kafka:")
                .doesNotContain("spring:\n  kafka:"));
  }

  @Test
  void kafkaProfileUsesItsEmbeddedDatabaseForCoreDatasource() throws IOException {
    assertThat(
            readSource(
                "applications/emme-platform/src/integrationTest/resources/application-kafka-test.yml"))
        .contains("core:\n      url: jdbc:h2:mem:emme-kafka");
  }

  @Test
  void kubernetesBaseDeploymentDoesNotContainSecretPlaceholders() {
    String deployment = readSource("infra/kubernetes/base/backend-deployment.yaml");

    assertThat(deployment)
        .doesNotContain("APP_GOOGLE_OAUTH_CLIENT_ID")
        .doesNotContain("APP_GOOGLE_OAUTH_CLIENT_SECRET")
        .doesNotContain("replace-with-32-char-secure-key!!");
  }

  @Test
  void productionKubernetesOverlaysDoNotInjectDeferredKafkaSecrets() {
    List<String> overlays =
        List.of(
            "infra/kubernetes/overlays/k3s-production-jvm/kustomization.yaml",
            "infra/kubernetes/overlays/k3s-production-native/kustomization.yaml");

    overlays.forEach(
        overlay ->
            assertThat(readSource(overlay))
                .as("Deferred Kafka secrets must not be injected by %s", overlay)
                .doesNotContain("KAFKA_SASL_JAAS_CONFIG")
                .doesNotContain("kafka-sasl-jaas-config"));
  }

  @Test
  void kubernetesFrontendContractMatchesTheBuildpacksImagePort() {
    String deployment = readSource("infra/kubernetes/base/frontend-deployment.yaml");
    String service = readSource("infra/kubernetes/base/frontend-service.yaml");

    assertThat(deployment)
        .contains("containerPort: 8080")
        .contains("path: /health\n              port: 8080");
    assertThat(service).contains("targetPort: 8080");
  }

  @Test
  void e2eComposeSeparatesExternalIdentityIssuersFromInternalTransport() {
    String compose = readSource("deployment/compose/compose.environment-e2e.yaml");

    assertThat(compose)
        .contains("APP_KEYCLOAK_BASE_URL: http://host.docker.internal:18080")
        .contains("APP_KEYCLOAK_ISSUER_URI: http://localhost:18080/realms/emme-core")
        .contains("APP_KEYCLOAK_CUSTOMER_ISSUER_URI: http://localhost:18080/realms/emme-customers")
        .contains("APP_KEYCLOAK_JWK_SET_BASE_URL: http://keycloak:8080")
        .contains("KC_HOSTNAME: http://localhost:18080");
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

  private static String readSource(String relativePath) {
    try {
      return Files.readString(sourcePath(relativePath));
    } catch (IOException exception) {
      throw new IllegalStateException("Cannot read source path: " + relativePath, exception);
    }
  }
}
