package com.emme.buildlogic

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

class PluginIdContractTest {
  @Test
  fun `all public convention ids have a matching precompiled script`() {
    val sourceRoot = sourcePath("build-logic/src/main/kotlin")
    val publicIds =
      setOf(
        "emme.api-compat",
        "emme.container",
        "emme.deployment",
        "emme.feature-flags",
        "emme.integration-testing",
        "emme.java-base",
        "emme.java-library",
        "emme.messaging",
        "emme.modulith",
        "emme.persistence",
        "emme.publishing",
        "emme.quality",
        "emme.security",
        "emme.spring-application",
        "emme.spring-module",
        "emme.spring-web",
        "emme.test-fixtures",
        "emme.testing",
      )

    publicIds.forEach { id ->
      assertThat(Files.exists(sourceRoot.resolve("$id.gradle.kts"))).isTrue()
    }
  }

  @Test
  fun `binary plugin registrations retain their published implementation classes`() {
    val buildFile = Files.readString(sourcePath("build-logic/build.gradle.kts"))

    assertThat(buildFile)
      .contains("id = \"com.emme.root\"")
      .contains("implementationClass = \"com.emme.buildlogic.root.EmmeRootPlugin\"")
      .contains("id = \"com.emme.container-binary\"")
      .contains("implementationClass = \"com.emme.buildlogic.container.EmmeContainerPlugin\"")
      .contains("id = \"com.emme.publishing-binary\"")
      .contains(
        "implementationClass = \"com.emme.buildlogic.publishing.EmmePublishingPlugin\"",
      ).contains("id = \"com.emme.deployment\"")
      .contains("implementationClass = \"com.emme.buildlogic.deployment.EmmeDeploymentPlugin\"")
      .contains("id = \"com.emme.security-binary\"")
      .contains("implementationClass = \"com.emme.buildlogic.security.EmmeSecurityPlugin\"")
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
