package com.emme.buildlogic

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.writeText

class ApiCompatibilityConventionFunctionalTest {
  @Test
  fun `api compatibility convention registers the api check task lazily`(
    @TempDir projectDir: Path,
  ) {
    writeSettings(projectDir)
    projectDir.resolve("build.gradle.kts").writeText(
      """
      plugins {
          id("emme.api-compat")
      }
      """.trimIndent(),
    )

    val result =
      GradleRunner
        .create()
        .withProjectDir(projectDir.toFile())
        .withArguments("tasks", "--all", "--stacktrace")
        .build()

    assertThat(result.output).contains("apiCheck", "BUILD SUCCESSFUL")
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
      rootProject.name = "test-api-compat"
      """.trimIndent(),
    )
  }
}
