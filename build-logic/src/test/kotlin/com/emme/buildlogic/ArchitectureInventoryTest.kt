package com.emme.buildlogic

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

class ArchitectureInventoryTest {
  @Test
  fun `every convention script is materialized at the build logic boundary`() {
    val sourceRoot = sourcePath("build-logic/src/main/kotlin")
    val expectedScripts =
      setOf(
        "emme.api-compat.gradle.kts",
        "emme.container.gradle.kts",
        "emme.deployment.gradle.kts",
        "emme.feature-flags.gradle.kts",
        "emme.integration-testing.gradle.kts",
        "emme.java-base.gradle.kts",
        "emme.java-library.gradle.kts",
        "emme.messaging.gradle.kts",
        "emme.modulith.gradle.kts",
        "emme.persistence.gradle.kts",
        "emme.publishing.gradle.kts",
        "emme.quality.gradle.kts",
        "emme.security.gradle.kts",
        "emme.spring-application.gradle.kts",
        "emme.spring-module.gradle.kts",
        "emme.spring-web.gradle.kts",
        "emme.test-fixtures.gradle.kts",
        "emme.testing.gradle.kts",
      )

    assertThat(
      Files.list(sourceRoot).use { paths ->
        paths
          .map { it.fileName.toString() }
          .filter { it.endsWith(".gradle.kts") }
          .toList()
          .toSet()
      },
    ).containsExactlyInAnyOrderElementsOf(expectedScripts)
  }

  @Test
  fun `capability implementation packages own complex behavior`() {
    val sourceRoot = sourcePath("build-logic/src/main/kotlin/com/emme/buildlogic")

    assertThat(Files.exists(sourceRoot.resolve("container/EmmeContainerPlugin.kt"))).isTrue()
    assertThat(Files.exists(sourceRoot.resolve("deployment/EmmeDeploymentPlugin.kt"))).isTrue()
    assertThat(Files.exists(sourceRoot.resolve("publishing/EmmePublishingPlugin.kt"))).isTrue()
    assertThat(Files.exists(sourceRoot.resolve("security/EmmeSecurityPlugin.kt"))).isTrue()
    assertThat(Files.exists(sourceRoot.resolve("root/EmmeRootPlugin.kt"))).isTrue()
    assertThat(Files.exists(sourceRoot.resolve("container/task/BuildContainerImageTask.kt"))).isTrue()
    assertThat(Files.exists(sourceRoot.resolve("container/task/PushContainerImageTask.kt"))).isTrue()
    assertThat(Files.exists(sourceRoot.resolve("container/task/VerifyContainerImageTask.kt"))).isTrue()
    assertThat(Files.exists(sourceRoot.resolve("core/EmmeContainerPlugin.kt"))).isFalse()
    assertThat(Files.exists(sourceRoot.resolve("core/DeployTask.kt"))).isFalse()
  }

  @Test
  fun `capability conventions do not apply the spring module type`() {
    val sourceRoot = sourcePath("build-logic/src/main/kotlin")
    val capabilityScripts =
      listOf(
        "emme.persistence.gradle.kts",
        "emme.messaging.gradle.kts",
        "emme.modulith.gradle.kts",
        "emme.spring-web.gradle.kts",
      )

    capabilityScripts.forEach { scriptName ->
      assertThat(Files.readString(sourceRoot.resolve(scriptName)))
        .describedAs("$scriptName must remain a capability, not a module type")
        .doesNotContain("id(\"emme.spring-module\")")
    }
  }

  @Test
  fun `registry results do not depend on container implementation types`() {
    val registryProvider =
      Files.readString(
        sourcePath("build-logic/src/main/kotlin/com/emme/buildlogic/registry/RegistryProvider.kt"),
      )
    val containerResults =
      Files.readString(
        sourcePath("build-logic/src/main/kotlin/com/emme/buildlogic/container/provider/ContainerResult.kt"),
      )

    assertThat(registryProvider).doesNotContain("com.emme.buildlogic.container")
    assertThat(registryProvider).contains("RegistryPushResult")
    assertThat(containerResults).contains("ContainerPushResult")
  }

  @Test
  fun `root extension does not own capability configuration`() {
    val rootExtension =
      Files.readString(sourcePath("build-logic/src/main/kotlin/com/emme/buildlogic/root/EmmeBuildExtension.kt"))

    assertThat(rootExtension).doesNotContain("com.emme.buildlogic.container")
    assertThat(rootExtension).doesNotContain("val container")
  }

  @Test
  fun `provider registry preserves build service parameter types`() {
    val providerRegistry =
      Files.readString(sourcePath("build-logic/src/main/kotlin/com/emme/buildlogic/core/ProviderRegistry.kt"))

    assertThat(providerRegistry).doesNotContain("as BuildServiceSpec<BuildServiceParameters>")
    assertThat(providerRegistry).contains("BuildServiceSpec<P>.containerConcurrency()")
    assertThat(providerRegistry).contains("BuildServiceSpec<P>.singleConcurrency()")
  }

  @Test
  fun `container capability selects runtime providers lazily`() {
    val containerPlugin =
      Files.readString(sourcePath("build-logic/src/main/kotlin/com/emme/buildlogic/container/EmmeContainerPlugin.kt"))

    assertThat(containerPlugin).doesNotContain("extension.runtime.map { it.name.lowercase() }.get()")
    assertThat(containerPlugin).contains("PodmanProvider")
    assertThat(containerPlugin).contains("emmePodmanRuntime")
  }

  @Test
  fun `deployment and security capabilities select providers lazily without fallback`() {
    val deploymentPlugin =
      Files.readString(sourcePath("build-logic/src/main/kotlin/com/emme/buildlogic/deployment/EmmeDeploymentPlugin.kt"))
    val securityPlugin =
      Files.readString(sourcePath("build-logic/src/main/kotlin/com/emme/buildlogic/security/EmmeSecurityPlugin.kt"))

    assertThat(deploymentPlugin).doesNotContain("ext.target.get()")
    assertThat(deploymentPlugin).doesNotContain("else -> ComposeProvider")
    assertThat(securityPlugin).doesNotContain("extension.scanner.get()")
    assertThat(securityPlugin).doesNotContain("else -> TrivyProvider")
  }

  @Test
  fun `quality and api compatibility conventions avoid configuration-time value reads`() {
    val qualityConvention =
      Files.readString(sourcePath("build-logic/src/main/kotlin/emme.quality.gradle.kts"))
    val apiCompatibilityConvention =
      Files.readString(sourcePath("build-logic/src/main/kotlin/emme.api-compat.gradle.kts"))

    assertThat(qualityConvention).contains("target(\"src/**/*.java\")")
    assertThat(qualityConvention).doesNotContain("layout.buildDirectory.get()")
    assertThat(apiCompatibilityConvention.substringBefore("doLast")).doesNotContain("baselineVersion.get()")
  }

  private fun sourcePath(relativePath: String): Path {
    var current: Path? = Path.of("").toAbsolutePath()
    while (current != null) {
      val candidate = current!!.resolve(relativePath)
      if (Files.exists(candidate)) return candidate
      current = current!!.parent
    }
    error("Cannot locate source path: $relativePath")
  }
}
