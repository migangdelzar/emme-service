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
    assertThat(Files.exists(sourceRoot.resolve("core/EmmeContainerPlugin.kt"))).isFalse()
    assertThat(Files.exists(sourceRoot.resolve("core/DeployTask.kt"))).isFalse()
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
