package com.emme.buildlogic

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.writeText

class DeploymentPluginFunctionalTest {
  @Test
  fun `registers deployment tasks without resolving target during configuration`(
    @TempDir projectDir: Path,
  ) {
    writeSettings(projectDir)
    projectDir.resolve("gradle.properties").writeText("emme.deployment.target=invalid")
    projectDir.resolve("build.gradle.kts").writeText(
      """
      plugins {
          id("emme.deployment")
      }
      """.trimIndent(),
    )

    val result =
      GradleRunner
        .create()
        .withProjectDir(projectDir.toFile())
        .withArguments("tasks", "--group=deployment", "--stacktrace")
        .build()

    assertThat(result.output).contains("deployUp", "deployStatus", "BUILD SUCCESSFUL")
  }

  @Test
  fun `unsupported target fails when deployment executes`(
    @TempDir projectDir: Path,
  ) {
    writeSettings(projectDir)
    projectDir.resolve("gradle.properties").writeText("emme.deployment.target=invalid")
    projectDir.resolve("build.gradle.kts").writeText(
      """
      plugins {
          id("emme.deployment")
      }
      """.trimIndent(),
    )

    val result =
      GradleRunner
        .create()
        .withProjectDir(projectDir.toFile())
        .withArguments("deployStatus", "--stacktrace")
        .buildAndFail()

    assertThat(result.output).contains("Unsupported deployment target 'invalid'")
  }

  private fun writeSettings(projectDir: Path) {
    projectDir.resolve("settings.gradle.kts").writeText(
      """
      pluginManagement {
          includeBuild("${escapePath(findBuildLogicDir())}")
          repositories {
              gradlePluginPortal()
              mavenCentral()
          }
      }
      ${dependencyRepositories()}
      rootProject.name = "test-deployment"
      """.trimIndent(),
    )
  }
}
