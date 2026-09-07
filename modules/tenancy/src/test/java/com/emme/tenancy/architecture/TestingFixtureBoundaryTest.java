package com.emme.tenancy.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class TestingFixtureBoundaryTest {

  private static final Path TESTING_FIXTURE_ROOT =
      sourcePath("libraries/testing/src/testFixtures/java/com/emme/testing");
  private static final Path TENANCY_FIXTURE_ROOT =
      sourcePath("modules/tenancy/src/testFixtures/java/com/emme/tenancy/testing");
  private static final Path TENANT_BASE_FIXTURE =
      TENANCY_FIXTURE_ROOT.resolve("BaseTenantModuleTest.java");
  private static final Path ENTITLED_TENANT_FIXTURE =
      TENANCY_FIXTURE_ROOT.resolve("EntitledTenantModuleTest.java");
  private static final Path TENANCY_BUILD_FILE = sourcePath("modules/tenancy/build.gradle.kts");
  private static final Path GENERIC_TEST_RESOURCES =
      sourcePath("libraries/testing/src/testFixtures/resources");
  private static final Path TENANCY_TEST_PROFILE =
      sourcePath("modules/tenancy/src/testFixtures/resources/application-tenancy-test.yml");
  private static final Path TENANCY_BOOTSTRAP_FIXTURE =
      sourcePath(
          "modules/tenancy/src/testFixtures/java/com/emme/testing/TestBootstrapJdbcConfig.java");
  private static final Path TENANCY_WEB_TEST =
      sourcePath("modules/tenancy/src/test/java/com/emme/tenancy/web/TenantWebTest.java");

  @Test
  void genericWebFixtureDoesNotOwnTenantProvisioning() throws Exception {
    assertThat(Files.readString(TESTING_FIXTURE_ROOT.resolve("BaseWebTest.java")))
        .doesNotContain("TestBootstrapJdbcConfig");
    assertThat(TESTING_FIXTURE_ROOT.resolve("BaseSpringModuleTest.java")).doesNotExist();
    assertThat(TESTING_FIXTURE_ROOT.resolve("TestBootstrapJdbcConfig.java")).doesNotExist();
    assertThat(TENANCY_FIXTURE_ROOT.resolve("BaseTenantModuleTest.java")).exists();
    assertThat(TENANCY_BOOTSTRAP_FIXTURE).exists();
    assertThat(Files.readString(TENANCY_WEB_TEST))
        .contains("@Import(TestBootstrapJdbcConfig.class)");
  }

  @Test
  void tenantFixtureDoesNotExposeUnusedSalonRepositories() throws Exception {
    assertThat(Files.readString(TENANT_BASE_FIXTURE))
        .doesNotContain("SpringDataBusinessProfileRepository")
        .doesNotContain("profileRepo")
        .doesNotContain("SpringDataMembershipRepository")
        .doesNotContain("membershipRepo")
        .doesNotContain("SpringDataRoleRepository")
        .doesNotContain("roleRepo");
    assertThat(Files.readString(TENANCY_BUILD_FILE))
        .doesNotContain("testFixturesImplementation(project(\":modules:salon\"))");
  }

  @Test
  void keepsEntitlementSetupOutOfTheGenericTenantFixture() throws Exception {
    assertThat(Files.readString(TENANT_BASE_FIXTURE))
        .doesNotContain("SpringDataSubscriptionRepository")
        .doesNotContain("SpringDataFeatureFlagRepository")
        .doesNotContain("fullSetup");
    assertThat(ENTITLED_TENANT_FIXTURE).exists();
    assertThat(Files.readString(ENTITLED_TENANT_FIXTURE)).contains("fullSetup");
  }

  @Test
  void keepsIdentityProviderProvisioningOutOfTheTenantFixture() throws Exception {
    assertThat(Files.readString(TENANT_BASE_FIXTURE))
        .doesNotContain("MockIdentityProviderAdministrationConfig")
        .doesNotContain("com.emme.identity.testing");
    assertThat(Files.readString(ENTITLED_TENANT_FIXTURE))
        .contains("com.emme.identity.adapter.out.persistence")
        .contains("SpringDataFeatureFlagRepository");
  }

  @Test
  void keepsTenantPoolingConfigurationInTheTenancyFixture() throws Exception {
    for (String profile : new String[] {"repository", "resttest", "test", "web"}) {
      assertThat(
              Files.readString(GENERIC_TEST_RESOURCES.resolve("application-" + profile + ".yml")))
          .as("generic test profile: %s", profile)
          .doesNotContain("global-max-connections")
          .doesNotContain("emme.tenancy.pooling");
    }
    assertThat(TENANCY_TEST_PROFILE).exists();
    assertThat(Files.readString(TENANCY_TEST_PROFILE))
        .contains("global-max-connections: 20")
        .contains("default-database-id:");
    assertThat(Files.readString(TENANT_BASE_FIXTURE)).contains("tenancy-test");
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
